package com.example.rotoscope;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
//import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

//import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

public class MyGL20ImageRenderer implements GLSurfaceView.Renderer {

	Context mcontext;
	int mProgram;
	
    private FloatBuffer vertexBuffer;   // buffer holding the vertices
    private int vertexBufferPointer;
   
    private int textureBufferPointer;
    
    private float vertices[] = {
    -1.0f,  1.0f,  0.0f,        // V2 - top left
   	-1.0f, -1.0f,  0.0f,        // V1 - bottom left
    1.0f,  1.0f,  0.0f,         // V4 - top right
    1.0f, -1.0f,  0.0f        // V3 - bottom right


    };
	/** This will be used to pass in model position information. */
    //private int mPositionHandle;


	private FloatBuffer textureBuffer;  // buffer holding the texture coordinates
	private float texture[] = {
	     // Mapping coordinates for the vertices
	     0.0f, 0.0f,     // top left     (V2)
	     0.0f, 1.0f,     // bottom left  (V1)
	     1.0f, 0.0f,     // top right    (V4)
	     1.0f, 1.0f      // bottom right (V3)
	};
	/** This will be used to pass in model texture coordinate information. */
	//private int mTextureCoordinateHandle;

	/** This will be used to pass in the texture. */
	private int mTextureUniformHandle;
    private int[] textures = new int[1];

