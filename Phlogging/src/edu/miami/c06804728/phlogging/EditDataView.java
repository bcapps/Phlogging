package edu.miami.c06804728.phlogging;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//=============================================================================
public class EditDataView extends Activity
implements DialogInterface.OnDismissListener{
//-----------------------------------------------------------------------------
	private DataSQLiteDB phloggingDatabase;
	private static final int PICTURE_DIALOG = 1;
	
	private String description;
	private long rowId;

	private MediaRecorder recorder;
    private String recordFileName;

    private boolean isRecording;
//-----------------------------------------------------------------------------
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
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy.HH.mm.ss");
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
	}
//-----------------------------------------------------------------------------
	public void myClickHandler(View view) {
    	Intent returnIntent;
    	EditText descriptionView;
    	ContentValues phlogEntry;
    	TextView titleView;
    	String title;
    	String description;

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
        	//phlogEntry.put("image_media_id",imageMediaId); //This will be when we have the image id
        	phlogEntry.put("title",title);
        	phlogEntry.put("description",description);
        	phlogEntry.put("time", System.currentTimeMillis());
        	//phlogEntry.put("location", );
        	//phlogEntry.put("orientation", );
        	
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
        /*case R.id.audio_record:
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
        case R.id.audio_stop:
        	//Stop recording
        	stopRecording();
            break;
        case R.id.audio_clear:
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
        case R.id.save_button:
        	//Stop recording
        	stopRecording();

        	//Get the current description
        	descriptionView = (EditText) findViewById(R.id.edit_text_field);
        	description = descriptionView.getText().toString();

        	//Set the values for the return Intent and exit
        	returnIntent = new Intent();
	        returnIntent.putExtra("edu.miami.c06804728.phlogging.description", description);
	        returnIntent.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

	        //Check if we cleared the file, if so set the filename to null
	        File recordFile = new File(recordFileName);
	        if(!recordFile.exists()){
	        	recordFileName = null;
	        }
        	returnIntent.putExtra("edu.miami.c06804728.phlogging.recordFileName", recordFileName);

	        setResult(RESULT_OK,returnIntent);

	        finish();
            break;*/
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
    }
//-----------------------------------------------------------------------------
    @Override
	public void onDismiss (DialogInterface dialog){
    	
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
