package com.example.rotoscope;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.TimecodeMP4MuxerTrack;
import org.jcodec.scale.RgbToYuv420;

@SuppressLint("ViewConstructor")
public class VideoSurfaceSave extends GLSurfaceView {
    //private static final String TAG = "VideoSurfaceSave";
    //private static final int SLEEP_TIME_MS = 1000;


    VideoRender mRenderer;
    private MediaPlayer mMediaPlayer = null;

    public VideoSurfaceSave(Context context, MediaPlayer mp, String filter) {
        super(context);

        setEGLContextClientVersion(2);
		//Turn on debugging
		setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);

        mMediaPlayer = mp;
        mRenderer = new VideoRender(context, filter, this);
        setEGLConfigChooser(8,8,8,8,0,0); //RGB8UNORM, Depth 0, Stencil 0
        
        setPreserveEGLContextOnPause(true);
        setRenderer(mRenderer);
        //setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onResume() {
        queueEvent(new Runnable(){
                public void run() {
                    mRenderer.setMediaPlayer(mMediaPlayer);
                }});

        super.onResume();
    }
//    public void startTest() throws Exception {
//        Thread.sleep(SLEEP_TIME_MS);
//        mMediaPlayer.start();
//
//        Thread.sleep(SLEEP_TIME_MS * 5);
//        mMediaPlayer.setSurface(null);
//
//        while (mMediaPlayer.isPlaying()) {
//            Thread.sleep(SLEEP_TIME_MS);
//        }
//    }

    public void resetMediaPlayer() throws Exception {
    	Log.d("DEBUG", "resetMediaPlayer called (in VideoSurfaceSave)");
    	mRenderer.finish(); //finalize the mp4 file
    	mRenderer.resetMediaRender();
    }
    
    private static class VideoRender
        implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener{
        private static String TAG = "VideoRender";

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;

 

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        private SurfaceTexture mSurface;
        private boolean updateSurface = false;

        private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        //Offscreen render target
        private int[] m_frameBufferIDs;
        private int[] m_renderBufferIDs;
        
        private MediaPlayer mMediaPlayer;
        private int m_videoDuration;
        private int m_timeStep;
        private int m_currentTime;
        private int m_previousPosition;
        private String m_filter;

        //Video encoding stuff
        private static Buffer m_buffer420;
        private MediaCodec m_encoder;
        private ByteBuffer [] m_encoderInputBuffers;
      	private ByteBuffer [] m_encoderOutputBuffers;
        private static String mimeType = "video/avc";
        private BufferedOutputStream m_outputStream;
        
        private int m_chromaStride;
        private int m_frameSize;
      	
        private final long kTimeOutUs = 5000;
        private MediaCodec.BufferInfo m_info;
        private int m_numInputFrames;
        private boolean m_startedEncoding = false;
        
        //hardcoding width and height for galaxy s 2 (480x800)
        private static int videoWidth = 480;
        private static int videoHeight = 680;
        private static MediaFormat inputFormat = MediaFormat.createVideoFormat(mimeType, videoWidth, videoHeight);
        private static int bitRate = (int) (videoWidth * videoHeight * 24 * 4 * 0.07); // Kush gauge for bitrate (width * height * fram/sec * motionfactor * 0.07) // 
        private static int frameRate = 15; //24 frames per sec
        private static int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar; //AYUV
        private static int stride = 4 * videoWidth; //4 bytes * width
        private static int sliceHeight = videoHeight; //height
        
        private static RgbToYuv420 m_converter = new RgbToYuv420(0, 0);;
        private static Picture m_rgbPicture;
        private static Picture m_yuvPicture;
        private static MP4Muxer m_muxer; 
        private static FramesMP4MuxerTrack outTrack;
        private static ByteBuffer _out; //encoder h.264 output
        private static SeekableByteChannel m_seekableByteCH;
        private static H264Encoder encoder;
        private static ArrayList<ByteBuffer> spsList;
        private static ArrayList<ByteBuffer> ppsList;
        
        private static final long TIMEOUT = 1500000000;
        private static final int TIMESCALE = 24;
        private static long m_TimeRefreshed;
        private static long m_PreviousRefresh;
        private static long m_startTime;
        
        private Context m_Context;
        private VideoSurfaceSave m_videoSaver;
        
        public VideoRender(Context context, String filter, VideoSurfaceSave videoSaver) {
        	m_Context = context;
        	m_filter = filter;
        	m_frameBufferIDs = new int[1];
        	m_renderBufferIDs = new int[1];
        	m_videoSaver = videoSaver;
            mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
            
            m_TimeRefreshed = System.nanoTime();
            m_PreviousRefresh = m_TimeRefreshed;
            m_startTime = System.nanoTime();
        }

        public void setMediaPlayer(MediaPlayer player) {
            mMediaPlayer = player;
        }

        public void resetMediaRender() {
        	mMediaPlayer.reset();
        }
        public void onDrawFrame(GL10 glUnused) {

        	
        	synchronized(this) {
                if (updateSurface) {
                	int currentPosition = mMediaPlayer.getCurrentPosition(); 
                	if (currentPosition < 0) {
                		try 
                		{
                			Log.d("FINISH", "m_currentTime >= m_videoDuration");
        					this.finish();
        				} 
                		catch (IOException e)
        				{
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
                	}
                	if (currentPosition <= m_previousPosition) {
                		Log.d("DEBUG", "Returning because currentPosition " + currentPosition + " <= m_previousPosition " + 
                				m_previousPosition + "on thread " + Thread.currentThread().getId());
                		try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                		return;
                	}
                	m_previousPosition = currentPosition;
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(mSTMatrix);
                    updateSurface = false;
                    mMediaPlayer.pause();
                    
                    //m_currentTime += m_timeStep;
                	//mMediaPlayer.seekTo(m_currentTime);
                
                }
                else
                	Log.d("DEBUG", "onDrawFrame but not updatedSurface");
            }        
            
        	//Bind the frame buffer
        	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, m_frameBufferIDs[0]);
        	
        	
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            GLES20.glFinish();


//	        if (!m_startedEncoding)
//	        {
//	        	if (mMediaPlayer.getCurrentPosition() > 2000) //at time equals 2 seconds in the video
//	        	{
//	        		m_startedEncoding = true;
//	        	}
//	        }
	           

        	try 
	        {
	           	//convertyuv444to420(); 
	           	//encodeFrameMP4();
     //   		synchronized(this)
   //     		{
        			jcodecConvertRGBtoYUV();
        			encodeVideo();
 //       		}
	        } catch (Exception e) {
	           	e.printStackTrace();
	          	System.exit(-1);
	        }
        	
        	//Now move the media pointer by 1 frame approximately (seek by 42 milliseconds)
        	//THen unpause the mediaplayer to continue
        	if (!mMediaPlayer.isPlaying())
        	{
        		m_currentTime += m_timeStep;
            	
            	if (m_currentTime >= m_videoDuration)
            	{
            		try 
            		{
            			Log.d("FINISH", "m_currentTime >= m_videoDuration");
    					this.finish();
    				} 
            		catch (IOException e)
    				{
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
	 
            	}
            	else
            	{

            		mMediaPlayer.seekTo(m_currentTime);
	            	mMediaPlayer.start();
            	}
    	           	
        	}
        //	m_currentTime += m_timeStep;
        	//mMediaPlayer.seekTo(m_currentTime);
        	
        }
        
        public void jcodecConvertRGBtoYUV() {
        	ByteBuffer bufferRGB = ByteBuffer.allocate(videoWidth*videoHeight*4); //width*height*bytesperpixel 3 bytes since only RGB noA
        	bufferRGB.clear();
        	(bufferRGB).order(ByteOrder.nativeOrder());
        	
        	//int[] FBWidth = new int[1];
        	//int[] FBHeight = new int[1];
        	
        	
        	//GLES20.glGetFramebufferAttachmentParameteriv(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_FRAMEBUFFER_WIDTH, FBWidth, 0);
        	//GLES20.glGetFramebufferAttachmentParameteriv(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER_HEIGHT, FBHeight, 0);
        	
        	//System.out.println("FrameBuffer size is " + FBWidth[0] + " , " + FBHeight[0]);
        	
        	GLES20.glReadPixels(0, 0, videoWidth, videoHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bufferRGB);
        	checkGlError("glReadPixels");
        	//Now flip the y direction because glReadPixels returns upsidedown
        	ByteBuffer yflipBuffer;
        	yflipBuffer = flipY(bufferRGB);
        	m_rgbPicture = fromByteArray(yflipBuffer.array(), videoWidth, videoHeight);
        	//m_rgbPicture = fromBitmap(bi);
        	//for (int[] plane: m_yuvPicture.getData())
        	//	System.out.println("Plane is " + plane);
        	m_converter.transform(m_rgbPicture, m_yuvPicture);
        	
        }
        public static ByteBuffer flipY(ByteBuffer src)
        {
        	ByteBuffer dest = ByteBuffer.allocate(videoWidth*videoHeight*4);
        	int pitch = videoWidth*4; //in bytes
        	int srcByteIdx = (videoHeight - 1) * pitch; //point to last row of bytes in src
        	//Write to the dest buffer
        	//Log.d("DEBUG", "pitch " + pitch + " srcByteIdx " + srcByteIdx + " capacity " + src.capacity() );
        	//Rewind the source buffer to set position to the start
        	//src.rewind();
        	for (int destIdx = 0;  destIdx < videoHeight*pitch;  destIdx += pitch)
        	{
        		byte[] row = new byte[pitch];
        		src.position(srcByteIdx);
        		src.get(row, 0, pitch); //write to position 0 of row        		
        		dest.put(row);        		
        		srcByteIdx -= pitch;
        	}
        	return dest;        	
        
        }
//        public static Picture fromBitmap(Bitmap src) {
//    	  Picture dst = Picture.create((int)src.getWidth(), (int)src.getHeight(), ColorSpace.RGB);
//    	  fromBitmap(src, dst);
//    	  return dst;
//    	}
//
//    	public static void fromBitmap(Bitmap src, Picture dst) {
//    	  int[] dstData = dst.getPlaneData(0);
//    	  int[] packed = new int[src.getWidth() * src.getHeight()];
//
//    	  src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
//
//    	  for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
//    	    for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
//    	      int rgb = packed[srcOff];
//    	      dstData[dstOff]     = (rgb >> 16) & 0xff;
//    	      dstData[dstOff + 1] = (rgb >> 8) & 0xff;
//    	      dstData[dstOff + 2] = rgb & 0xff;
//    	    }
//    	  }
//    	}
        public static Picture fromByteArray(byte[] src, int width, int height) {
    	  Picture dst = Picture.create(width, height, ColorSpace.RGB);
    	  fromByteArray(src, dst, width, height);
    	  return dst;
    	}
       	public static void fromByteArray(byte[] src, Picture dst, int width, int height) {
      	  int[] dstData = dst.getPlaneData(0);
      	 // int[] packed = new int[src.getWidth() * src.getHeight()];

      	  //src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
      	  
      	  for (int i = 0, srcOff = 0, dstOff = 0; i < height; i++) {
      	    for (int j = 0; j < width; j++, srcOff+=4, dstOff += 3) {
      	      //int rgb = packed[srcOff];
      	      dst.getPlaneData(0)[dstOff]     = src[srcOff] & 0xff;//(rgb >> 16) & 0xff;
      	      dst.getPlaneData(0)[dstOff + 1] = src[srcOff+1] & 0xff;
      	      dst.getPlaneData(0)[dstOff + 2] = src[srcOff+2] & 0xff;
      	    }
      	  }
      	  
      	}
//    	public static Bitmap toBitmap(Picture src) {
//		  Bitmap dst = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
//		  toBitmap(src, dst);
//		  return dst;
//		}
//
//		public static void toBitmap(Picture src, Bitmap dst) {
//		  int[] srcData = src.getPlaneData(0);
//		  int[] packed = new int[src.getWidth() * src.getHeight()];
//
//		  for (int i = 0, dstOff = 0, srcOff = 0; i < src.getHeight(); i++) {
//		    for (int j = 0; j < src.getWidth(); j++, dstOff++, srcOff += 3) {
//		      packed[dstOff] = (srcData[srcOff] << 16) | (srcData[srcOff + 1] << 8) | srcData[srcOff + 2];
//		    }
//		  }
//		  dst.setPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
//		}
		public void encodeVideo() {
			//m_yuvPicture is 4:2:0 format
			// Encode image into H.264 frame, the result is stored in '_out' buffer
	        _out.clear();
	        Log.d(TAG, "Encoding frame, m_numInputFrames= " +m_numInputFrames);
	        ByteBuffer result = encoder.encodeFrame(_out, m_yuvPicture);

	        // Based on the frame above form correct MP4 packet
	        spsList.clear();
	        ppsList.clear();
	        H264Utils.encodeMOVPacket(result, spsList, ppsList);
	        
	     // Add packet to video track
	        try
	        {
	        	outTrack.addFrame(new MP4Packet(result, m_numInputFrames, TIMESCALE, 1, m_numInputFrames, true, new TapeTimecode((byte)0, (byte)0, (byte)(m_numInputFrames/24), (byte)m_numInputFrames, false), m_numInputFrames, 0));
	        	//outTrack.setTimecode(new TimecodeMP4MuxerTrack (null, mProgram, mProgram));
	        } catch (Exception e)
	        {
	        	System.out.println("Exception when adding encoded frame to encoded track");
	        	e.printStackTrace();
	        	throw new RuntimeException("Fuck you");
	        }
	        m_numInputFrames++;
	        result = null;

		}
		//This function is to finish the mp4 file
	    public void finish() throws IOException {
	        // Push saved SPS/PPS to a special storage in MP4
	        outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));

	        Log.d(TAG, "Write MP4 header and finalize recording");
	        
	        m_muxer.writeHeader();
	        NIOUtils.closeQuietly(m_seekableByteCH);
	    }
        public void convertyuv444to420() {
        	Buffer buffer444 = ByteBuffer.allocate(videoWidth*videoHeight*4); //width*height*bytesperpixel
        	buffer444.clear();
        	((ByteBuffer)buffer444).order(ByteOrder.nativeOrder());
        	GLES20.glReadPixels(0, 0, videoWidth, videoHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer444);
        	
        	
        	//Now make the buffer that we will write the 420 output to
        	//The 420 buffer will be a smaller size.  we have the same number of y data, but less uv 
        	//every 4 pixels will be averaged so there is one uv pair per 4 pixels
        	//NV 12 looks like this
        	//  *********width********
        	//  h
        	//  e         Y
        	//  i
        	//  g
        	//  h
        	//  t
        	//  **********************
        	//  0.5
        	//  hei   U V interleaved
        	//  ght 
        	//  **********************
        	int buffer420_width = videoWidth;
        	int buffer420_height = videoHeight + (videoHeight/2);
        	
        	m_buffer420 = ByteBuffer.allocate(buffer420_width*buffer420_height); //width and height is per byte
        	m_buffer420.clear();
        	
        	//We are going to loop through the 444 buffer by 2x2 blocks of texels (each texel is 4 bytes)
        	//For each 2x2 pixel block, we write 2x2 luma blcok and 1x2 uv pair (1 u and 1 v)
        	//Divide width and height by 2 (since we doing 2x2 blcok)
        	int surfPitch = videoWidth*4; //bytes of width for 444 surface
        	int surfPitch420 = videoWidth*1;
        	for (int row = 0; row < videoHeight; row+=2){
        		for (int col = 0; col < videoWidth; col+=2){
        			int byteOffset = row*(surfPitch) + col*4; 
        			byte luma00 = ((ByteBuffer)buffer444).get(byteOffset+0); //AYUV, so offset points to A, so +1 to get Y
        			byte luma10 = ((ByteBuffer)buffer444).get(byteOffset+4); //top right of 2x2
        			byte luma01 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+0); //bottom left (go to next row)
        			byte luma11 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+4); //bottom right
        			
        			//lets get the 4 pairs of uv so we can average them
        			byte u00 = ((ByteBuffer)buffer444).get(byteOffset+1); //AYUV, so offset points to A, so +2 to get U
        			byte u10 = ((ByteBuffer)buffer444).get(byteOffset+5); //top right of 2x2
        			byte u01 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+1); //bottom left (go to next row)
        			byte u11 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+5); //bottom right
          			byte v00 = ((ByteBuffer)buffer444).get(byteOffset+2); //AYUV, so offset points to A, so +2 to get U
        			byte v10 = ((ByteBuffer)buffer444).get(byteOffset+6); //top right of 2x2
        			byte v01 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+2); //bottom left (go to next row)
        			byte v11 = ((ByteBuffer)buffer444).get(byteOffset+surfPitch+6); //bottom right

        			byte u420 = (byte) (( ( ((int)u00 + (int)u10 + (int)u01 + (int)u11) ) / 4 ) & 0xFF);
        			byte v420 = (byte) (( ( ((int)v00 + (int)v10 + (int)v01 + (int)v11) ) / 4 ) & 0xFF);

        			//ready to write, but we need offsets
        			int y420offset = (row*surfPitch420 ) + col*1;
        			((ByteBuffer)m_buffer420).put(y420offset, luma00) ;
        			((ByteBuffer)m_buffer420).put(y420offset+1, luma10) ;
        			((ByteBuffer)m_buffer420).put(y420offset+surfPitch420, luma01) ;
        			((ByteBuffer)m_buffer420).put(y420offset+surfPitch420+1, luma11) ;
        			int uv420offset = surfPitch420*videoHeight + (row/2)*surfPitch420 + col;
        			((ByteBuffer)m_buffer420).put(uv420offset, u420);
        			((ByteBuffer)m_buffer420).put(uv420offset+1, v420);

        		}		
        	}
        	
