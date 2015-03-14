package com.example.rotoscope;

//import java.io.InputStream;
//import android.view.View.OnClickListener;
//import android.widget.ImageView;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

//import android.content.res.AssetFileDescriptor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.util.Log;



public class MainActivity extends Activity{

    private int REQUEST_ID = 1;
    private String m_filtername;
    //private static final int HALF = 2;

	//This is a key for key-value pairs called extras
	//you can pass these as parameters when you call an Intent
	//Always use your app's package name as a prefix, especially
	//if your app interacts with other apps.
	public final static String EXTRA_MESSAGE = "com.example.rotoscope.MESSAGE";
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //findViewById(R.id.startVideoOGL).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
 
    public void startCreateAppNavIcons (View view) {
    	Intent makeAppNavIconsIntent = new Intent(this, CreateAppNavIconsActivity.class);
    	startActivity(makeAppNavIconsIntent);
    	
    }

    public void startVideoOGL (View view) {
    	Intent grabvideoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    	if (Build.VERSION.SDK_INT <19){
	    	grabvideoIntent.setAction(Intent.ACTION_GET_CONTENT);
	    	grabvideoIntent.setType("video/*");
	    	//THe tag of the button pushed will choose which filter 
	    	String theTag = (String)view.getTag();
	    	m_filtername = theTag;
	    	startActivityForResult(grabvideoIntent, REQUEST_ID);
	    	//startActivity(videoIntent);
    	}
    	else {
	    	grabvideoIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);//.ACTION_GET_CONTENT);
	    	grabvideoIntent.addCategory(Intent.CATEGORY_OPENABLE);
	    	grabvideoIntent.setType("video/*");
            grabvideoIntent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
	    	//THe tag of the button pushed will choose which filter 
	    	String theTag = (String)view.getTag();
	    	m_filtername = theTag;
	    	startActivityForResult(grabvideoIntent, REQUEST_ID);
    	}
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent firstIntent) {
//            after user selects a video from the gallery, this function is automatically called from grabvideoIntent
    	super.onActivityResult(requestCode, resultCode, firstIntent);
    	try {
            if (requestCode == REQUEST_ID && resultCode == Activity.RESULT_OK) {
               	//System.out.println("inside onActivityResult of MainActivity");

            	Intent oglVideoIntent = new Intent(this, VideoActivity.class);
            	oglVideoIntent.putExtra("chosenvideoURI", firstIntent.getDataString());

            	String filter;
	        	if (m_filtername.equalsIgnoreCase("Original Video")) {
	        		filter = "original";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Black and White")) {
	        		filter = "blacknwhite";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Pixellate")) {
	        		filter = "pixellate";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Tile")) {
	        		filter = "tile";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Posterize")) {
	        		filter = "posterize";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Laplacian")) {
	        		filter = "laplacian";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Kuwahara")) {
	        		filter = "kuwahara";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Neon")) {
	        		filter = "neon";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("FreiChen")) {
	        		filter = "freichen";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Cartoonify")) {
	        		filter = "cartoonify";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Vignette")) {
	        		filter = "vignette";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("Sepia")) {
	        		filter = "sepia";
	        	}
	        	else if (m_filtername.equalsIgnoreCase("SaveToFile")) {
	        		filter = "savetofile";
	        	}
	        	else {
	        		throw new InvalidFilterException("Could not understand the provided m_filtername: " + m_filtername);
	        		
	        	}
            	oglVideoIntent.putExtra("filter", filter);
            	startActivity(oglVideoIntent);
            }
    	}
		catch (InvalidFilterException ife) {
			ife.printStackTrace();
		}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	public class InvalidFilterException extends Exception {

		  /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InvalidFilterException(String message){
		     super(message);
	    }

	}
}
