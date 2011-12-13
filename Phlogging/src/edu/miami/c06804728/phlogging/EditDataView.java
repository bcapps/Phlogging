package edu.miami.c06804728.phlogging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//=============================================================================
public class EditDataView extends Activity
implements DialogInterface.OnDismissListener{
//-----------------------------------------------------------------------------
	private DataSQLiteDB phloggingDatabase;
	private static final int PICTURE_DIALOG = 0;
	private static final int CREATE_MODE = 1;
	private static final int EDIT_MODE = 2;
	private static final int ACTIVITY_SELECT_PICTURE = 3;
	
	private String description;
	private long rowId;
	private int mode;

	private MediaRecorder recorder;
    private String recordFileName;
    private MediaPlayer recordingPlayer;
    
    private Dialog currentDialog;
    private int mainPictureMediaId;
    
    private Drawable defaultButtonBackground;

    private boolean isRecording;
//-----------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_entry_layout);
        /****************************************
         * Things to be added later
         *
         * long time = System.currentTimeMillis()
         * time is the time since Jan. 1, 1970.
         * Then use Time.set(time) and we can get a nicely formatted string
         *
         *****************************************/
//        String imageFilename;
//        Bitmap imageBitmap;
//        ImageView imageView;
//        EditText descriptionView;
        String formattedTime;
        String recordDirName;
        File recordDir;
        
        //Create database
        phloggingDatabase = new DataSQLiteDB(this);

        /*//Get the variables sent from last activity
        imageFilename = this.getIntent().getStringExtra("edu.miami.c06804728.phlogging.image_file_name");
        description = this.getIntent().getStringExtra("edu.miami.c06804728.phlogging.description");
        rowId = this.getIntent().getLongExtra("edu.miami.c06804728.phlogging.rowId", -1);

        //Something went wrong
        if(rowId==-1){
        	finish();
        }

        //Set the ImageView image
        int maxWidth, maxHeight;

        imageView = (ImageView) findViewById(R.id.image_large);
        if(imageFilename!=null){
        	//Get the max values, load and set the Bitmap
        	maxWidth = getResources().getInteger(R.integer.image_max_width);
    		maxHeight = getResources().getInteger(R.integer.image_max_height);

        	imageBitmap = loadResizedBitmap(imageFilename,
    				maxWidth, maxHeight, false);
        	imageView.setImageBitmap(imageBitmap);
        }

        //Set the EditText text
        descriptionView = (EditText) findViewById(R.id.edit_text_field);
        if(description!=null){
        	descriptionView.setText(description);
        }*/
        
        //Setup the date format
        SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy.HH.mm.ss");
		formattedTime = df.format(new Date(System.currentTimeMillis()));
        
        //Set the recording fileName
        recordDirName = Environment.getExternalStorageDirectory().
        		getAbsolutePath() + "/Android/data/edu.miami.c06804728.phlogging/files";
        recordDir = new File (recordDirName);
        if (!recordDir.exists() && !recordDir.mkdirs()) {
        	Toast.makeText(this,
        			"ERROR: Could not make temporary storage directory "+recordDir,
        			Toast.LENGTH_LONG).show();
        	finish();
        }
        recordFileName = recordDir + "/" +  formattedTime + getString(R.string.record_file_name);

        //Not recording
        isRecording = false;
        
        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		//Set current dialog to null
		currentDialog = null;
		mainPictureMediaId = -1;
		
		//Get the default button background - this is to be reused when resetting the button
		//This is a hack because android doesn't support resetting the background drawable
		defaultButtonBackground = findViewById(R.id.add_main_pic_button).getBackground();
	}
//-----------------------------------------------------------------------------
	public void audioClickHandler(View view) {
		File audioFile;
		
		switch(view.getId()){
		case R.id.record_button:
			//Setup the media recorder
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setOutputFile(recordFileName);
			try {
				recorder.prepare();
			} catch (Exception e) {
				Toast.makeText(this,
						"There was an error preparing the audio recording.",
						Toast.LENGTH_LONG).show();
			}
			//Start the recording and set isRecording to true
			recorder.start();
			isRecording = true;
			break;
		case R.id.stop_button:
			//Stop recording
			stopRecording();
			break;
		case R.id.clear_button:
			//Stop recording
			stopRecording();

			//If the recordedFile exists, delete it
			File recordedFile = new File(recordFileName);
			if(recordedFile.exists()){
				if(!recordedFile.delete()){
					Toast.makeText(this,
							"There was an error clearing the audio recording.",
							Toast.LENGTH_LONG).show();
				}
			}
			break;
		case R.id.play_button:
			//Stop recording
			stopRecording();
			
			//if the file exists
	    	audioFile = new File(recordFileName);
	    	if(audioFile.exists()){
	    		recordingPlayer.reset();
	    		try {
	    			recordingPlayer.setDataSource(recordFileName);
	    			recordingPlayer.prepare();
	            } catch (IOException e) {
	            	Toast.makeText(this,"There was an error playing the recording. " +
	            						"Try waiting a few seconds after recording.",
	            			Toast.LENGTH_LONG).show();
	            }
	    		recordingPlayer.start();
	    	}
			break;
		default:
			break;
		}
	}