//        	if ( (mMediaPlayer.getCurrentPosition()) > 3000) //at time equals 2 seconds in the video
//        	{
//	        	//To test this, lets write a frame to a file and view it in a nv12 viewer
//	        	File root = Environment.getExternalStorageDirectory();
//	        	String frame444 = "frame444_3sec.ayuv";
//	        	String frame420 = "frame420_3sec.nv12";
//	        	try {
//	        	FileOutputStream f = new FileOutputStream(new File(root, frame444));
//	
//	        	f.write(((ByteBuffer)buffer444).array());
//	        	f.flush();
//	        	f.close();
//	        	f = new FileOutputStream(new File(root, frame420));	    
//	        	f.write(((ByteBuffer)m_buffer420).array());
//	        	f.flush();
//	        	f.close();
//	        	// Tell the media scanner about the new file so that it is
//	        	 // immediately available to the user.
//	        	File file444 = new File(root, frame444);
//	        	File file420 = new File(root, frame420);
//	        	
//	        	
//	        	MediaScannerConnection.scanFile(m_Context,
//	        		new String[] { file444.toString(), file420.toString(), "Download/video_encoded.mp4" }, null,
//	        		new MediaScannerConnection.OnScanCompletedListener() {
//						@Override
//						public void onScanCompleted(String path, Uri uri) {
//				        	 Log.i("ExternalStorage", "Scanned " + path + ":");
//				        	 Log.i("ExternalStorage", "-> uri=" + uri);
//							
//						}
//	        	 	}
//	        	);
//	        	
//
//	        	
//	        	} catch (Exception e) {
//	        		e.printStackTrace();
//	        	}	        	
//
//	        	System.out.println("Printed 420 and 444 buffers to files");
//	        	
//        	}
        	
        	
        }
        
        public void encodeFrameMP4(){

          	
          	ByteBuffer inputFrame = (ByteBuffer)m_buffer420;
      
  	        boolean sawInputEOS = false;
  	        //boolean sawOutputEOS = false;
  	        //MediaFormat oformat = null;
  	        //int errors = -1;
  	        //int numInputFrames = 0;
  	        //while (!sawOutputEOS //&& errors < 0) {
        	//if (!sawInputEOS) {
            int inputBufIndex = m_encoder.dequeueInputBuffer(kTimeOutUs);

            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = m_encoderInputBuffers[inputBufIndex];

                int sampleSize = m_frameSize;
                long presentationTimeUs = 0;

                if (m_numInputFrames >= 300) {
                    Log.d(TAG, "saw input EOS.");
                    sawInputEOS = true;
                    sampleSize = 0;
                } else {
                    dstBuf.clear();
                    dstBuf.put(inputFrame);
                    presentationTimeUs = m_numInputFrames*1000000/frameRate;
                    Log.d(TAG, "numInputFrames = " + m_numInputFrames + " \n");
                    m_numInputFrames++;
                }

                m_encoder.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        sampleSize,
                        presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            }
           // }

            int res = m_encoder.dequeueOutputBuffer(m_info, kTimeOutUs);

            if (res >= 0) {
                int outputBufIndex = res;
                ByteBuffer buf = m_encoderOutputBuffers[outputBufIndex];

                buf.position(m_info.offset);
                buf.limit(m_info.offset + m_info.size);
                
                byte[] outData = new byte[m_info.size];
  	            buf.get(outData);
  	            try {
					m_outputStream.write(outData, 0, outData.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
  	            Log.i("AvcEncoder", outData.length + " bytes written");

  	            //outputBufIndex = m_encoder.dequeueOutputBuffer(m_info, 0);
  	            Log.i("DEBUG", "Write Successful");

//              m_encoder.releaseOutputBuffer(outputBufIndex, false /* render */);
            } else if ((m_info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
        	    Log.d(TAG, "saw output EOS.");
        	    //sawOutputEOS = true;
    	        Log.d(TAG, "STOP ENCODING");
    	    	//m_encoder.releaseOutputBuffer(res, false);
    	        m_encoder.stop();
    	        m_encoder.release();

	    	    try
	    	    {
	    	    	System.out.println("CLOSING MEDIA FILE");
	    	    	m_outputStream.flush();
	    	    	m_outputStream.close();
	    	    	//Lets just kill the whole app here and see if the mp4 got written and its playable
	    	    	System.exit(0);
	    	    }
	    	    catch (Exception e)
	    	    {
	    	     e.printStackTrace();
	    	    }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                m_encoderOutputBuffers = m_encoder.getOutputBuffers();

                Log.d(TAG, "m_encoder output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat encformat = m_encoder.getOutputFormat();

                Log.d(TAG, "m_encoder output format has changed to " + encformat);
            } else if (res == MediaCodec.INFO_TRY_AGAIN_LATER) {
            	Log.d(TAG, "dequeueOutputBuffer timed out, numInputFrames is " + m_numInputFrames + ", exiting");
            	System.exit(-1);
            }
            else {
            	System.out.println("result is integer value " + res + " Go lookup MediaCodec constants to see what that enumerates to.");
            	Log.d(TAG, "NOT SURE WHY THIS HAPPENED, EXITING");
            	System.exit(-1);
            }
	        m_encoder.releaseOutputBuffer(res, false);
           Log.i("DEBUG", "Done with Encode function");
  	       // } //while

        }
        
        @SuppressWarnings("unused")
		public void findSupportedMediaTypes() {
            int numCodecs = MediaCodecList.getCodecCount();
            MediaCodecInfo codecInfo = null;
            for (int i = 0; i < numCodecs && codecInfo == null; i++) {
                MediaCodecInfo tempinfo = MediaCodecList.getCodecInfoAt(i);
                if (!tempinfo.isEncoder()) {
                    continue;
                }
                String[] types = tempinfo.getSupportedTypes();
                boolean found = false;
                for (int j = 0; j < types.length && !found; j++) {
                    if (types[j].equals(mimeType))
                        found = true;
                }
                if (!found)
                    continue;
                codecInfo = tempinfo;
            }
            System.out.println("Found " + codecInfo.getName() + " supporting " + mimeType);
          	//Found OMX.SEC.avc.enc supporting video/avc (Galaxy S4)
            System.out.println("Now what color formats are supported");
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
            for (int i = 0; i < capabilities.colorFormats.length; i++) {
                 System.out.println(capabilities.colorFormats[i]);
            }
        }
        
        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        	
        }

        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        	//Generate a off-screen render target
        	
        	GLES20.glGenFramebuffers(1, m_frameBufferIDs, 0);
 //       	GLES20.glGenRenderbuffers(1, m_renderBufferIDs, 0);
//        	GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, m_renderBufferIDs[0]);
 //       	GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGB565, videoWidth, videoHeight);
        	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, m_frameBufferIDs[0]);
 //       	GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, m_renderBufferIDs[0]);
        	// The texture we're going to render to
        	int[] renderedTexture = new int[1];
        	//Gnerate 1 texture and place it in entry 0 of the renderedTexture array.
        	GLES20.glGenTextures(1, renderedTexture, 0);
        	 
        	// "Bind" the newly created texture : all future texture functions will modify this texture
        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderedTexture[0]);
        	 
        	// Give an empty (blank) image to OpenGL ( hence the null for buffer parameter )
        	GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,GLES20.GL_RGBA, videoWidth, videoHeight, 0,GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        	 
        	// Poor filtering. Needed !
        	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        	
        	// Set "renderedTexture" as our colour attachement #0
        	//(target, attachment, textarget, texture, level)
        	GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderedTexture[0], 0);

        	Log.d("DEBUG", String.valueOf(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)));
        	
        	String fragShader = Shader.mOriginalFragmentShader;
        	if (m_filter.equalsIgnoreCase("original")) {
        		fragShader = Shader.mOriginalFragmentShader;
        	}
        	else if (m_filter.equalsIgnoreCase("blacknwhite")) {
        		fragShader = Shader.mGrayScaleFragShader;
        	}
        	else if (m_filter.equalsIgnoreCase("pixellate")){
        		fragShader = Shader.mPixellateFragShader;
        	}
           	else if (m_filter.equalsIgnoreCase("tile")){
        		fragShader = Shader.mTileFragShader;
        	}
           	else if (m_filter.equalsIgnoreCase("posterize")){
        		fragShader = Shader.mPosterizeFragShader;
        	}
           	else if (m_filter.equalsIgnoreCase("laplacian")){
        		fragShader = Shader.laplacianFragShaderCode;
        	}
          	else if (m_filter.equalsIgnoreCase("neon")){
        		fragShader = Shader.neonFragShaderCode;
        	}
           	else if (m_filter.equalsIgnoreCase("kuwahara")){
        		fragShader = Shader.mKuwaharaFragShader;
        	}  
           	else if (m_filter.equalsIgnoreCase("freichen")){
        		fragShader = Shader.freichenFragShaderCode;
        	}
          	else if (m_filter.equalsIgnoreCase("cartoonify")){
        		fragShader = Shader.cartoonifyFragShaderCode;
        	}
           	else if (m_filter.equalsIgnoreCase("vignette")){
        		fragShader = Shader.mVignetteFragShader;
        	}
          	else if (m_filter.equalsIgnoreCase("sepia")){
        		fragShader = Shader.mSepiaFragShader;
        	}
          	else if (m_filter.equalsIgnoreCase("savetofile")){
        		fragShader = Shader.msavetofileFragShader;
        	}
        	mProgram = createProgram(Shader.mVertexShader, fragShader);
            if (mProgram == 0) {
            	throw new RuntimeException("createProgram returned 0");
                //return;
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }


            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                                   GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                                   GLES20.GL_LINEAR);

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            mSurface = new SurfaceTexture(mTextureID);
            mSurface.setOnFrameAvailableListener(this);
            //mMediaPlayer.setOnPreparedListener(this);

           	Surface surface = new Surface(mSurface); 
           	
           	mMediaPlayer.setSurface(surface);
            surface.release();

 
            //Create the output mp4 file that we will save this to
            File f = new File(Environment.getExternalStorageDirectory(), "Download/video_encoded.mp4");
            
            try {
                m_outputStream = new BufferedOutputStream(new FileOutputStream(f));
                Log.i("AvcEncoder", "m_outputStream initialized");
                //Lets try to encode the surface into a video file
                inputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
              	inputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
              	inputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
              	inputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //75
              	inputFormat.setInteger("stride", stride);
              	inputFormat.setInteger("slice-height", sliceHeight);
                System.out.println(" about to create m_encoder for video/mp4 ");            
              	m_encoder = MediaCodec.createEncoderByType(mimeType); // need to find name in media codec list, it is chipset-specific
              	System.out.println("successfully created m_encoder for video/mp4");
              	//findSupportedMediaTypes();
              	m_encoder.configure(inputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
              	System.out.println("m_encoder configured, about to start");
              	m_encoder.start();
              	m_encoderInputBuffers = m_encoder.getInputBuffers();
              	m_encoderOutputBuffers = m_encoder.getOutputBuffers();
              	//MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
                
              	m_chromaStride = stride/2;
              	m_frameSize = stride*sliceHeight + 2*m_chromaStride*sliceHeight/2;
              	
      	        m_info = new MediaCodec.BufferInfo();
      	        
      	        //lets just do 1,000 frames and then quit, just to see if it works
      	        //we will remove this and let it do the whole video once we get it working
      	        m_numInputFrames = 0;
      	        
                m_startedEncoding = false;
                
                //JCODEC METHOD
                
                //wrap seekable byte channel around output file
                m_seekableByteCH = NIOUtils.writableFileChannel(f); //f is our output file for the video
                		
                // Muxer that will store the encoded frames
                m_muxer = new MP4Muxer(m_seekableByteCH, Brand.MP4);
                
                // Add video track to muxer
                outTrack = m_muxer.addTrackForCompressed(TrackType.VIDEO, TIMESCALE);
                m_muxer.addTimecodeTrack(25);

                // Allocate a buffer big enough to hold output frames
                _out = ByteBuffer.allocate(1920 * 1080 * 6);

                //INitialize yuv picture
                m_yuvPicture = Picture.create(videoWidth, videoHeight, ColorSpace.YUV420);
                
                // Create an instance of encoder
                encoder = new H264Encoder();

                // Encoder extra data ( SPS, PPS ) to be stored in a special place of
                // MP4
                spsList = new ArrayList<ByteBuffer>();
                ppsList = new ArrayList<ByteBuffer>();
            } catch (Exception e){ 
                e.printStackTrace();
            }
            //Now start the media player
            try {
                mMediaPlayer.prepare(); //or prepare
                m_videoDuration = mMediaPlayer.getDuration();
                m_timeStep = (int)(1.0/15.0*1000); // = 1 sec / 15 frames * 1000ms/s  
                m_currentTime = 0;
                m_previousPosition = -1;
            } catch (Exception t) {
                Log.e(TAG, "media player prepare failed");
                t.printStackTrace();
            }

            synchronized(this) {
                updateSurface = false;
            }
            System.out.println("about to start");
            mMediaPlayer.start();
            mMediaPlayer.setVolume(0, 0); //temporary until we figure out how to get the audio properly
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            updateSurface = true;
        }

        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                checkGlError("glAttachShader");
                GLES20.glAttachShader(program, pixelShader);
                checkGlError("glAttachShader");
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

//		@Override
//		public void onPrepared(MediaPlayer mp) {
//			mp.start();
//			
//		}

    }  // End of class VideoRender.

}  // End of class VideoSurfaceSave