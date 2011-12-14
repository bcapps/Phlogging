package edu.miami.c06804728.phlogging;

import java.io.File;
import java.io.IOException;

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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class DisplayDataView extends Activity
implements DialogInterface.OnDismissListener{
//-----------------------------------------------------------------------------
	private static final int EDIT_ACTIVITY = 1;
    private static final int DELETE_DIALOG = 2;
	private static final int PICTURE_DIALOG = 3;
	private static final int SECOND_PICTURE_DIALOG = 4;
	private static final int VIDEO_DIALOG = 5;
	private DataSQLiteDB phloggingDatabase;
	
	private long rowId;

    private MediaPlayer recordingPlayer;
    private String recordingFileName;
    private String videoFileName;
    private int mainImageId;
    private int secondImageId;
    private int currentDialogId;
    private Dialog currentDialog;
//-----------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.display_entry);

      	//Open database
        phloggingDatabase = new DataSQLiteDB(this);

        //Get the rowId from the Intent
        rowId = this.getIntent().getLongExtra("edu.miami.c06804728.phlogging.rowId", -1);

        //Something went wrong
        if(rowId==-1){
        	finish();
        }

        //Initialize all the views with their values
        setViews();
        currentDialog = null;
        currentDialogId = -1;

        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
//-----------------------------------------------------------------------------
	public void myClickHandler(View view) {
		File audioFile;
		Intent nextActivity;
		VideoView videoView;
		File videoFile;
		Intent emailIntent;
		ContentValues entryData;
		String title;
    	String entryText;
    	

        switch (view.getId()) {
        case R.id.close_button:
        	//if playing, stop
        	if(recordingPlayer.isPlaying()){
        		recordingPlayer.stop();
        	}
        	setResult(RESULT_OK);
        	finish();
            break;
        case R.id.edit_button:
        	//if playing, stop
        	if(recordingPlayer.isPlaying()){
        		recordingPlayer.stop();
        	}

        	nextActivity = new Intent();
        	nextActivity.setClassName("edu.miami.c06804728.phlogging",
            		"edu.miami.c06804728.phlogging.EditDataView");
            nextActivity.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

            //Start the Edit activity
        	startActivityForResult(nextActivity,EDIT_ACTIVITY);

        	break;
        case R.id.play_recording:
        	//if it's playing already, stop
        	if(recordingPlayer.isPlaying() || recordingFileName == null){
        		recordingPlayer.stop();
        		break;
        	}

        	//if not playing and the file exists, play it
	    	audioFile = new File(recordingFileName);
	    	if(audioFile.exists()){
	    		recordingPlayer.reset();
	    		try {
	    			recordingPlayer.setDataSource(recordingFileName);
	    			recordingPlayer.prepare();
	            } catch (IOException e) {
	            	Toast.makeText(this,"There was an error playing the recording. ",
	            			Toast.LENGTH_LONG).show();
	            }
	    		recordingPlayer.start();
	    	}
        	break;
        case R.id.image_thumbnail:
        	if(mainImageId >= 0){
        		showDialog(PICTURE_DIALOG);
        	}
        	break;
        case R.id.second_photo_thumbnail:
        	if(secondImageId >= 0){
        		showDialog(SECOND_PICTURE_DIALOG);
        	}
        	break;
        case R.id.picture_button_dismiss:
        	dismissDialog(currentDialogId);
        	break;
        case R.id.delete_button:
        	showDialog(DELETE_DIALOG);
        	break;
        case R.id.video_button:
        	//error checking
        	if(videoFileName == null){
        		break;
        	}
        	//if not playing and the file exists, play it
	    	videoFile = new File(videoFileName);
	    	if(videoFile.exists()){
        		showDialog(VIDEO_DIALOG);
	    	}
        	break;
        case R.id.video_button_dismiss:
        	dismissDialog(VIDEO_DIALOG);
        	break;
        case R.id.video_play:
        	videoView = (VideoView)currentDialog.findViewById(R.id.video_full_size);
        	videoView.start();
        	break;
        case R.id.video_stop:
        	//error checking
        	if(videoFileName == null){
        		break;
        	}
        	videoView = (VideoView)currentDialog.findViewById(R.id.video_full_size);
        	if(videoView.isPlaying()){
        		videoView.stopPlayback();
        	}
        	break;
        case R.id.add_email:
        	emailIntent = new Intent();
        	emailIntent.setClassName("edu.miami.c06804728.phlogging",
    		"edu.miami.c06804728.phlogging.ContactsEmailView");
        	
        	//Get the ContentValues and corresponding data
        	entryData = phloggingDatabase.getEntryByRowId(rowId);
        	title = entryData.getAsString("title");
        	entryText = entryData.getAsString("description");
        	int imageMediaId = entryData.getAsInteger("image_media_id");
        	String imageFileName = getFilenameFromMediaId(imageMediaId);
        	
        	emailIntent.putExtra("edu.miami.c06804728.phlogging.title", title);
        	emailIntent.putExtra("edu.miami.c06804728.phlogging.entryText", entryText);
        	emailIntent.putExtra("edu.miami.c06804728.phlogging.imageFileName", imageFileName);
        	
        	startActivity(emailIntent);
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
    	case SECOND_PICTURE_DIALOG:
    		//Inflate the dialog and set it to the builder's view
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_dialog_image_display_layout,
    				(ViewGroup)findViewById(R.id.dialog_root));
    		dialogBuilder.setView(dialogView);
    		break;
    	case DELETE_DIALOG:
    		 dialogBuilder.setMessage("Are you sure you want to permanantly delete this phlog entry?");
             dialogBuilder.setPositiveButton("Yes",deleteListener);
             dialogBuilder.setNegativeButton("No",deleteListener);
    		break;
    	case VIDEO_DIALOG:
    		//Inflate the dialog and set it to the builder's view
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_dialog_video_display_layout,
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
	    	String pictureFilename;
	    	Bitmap pictureBitmap;
	    	ImageView pictureView;
	    	VideoView videoView;

	    	currentDialog = dialog;
	    	currentDialogId = dialogId;
	    	
	    	switch(dialogId){
	    	case PICTURE_DIALOG:
	        	//Get the pictureView
	        	pictureView = (ImageView)dialog.findViewById(R.id.image_full_size);

	        	//Get the image bitmap
	        	pictureFilename = getFilenameFromMediaId(mainImageId);
	        	pictureBitmap = loadResizedBitmap(pictureFilename, 300, 300, false);

	        	//Set the pictureView's image to the image bitmap
	        	pictureView.setImageBitmap(pictureBitmap);

	    		break;
	    	case SECOND_PICTURE_DIALOG:
	        	//Get the pictureView
	        	pictureView = (ImageView)dialog.findViewById(R.id.image_full_size);

	        	//Get the image bitmap
	        	pictureFilename = getFilenameFromMediaId(secondImageId);
	        	pictureBitmap = loadResizedBitmap(pictureFilename, 300, 300, false);

	        	//Set the pictureView's image to the image bitmap
	        	pictureView.setImageBitmap(pictureBitmap);

	    		break;
	    	case VIDEO_DIALOG:
	    		videoView = (VideoView)dialog.findViewById(R.id.video_full_size);
	    		videoView.setVideoPath(videoFileName);
	    		break;
	    	}
	    }
//-----------------------------------------------------------------------------
	 @Override
	 public void onDismiss (DialogInterface dialog){
		 currentDialog = null;
		 currentDialogId = -1;
	 }
//-----------------------------------------------------------------------------
	 @Override
	 public void onDestroy() {
		 super.onDestroy();

		 recordingPlayer.release();
		 phloggingDatabase.close();
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
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (resultCode == Activity.RESULT_OK) {
        	//Reload the views based on the new data
        	setViews();
        }
    }
//-----------------------------------------------------------------------------
    public void setViews(){
    	ContentValues entryData;
    	String title;
    	String entryText;
    	Bitmap imageThumbnail;

    	TextView titleView;
    	TextView entryTextView;
    	ImageView imageView;

    	//Get the ContentValues and corresponding data
    	entryData = phloggingDatabase.getEntryByRowId(rowId);
    	title = entryData.getAsString("title");
    	entryText = entryData.getAsString("description");
    	mainImageId = entryData.getAsInteger("image_media_id");
    	secondImageId = entryData.getAsInteger("secondary_image_media_id");
    	recordingFileName = entryData.getAsString("audio_file_name");
    	videoFileName = entryData.getAsString("video_file_name");
    	
    	//TODO: get and display location and orientation

    	imageView = (ImageView) findViewById(R.id.image_thumbnail);

    	//Set the imageView image
    	if(mainImageId != -1){
    		imageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
    				getContentResolver(),mainImageId,
    				MediaStore.Images.Thumbnails.MICRO_KIND,null);
    		imageView.setImageBitmap(imageThumbnail);
    	} else if(imageView.getBackground()!=null){
    		imageView.setImageBitmap(null);
    		imageView.setBackgroundDrawable(null);
    	}
    	
    	//Set the secondary image
    	if(secondImageId != -1){
    		imageView = (ImageView) findViewById(R.id.second_photo_thumbnail);
    		imageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
    				getContentResolver(),secondImageId,
    				MediaStore.Images.Thumbnails.MICRO_KIND,null);
    		imageView.setImageBitmap(imageThumbnail);
    	} else if(imageView.getBackground()!=null){
    		imageView.setImageBitmap(null);
    		imageView.setBackgroundDrawable(null);
    	}

    	//Set the titleView title
    	titleView = (TextView) findViewById(R.id.title);
    	titleView.setText(title);

    	//Set the entryView entry
    	entryTextView = (TextView) findViewById(R.id.entry_text);
    	entryTextView.setText(entryText);
    }
//-----------------------------------------------------------------------------
    DialogInterface.OnClickListener deleteListener =
    	new DialogInterface.OnClickListener() {
    @Override
	public void onClick(DialogInterface dialog,int whatWasClicked) {

        switch (whatWasClicked) {
        case DialogInterface.BUTTON_POSITIVE:
        	phloggingDatabase.deleteRowData(rowId);
        	setResult(RESULT_OK);
            finish();
            break;
        case DialogInterface.BUTTON_NEGATIVE:
        	dismissDialog(DELETE_DIALOG);
        	break;
        default:
            break;
        }
    }
    };
//-----------------------------------------------------------------------------
}