//-----------------------------------------------------------------------------
	public void myClickHandler(View view) {
    	Intent returnIntent;
    	Intent galleryIntent;
    	EditText descriptionView;
    	ContentValues phlogEntry;
    	TextView titleView;
    	String title;
    	String description;
    	File audioFile;
    	ImageView pictureView;
    	Button mainPictureButton;

        switch (view.getId()) {
        case R.id.cancel_button:
        	finish();
        	break;
        case R.id.phlog_button:
        	//Get the values from their fields
        	titleView = (TextView) findViewById(R.id.title);
        	title = titleView.getText().toString().trim();
        	
        	descriptionView = (EditText) findViewById(R.id.entry_text);
        	description = descriptionView.getText().toString().trim();
        	
        	//Update the database
        	phlogEntry = new ContentValues();
        	
        	phlogEntry.put("title",title);
        	phlogEntry.put("description",description);
        	phlogEntry.put("time", System.currentTimeMillis());
        	phlogEntry.put("image_media_id",mainPictureMediaId);
        	//TODO: phlogEntry.put("location", );
        	//phlogEntry.put("orientation", );
        	
        	//if the file exists, put it in the Intent
	    	audioFile = new File(recordFileName);
        	if(audioFile.exists()){
        		phlogEntry.put("audio_file_name",recordFileName);
        	}
        	
        	//Add it to the database
        	phloggingDatabase.addRowData(phlogEntry);
        	
        	//Set the blank return Intent and exit
        	//Tells Phlogging.java to requery if result_ok
        	returnIntent = new Intent();
	        setResult(RESULT_OK,returnIntent);

	        finish();
        	break;
        case R.id.picture_button_dismiss:
        	dismissDialog(PICTURE_DIALOG);
        	break;
        case R.id.add_main_pic_button:
        	showDialog(PICTURE_DIALOG);
        	break;

        case R.id.button_choose_gallery:
        	galleryIntent = new Intent(Intent.ACTION_PICK,
        			MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        	startActivityForResult(galleryIntent,ACTIVITY_SELECT_PICTURE);
        	break;
        case R.id.button_delete_photo:
        	//If there was already no image, don't do anything
        	if(mainPictureMediaId == -1){
        		break;
        	}
        	
        	//Reset the main photo id to not found
        	mainPictureMediaId = -1;
        	
        	//Remove the image
        	pictureView = (ImageView)currentDialog.findViewById(R.id.image_full_size);
        	recycleView(pictureView);
        	
        	//Remove thumbnail and reset the button
            mainPictureButton = (Button) findViewById(R.id.add_main_pic_button);
            mainPictureButton.setBackgroundDrawable(defaultButtonBackground);
            mainPictureButton.setText("Click me to add a photo");
        	
        	break;
        default:
            break;
        }
    }
//-----------------------------------------------------------------------------
    @Override
	protected Dialog onCreateDialog(int dialogId) {
    	AlertDialog.Builder dialogBuilder;
    	View dialogView;
    	LayoutInflater dialogInflator;
    	AlertDialog theDialog;
    	
    	dialogBuilder = new AlertDialog.Builder(this);
    	
    	switch (dialogId) {
    	case PICTURE_DIALOG:
    		//Inflate the dialog and set it to the builder's view
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_picture_dialog_layout,
    				(ViewGroup)findViewById(R.id.dialog_root));
    		dialogBuilder.setView(dialogView);
    		break;
    	default:
    		break;
    	}
    	theDialog = dialogBuilder.create();
    	theDialog.setOnDismissListener(this);
		
    	return(theDialog);
    }
//-----------------------------------------------------------------------------
    @Override
	protected void onPrepareDialog (int dialogId, Dialog dialog){
    	String mainPictureFilename;
    	Bitmap mainPictureBitmap;
    	ImageView pictureView;
    	
    	//set the current dialog
    	currentDialog = dialog;
    			
    	switch(dialogId){
    	case PICTURE_DIALOG:
    		//If there is no image, don't do anything
        	if(mainPictureMediaId == -1){
        		break;
        	}
        	//Get the pictureView
        	pictureView = (ImageView)dialog.findViewById(R.id.image_full_size);
        	
        	//Get the image bitmap
        	mainPictureFilename = getFilenameFromMediaId(mainPictureMediaId);
        	Log.v("Brian", "mainpicFileName = "+mainPictureFilename);
        	mainPictureBitmap = loadResizedBitmap(mainPictureFilename, 300, 300, false);
        	
        	//Set the pictureView's image to the image bitmap
        	pictureView.setImageBitmap(mainPictureBitmap);

    		break;
    	}
    }
//-----------------------------------------------------------------------------
    @Override
	public void onDismiss (DialogInterface dialog){
		currentDialog = null;
    }
//-----------------------------------------------------------------------------  
    @Override
    public void onDestroy() {
        super.onDestroy();
        recordingPlayer.release();
        phloggingDatabase.close();
    }
