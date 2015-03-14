package com.example.rotoscope;

//import java.io.FileDescriptor;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
//import android.os.ParcelFileDescriptor;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
//import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v4.app.NavUtils;
import android.util.Log;

public class VideoActivity extends Activity {
	private static final String TAG = "MediaPlayerVidSurfAct";//MediaPlayerVideoSurfaceActivity tag was too long

	protected Resources mResources;
	private VideoSurfaceView mVideoViewer = null;
	private VideoSurfaceSave mVideoSaver = null;
	private String mMode = null;
	private MediaPlayer mMediaPlayer = null;
	
	@Override
	@SuppressLint("NewApi")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);
		// Show the Up button in the action bar.
		// Make sure we're running on Honeycomb or higher to use ActionBar APIs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
        mResources = getResources();
        mMediaPlayer = new MediaPlayer();

        try {
	        	//AssetManager am = this.getAssets();
	        	Intent intent = getIntent();
	        	String filter = intent.getStringExtra("filter");

	        	Uri theuri = Uri.parse(intent.getStringExtra("chosenvideoURI"));

                AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(theuri, "r"); //intent.getData().;
	            ///AssetFileDescriptor afd = am.openFd("20121015_142407.mp4");//mResources.openRawResourceFd(am.open("gedu-balloon.mp4"))
	            mMediaPlayer.setDataSource(
	                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
	            afd.close();
	            //create an GLSurfaceView instance and set it
	            //as the ContentView for this Activity
	            
	            //TEMPORARY: If click SaveToFile, use VideoSurfaceSave (Render off screen)
	            mMode = filter;
	            if (mMode.equalsIgnoreCase("SaveToFile"))
	            {
	            	mVideoSaver = new VideoSurfaceSave(this, mMediaPlayer, filter);
	            	setContentView(mVideoSaver);
	            	
	            }
	            else 
	            {
	            	mVideoViewer = new VideoSurfaceView(this, mMediaPlayer, filter);
	            	setContentView(mVideoViewer);
	            }
	    	
        } 	

        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

	}
	
	protected void resetCurrentMode() {
		try {
	        if (mMode.equalsIgnoreCase("SaveToFile"))
	        {
	        	mVideoSaver.resetMediaPlayer();
	        } else {
	        	mVideoViewer.resetMediaPlayer();
	        }
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	

    @Override
    protected void onResume() {
        super.onResume();
        if (mMode.equalsIgnoreCase("SaveToFile"))
        {
        	mVideoSaver.onResume();
        } else {
        	mVideoViewer.onResume();
        }
    }

//    public void playVideo() throws Exception {
//    	System.out.println("playVideo was called, who called it?");
//        mVideoViewer.startTest();
//    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_video, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			resetCurrentMode();
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onNavigateUp(){
		resetCurrentMode();
		return true;
	}
	@Override
	public void onBackPressed(){
		resetCurrentMode();
		NavUtils.navigateUpFromSameTask(this);
	}
	
	@Override
    protected void onStop() 
    {
        Log.d(TAG, "MYonStop is called");
        new Exception().printStackTrace();
        resetCurrentMode();
        super.onStop();		
    }

}
