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

public class DisplayDataView extends Activity
implements DialogInterface.OnDismissListener{
//-----------------------------------------------------------------------------
	private static final int PICTURE_DIALOG = 0;
	private DataSQLiteDB phloggingDatabase;
	private long rowId;

    private MediaPlayer recordingPlayer;
    private String recordingFileName;
    private int mainImageId;

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

        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
//-----------------------------------------------------------------------------
	public void myClickHandler(View view) {
		File audioFile;
		Intent nextActivity;

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
        	//TODO: start the EditData Intent
        	nextActivity = new Intent();
        	nextActivity.setClassName("edu.miami.c06804728.phlogging",
            		"edu.miami.c06804728.phlogging.EditDataView");
            nextActivity.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

          //Start the Edit activity
        	startActivityForResult(nextActivity,0);

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
        case R.id.picture_button_dismiss:
        	dismissDialog(PICTURE_DIALOG);
        	break;
        case R.id.delete_button:
        	phloggingDatabase.deleteRowData(rowId);
        	setResult(RESULT_OK);
        	finish();
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

    		dialogView = dialogInflator.inflate(R.layout.ui_dialog_image_display_layout,
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

	    	switch(dialogId){
	    	case PICTURE_DIALOG:
	        	//Get the pictureView
	        	pictureView = (ImageView)dialog.findViewById(R.id.image_full_size);

	        	//Get the image bitmap
	        	mainPictureFilename = getFilenameFromMediaId(mainImageId);
	        	mainPictureBitmap = loadResizedBitmap(mainPictureFilename, 300, 300, false);

	        	//Set the pictureView's image to the image bitmap
	        	pictureView.setImageBitmap(mainPictureBitmap);

	    		break;
	    	}
	    }
//-----------------------------------------------------------------------------
	 @Override
	 public void onDismiss (DialogInterface dialog){
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
        	//Get the description and rowId values
        	setViews();
        }
    }
//-----------------------------------------------------------------------------
    public void setViews(){
        ContentValues entryData;
        String title;
        String entryText;
        Bitmap mainImageThumbnail;
        long timeSinceEpoch;

        TextView titleView;
        TextView entryTextView;
        ImageView mainImageView;

  //Get the ContentValues and corresponding data
    entryData = phloggingDatabase.getEntryByRowId(rowId);
    title = entryData.getAsString("title");
    entryText = entryData.getAsString("description");
    mainImageId = entryData.getAsInteger("image_media_id");
    timeSinceEpoch = entryData.getAsLong("time");
    recordingFileName = entryData.getAsString("audio_file_name");
    //TODO: get and display location and orientation

    mainImageView = (ImageView) findViewById(R.id.image_thumbnail);

    //Set the imageView image
    if(mainImageId != -1){
    	mainImageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
    			getContentResolver(),mainImageId,
    			MediaStore.Images.Thumbnails.MICRO_KIND,null);
    	mainImageView.setImageBitmap(mainImageThumbnail);
    } else if(mainImageView.getBackground()!=null){
    	Log.i("SETVAL", "**reached**");
    	//recycleView(mainImageView);
    	mainImageView.setImageBitmap(null);
    	mainImageView.setBackgroundDrawable(null);
    	
    }

    //Set the titleView title
    titleView = (TextView) findViewById(R.id.title);
    titleView.setText(title);

    //Set the entryView entry
    entryTextView = (TextView) findViewById(R.id.entry_text);
    entryTextView.setText(entryText);
    }
}