//-----------------------------------------------------------------------------
	public void stopRecording(){
		//Stop the recorder
    	if(isRecording){
    		recorder.stop();
    		recorder.release();
    		isRecording = false;
    	}
	}
//-----------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
    	super.onActivityResult(requestCode,resultCode,data);
    	
        ImageView pictureView;
        Button mainPictureButton;
        Uri selectedURI;
        Bitmap selectedPicture;
        String selectedImagePath;

        switch (requestCode) {
        case ACTIVITY_SELECT_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                pictureView = (ImageView)currentDialog.findViewById(R.id.image_full_size);
                selectedURI = data.getData();
                selectedImagePath = getPath(selectedURI);
                
                //Set the media ID, to be added to the database
                mainPictureMediaId = getMediaId(selectedURI);
                
                //Set the thumbnail onto the button
                mainPictureButton = (Button) findViewById(R.id.add_main_pic_button);
                
                try {
                	//Recycle the view
                	recycleView(pictureView);
                	
                	//Set the pictureView
                    selectedPicture = loadResizedBitmap(selectedImagePath, 300, 300, false);                    
                    pictureView.setImageBitmap(selectedPicture);
                    
                    //Set the thumbnail
                    mainPictureButton.setBackgroundDrawable(new BitmapDrawable(selectedPicture));
                    mainPictureButton.setText("");
                } catch (Exception e) {
                	//Error
                }
            }
            break;
        }
    }
//-----------------------------------------------------------------------------
    //Convenience method to get the image filename from a rowId
    public String getFilenameFromMediaId(int imageMediaId){
    	boolean imageFound;
    	int imageIDIndex;
    	int imageDataIndex;
    	String imageFileName;
    	Cursor imageMediaCursor;

    	imageFound = false;
    	
    	//Setup
    	String[] queryFields = {
                BaseColumns._ID,
                //The data field will be used later to obtain the fileName
                MediaColumns.DATA
            };

    	//Setup the query
        imageMediaCursor = managedQuery(
            			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,queryFields,null,null,
            			MediaStore.Images.Media.DEFAULT_SORT_ORDER);
    	
    	//Get the relevant MediaStore column indexes
    	imageIDIndex = imageMediaCursor.getColumnIndex(BaseColumns._ID);
    	imageDataIndex = imageMediaCursor.getColumnIndex(MediaColumns.DATA);

    	//Use the cursor to iterate through images to find one that matches the id
        if (imageMediaCursor.moveToFirst()) {
            do {
            	//Compare the imageMediaId with id's in the media store
                imageFound = (imageMediaId == imageMediaCursor.getInt(imageIDIndex));
            } while (!imageFound && imageMediaCursor.moveToNext());
        }
        if (imageFound) {
        	//if the image was found, get the fileName
        	imageFileName = imageMediaCursor.getString(imageDataIndex);

        	return imageFileName;
        }
        
        //If the image isn't found, return null
        return null;
    }
//-----------------------------------------------------------------------------
    //Helper method to get the file path from a Uri
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndex(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
//-----------------------------------------------------------------------------
    //Helper method to get the media id from a Uri
    public int getMediaId(Uri uri) {
        String[] projection = { MediaStore.Images.Media._ID };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndex(MediaStore.Images.Media._ID);
        cursor.moveToFirst();
        return cursor.getInt(column_index);
    }
//-----------------------------------------------------------------------------
    //Generic code to recycleView. Taken verbatim from Geoff's site.
    private void recycleView(View view) {

        ImageView imageView;
        Bitmap imageBitmap;
        BitmapDrawable imageBitmapDrawable;

        if (view != null) {
            if (view instanceof ImageView) {
                imageView = (ImageView)view;
                if ((imageBitmapDrawable =
                		(BitmapDrawable)imageView.getDrawable()) != null &&
                		(imageBitmap = imageBitmapDrawable.getBitmap()) != null) {
                    imageBitmap.recycle();
                }
                imageView.setImageURI(null);
                imageView.setImageBitmap(null);
            }
            if ((imageBitmapDrawable =
            		(BitmapDrawable)view.getBackground()) != null &&
            		(imageBitmap = imageBitmapDrawable.getBitmap()) != null) {
                imageBitmap.recycle();
            }
            view.setBackgroundDrawable(null);
            System.gc();
        }
    }
//-----------------------------------------------------------------------------

	//This code is from the internet. It fixes a common Android issue
    //where if the image is too big, it just crashes.
    public static Bitmap loadResizedBitmap( String filename, int width, int height, boolean exact ) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile( filename, options );
        if ( options.outHeight > 0 && options.outWidth > 0 ) {
            options.inJustDecodeBounds = false;
            options.inSampleSize = 2;
            while (    options.outWidth  / options.inSampleSize > width
                    && options.outHeight / options.inSampleSize > height ) {
                options.inSampleSize++;
            }
            options.inSampleSize--;

            bitmap = BitmapFactory.decodeFile( filename, options );
            if ( bitmap != null && exact ) {
                bitmap = Bitmap.createScaledBitmap( bitmap, width, height, false );
            }
        }
        return bitmap;
    }
//-----------------------------------------------------------------------------
}
