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
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
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
	private static final int ACTIVITY_SELECT_PICTURE = 3;
	private static final int ACTIVITY_CAMERA_APP = 4;

	private long rowId;
	private long creationTime;

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

        //Open database
        phloggingDatabase = new DataSQLiteDB(this);

        //Get the variables sent from last activity
        rowId = this.getIntent().getLongExtra("edu.miami.c06804728.phlogging.rowId", -1);

		//Get the default button background - this is to be reused when resetting the button
		//This is a hack because android doesn't support resetting the background drawable
		defaultButtonBackground = findViewById(R.id.add_main_pic_button).getBackground();

        //No corresponding entry found in database- enter Create Mode
        if(rowId==-1){
        	mainPictureMediaId = -1;
        	setDefaultRecordFileName();
        } else{
        	loadExistingEntry(rowId);
        }


        //Not recording
        isRecording = false;

        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		//Set current dialog to null
		currentDialog = null;
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
			//Stop playing
			if(recordingPlayer.isPlaying()){
				recordingPlayer.stop();
			}
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
    	Intent galleryIntent;
    	EditText descriptionView;
    	ContentValues phlogContent;
    	TextView titleView;
    	String title;
    	String description;
    	File audioFile;
    	ImageView pictureView;
    	Button mainPictureButton;
    	Intent cameraIntent;

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
        	phlogContent = new ContentValues();

        	phlogContent.put("title",title);
        	phlogContent.put("description",description);
        	phlogContent.put("time", creationTime);
        	phlogContent.put("image_media_id",mainPictureMediaId);
        	//TODO: phlogEntry.put("location", );
        	//TODO: phlogEntry.put("orientation", );

        	//if the file exists, put it in the Intent
        	if(recordFileName!=null){
		    	audioFile = new File(recordFileName);
	        	if(audioFile.exists()){
	        		phlogContent.put("audio_file_name",recordFileName);
	        	}
        	}

        	if(rowId==-1){
	        	//Add a new entry to the database
	        	phloggingDatabase.addRowData(phlogContent);
        	} else {
        		//Update the existing entry in the database
        		phloggingDatabase.updateRowData(rowId, phlogContent);
        	}
	        setResult(RESULT_OK);

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
        case R.id.button_take_photo:
        	cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent,ACTIVITY_CAMERA_APP);
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
        	pictureView.setBackgroundResource(R.drawable.no_photo);

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
    		//Get the pictureView
        	pictureView = (ImageView)dialog.findViewById(R.id.image_full_size);

    		//If there is no image, set to blank image
        	if(mainPictureMediaId == -1){
        		pictureView.setBackgroundResource(R.drawable.no_photo);
        		break;
        	}

        	//Get the image bitmap
        	mainPictureFilename = getFilenameFromMediaId(mainPictureMediaId);
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
    public void onActivityResult(int requestCode,int resultCode,Intent returnIntent) {
    	super.onActivityResult(requestCode,resultCode,returnIntent);

        ImageView pictureView;
        Button mainPictureButton;
        Uri selectedURI;
        Bitmap selectedPicture;
        String selectedImagePath;
        Bitmap photoBitmap;

        switch (requestCode) {
        case ACTIVITY_SELECT_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                pictureView = (ImageView)currentDialog.findViewById(R.id.image_full_size);
                selectedURI = returnIntent.getData();
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
        case ACTIVITY_CAMERA_APP:
        	if (resultCode == Activity.RESULT_OK) {
        		pictureView = (ImageView)currentDialog.findViewById(R.id.image_full_size);
                photoBitmap = (Bitmap)returnIntent.getExtras().get("data");
                selectedImagePath = MediaStore.Images.Media.insertImage(getContentResolver(),
                								photoBitmap, "title", "description");
                selectedURI = Uri.parse(selectedImagePath);

                //Set the media ID, to be added to the database
                mainPictureMediaId = getMediaId(selectedURI);

                //Set the thumbnail onto the button
                mainPictureButton = (Button) findViewById(R.id.add_main_pic_button);

                try {
                	//Recycle the view
                	recycleView(pictureView);

                	//Set the pictureView
                    pictureView.setImageBitmap(photoBitmap);

                    //Set the thumbnail
                    mainPictureButton.setBackgroundDrawable(new BitmapDrawable(photoBitmap));
                    mainPictureButton.setText("");
                } catch (Exception e) {
                	//Error
                }
        	}
        	break;
        default:
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
    // where if the image is too big, it just crashes.
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
    private void setDefaultRecordFileName(){
        String formattedTime;
        String recordDirName;
        File recordDir;

		//Setup the date format
        creationTime = System.currentTimeMillis();
        SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy.HH.mm.ss");
		formattedTime = df.format(new Date(creationTime));

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

    }
//-----------------------------------------------------------------------------
    private void loadExistingEntry(long rowId){
        ContentValues entryData;
        String title;
        String entryText;
        Bitmap mainImageThumbnail;
        Button mainPictureButton;

    	 //Get the ContentValues and corresponding data
        entryData = phloggingDatabase.getEntryByRowId(rowId);
        title = entryData.getAsString("title");
        entryText = entryData.getAsString("description");
        mainPictureMediaId = entryData.getAsInteger("image_media_id");
       // timeSinceEpoch = entryData.getAsLong("time");
        recordFileName = entryData.getAsString("audio_file_name");
        //If the filename is invalid, set it to the default
        if(recordFileName == null || recordFileName.length()<=0){
        	setDefaultRecordFileName();
        	/*((Button)findViewById(R.id.clear_button)).setEnabled(false);
        	((Button)findViewById(R.id.play_button)).setEnabled(false);*/
        }
        //TODO: get and display location and orientation

        mainPictureButton = (Button) findViewById(R.id.add_main_pic_button);

        //Set the imageView image
        if(mainPictureMediaId != -1){
        	mainImageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
        			getContentResolver(),mainPictureMediaId,
        			MediaStore.Images.Thumbnails.MICRO_KIND,null);
        	mainPictureButton = (Button) findViewById(R.id.add_main_pic_button);
        	try {
                //Set the thumbnail
                mainPictureButton.setBackgroundDrawable(new BitmapDrawable(mainImageThumbnail));
                mainPictureButton.setText("");
            } catch (Exception e) {
            	//Error
            }
        }

        //Set the titleView title text
    	EditText titleView = (EditText) findViewById(R.id.title);
    	titleView.setText(title);

    	//Set the entryView text
    	EditText descriptionView = (EditText) findViewById(R.id.entry_text);
    	descriptionView.setText(entryText);
    }
//-----------------------------------------------------------------------------
}