	public MyGL20ImageRenderer(Context context) {
		
		mcontext = context;
	    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
	    byteBuffer.order(ByteOrder.nativeOrder());
	    vertexBuffer = byteBuffer.asFloatBuffer();
	    vertexBuffer.put(vertices);
	    vertexBuffer.position(0);

	    byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
	    byteBuffer.order(ByteOrder.nativeOrder());
	    textureBuffer = byteBuffer.asFloatBuffer();
	    textureBuffer.put(texture);
	    textureBuffer.position(0);
	    

	}
	private void initBuffers() {
	    vertexBufferPointer = initFloatBuffer(vertices);
	    
	    textureBufferPointer = initFloatBuffer(texture);
	}
	private int initFloatBuffer(float[] data) {
	    int[] buffer = new int[1];
	    GLES20.glGenBuffers(1, buffer, 0);
	    int pointer = buffer[0];
	    if(pointer == -1) {
	        System.out.println("Error: Couldn't create buffer");
	    } else {
	    	System.out.println("Succesfully created buffer to " + pointer);
	    }
	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, pointer);
	    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4); //one float size is 4 bytes
	    byteBuffer.order(ByteOrder.nativeOrder()); //byte order must be native
	    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
	    floatBuffer.put(data);
	    floatBuffer.flip();
	    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, floatBuffer, GLES20.GL_STATIC_DRAW);
	    return pointer;
	}
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	        // Set the background frame color
	        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
	        
	        //Set the viewport size.  
	    	GLES20.glViewport(0, 0, 2444, 2444);
	        
	        //instantiate the vertex and frag shaders
	        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
	        
	        //Create program handle to vertex and frag shaders
		    mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
		    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
		    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
		    GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
			GLES20.glBindAttribLocation(mProgram, 0, "vPosition");
			GLES20.glBindAttribLocation(mProgram, 1, "a_TexCoordinate");
	        
			//Creating and loaded the vertex buffer and texture buffer
			initBuffers();
			
	        //Load the texture
	        //gl.glEnable(GL10.GL_TEXTURE_2D); 
	        AssetManager am = mcontext.getAssets();
	        try {
	        	InputStream is = am.open("jalal_strawberry_2444x2444.png");
	        
		        Bitmap bitmap = BitmapFactory.decodeStream(is);
		        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.android);
		        //ByteBuffer imageBuffer = ByteBuffer.allocateDirect(bitmap.getByteCount()); //one float size is 4 bytes
			    //imageBuffer.order(ByteOrder.nativeOrder()); //byte order must be native
			    //bitmap.copyPixelsToBuffer(imageBuffer);
			    
			    
		        // generate one texture pointer
		        gl.glGenTextures(1, textures, 0);
		        // ...and bind it to our array
		        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
	
		        // create nearest filtered texture
		        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	
		        // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		        //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		        //(target, level, internalformat, width, height, border, format, type, pixels)
		        System.out.println("width is " + bitmap.getWidth() + " and height is " + bitmap.getHeight());
		        System.out.println("max texture size is " + GLES10.GL_MAX_TEXTURE_SIZE);
		        ByteArrayOutputStream os = new ByteArrayOutputStream();
		        bitmap.compress(Bitmap.CompressFormat.WEBP, 1, os);
		        byte[] array = os.toByteArray();
		        //ByteBuffer imageBuffer = ByteBuffer.allocateDirect(array.length);
		        //imageBuffer.order(ByteOrder.nativeOrder());
		        //imageBuffer.put(array);
		        Bitmap compressedImage = BitmapFactory.decodeByteArray(array, 0, array.length);
		        //GL_PALETTE8_RGBA8_OES
		        //GLES10.glTexImage2D(GLES10.GL_TEXTURE_2D, 0, GLES10.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, imageBuffer);
		        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, compressedImage, 0);
		        
		        // Clean up
		        bitmap.recycle();
		        if (textures[0] == 0)
		        {
		            throw new RuntimeException("Error loading texture.");
		        }
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        }
	    }

		public void draw(GL10 gl) {
			// Set our vertex/frag shader program.
	        GLES20.glUseProgram(mProgram);
			
			// Set program handles for drawing.
			//mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
			//mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
	        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
	        // Set the active texture unit to texture unit 0.
	        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        
	        // Bind the texture to this unit.
	        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
	        
	        vertexBuffer.rewind();
	        textureBuffer.rewind();
	        
	        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
	        GLES20.glUniform1i(mTextureUniformHandle, 0);
	        	 	        
		    // Point to our buffers
		    //gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		    //gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		    
	       	        
		    // Point to our vertex buffer
		    //gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		    //gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
	        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferPointer);
	        //first parameter is the ID number of the attribute of the vertex
	        // we set attribute id = 0 to the position and 1 to the texture coordin
	        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);
	        GLES20.glEnableVertexAttribArray(0);

	        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBufferPointer);
	        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);
	        GLES20.glEnableVertexAttribArray(1);
	        
		    // Set the face rotation
		    //GLES20.glFrontFace(GL10.GL_CW);
		    
		    // Draw the vertices as triangle strip
		    GLES20.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

		    System.out.println(GLUtils.getEGLErrorString(gl.glGetError()));
		    
		    //Disable the client state before leaving
		    //gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		    //gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		}
	    public void onDrawFrame(GL10 gl) {
	        // Redraw background color
	        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	        draw(gl);
	    }

	    public void onSurfaceChanged(GL10 unused, int width, int height) {
	        //GLES20.glViewport(0, 0, width, height);
	    	GLES20.glViewport(0, 0, 2444, 2444);
	    }

	    public static int loadShader(int type, String shaderCode){

	        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	        int shader = GLES20.glCreateShader(type);

	        // add the source code to the shader and compile it
	        GLES20.glShaderSource(shader, shaderCode);
	        GLES20.glCompileShader(shader);
	        
	     // Get the compilation status.
	        final int[] compileStatus = new int[1];
	        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
	     
	        // If the compilation failed, delete the shader.
	        if (compileStatus[0] == 0)
	        {
	            GLES20.glDeleteShader(shader);
	            shader = 0;
	        }
	   
	     
		    if (shader == 0)
		    {
		        throw new RuntimeException("Error creating shader.");
		    }

	        return shader;
	    }
	    private final String vertexShaderCode =
	    	    "attribute vec4 vPosition;" +
	    	    "attribute vec2 a_TexCoordinate;" + //input texel coordinate
	    	    "varying vec2 v_TexCoordinate;" + //this is used to pass it to the frag shader
	    	    "void main() {" +
	    	    "  gl_Position = vPosition;" +
	    	    "  v_TexCoordinate = a_TexCoordinate;" +
	    	    "}";

	    private final String fragmentShaderCode =
	    	    "precision mediump float;" +
	    	    "uniform sampler2D u_Texture;" +    // The input texture.
	    	    "varying vec2 v_TexCoordinate;" +   // Interpolated texture coordinate per fragment.
	    	    "void main() {" +
	    	    "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate); " +
	    	    "}";

}
