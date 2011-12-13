package edu.miami.c06804728.phlogging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
//=============================================================================
public class Phlogging extends Activity
implements OnItemClickListener, OnItemLongClickListener, SimpleCursorAdapter.ViewBinder,
DialogInterface.OnDismissListener, TextToSpeech.OnInitListener,TextToSpeech.OnUtteranceCompletedListener {
//-----------------------------------------------------------------------------
    private DataSQLiteDB phloggingDatabase;
    private Cursor entryCursor;
    private Cursor imageMediaCursor;

    private MediaPlayer musicPlayer;

    private static final int PICTURE_DIALOG = 3;
    private static final int SETTINGS_DIALOG = 1;
    private static final int ACTIVITY_EDIT = 2;

    private Bitmap dialogImageBitmap;
    private String dialogDescription;
    private String dialogAudioFileName;

    private TextToSpeech mySpeaker;
    private MediaPlayer recordingPlayer;

    private boolean musicWasPlaying;
//-----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {

        SimpleCursorAdapter cursorAdapter;
        ListView theList;
        String[] displayFields = {
        	"image_media_id",
            "title",
            "time"
        };

        int[] displayViews = {
        	R.id.image_thumbnail,
            R.id.title,
            R.id.time
        };

        super.onCreate(savedInstanceState);
        setContentView(R.layout.phlogging_main_layout);

        //Create database and add any new images
        phloggingDatabase = new DataSQLiteDB(this);
        //updateImageDBFromContent();

        //Setup the database cursor and listeners
        entryCursor = phloggingDatabase.fetchAllData();
        theList = (ListView)findViewById(R.id.the_list);
        cursorAdapter = new SimpleCursorAdapter(this,
          R.layout.list_item_layout,
              entryCursor,displayFields,displayViews);
        cursorAdapter.setViewBinder(this);
        theList.setAdapter(cursorAdapter);
        theList.setOnItemClickListener(this);
        theList.setOnItemLongClickListener(this);

        //Setup TTS
        mySpeaker = new TextToSpeech(this,this);

        //Initialize the dialogBitmap and description and audio filename to null
        dialogImageBitmap = null;
        dialogDescription = null;
        dialogAudioFileName = null;

        //No music playing
        musicWasPlaying = false;

        //Play a random song from the music library
        //playRandomSong();

        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
   }
//-----------------------------------------------------------------------------
    @Override
    public void onDestroy() {
        super.onDestroy();

        musicPlayer.release();
        recordingPlayer.release();
        entryCursor.close();
        phloggingDatabase.close();
        mySpeaker.shutdown();
    }
//-----------------------------------------------------------------------------
    @Override
	public void onInit(int status) {
    	//When the TTS is initialized, check if it's ready to speak
        if (status == TextToSpeech.SUCCESS &&
        			mySpeaker.isLanguageAvailable(Locale.US) ==
        			TextToSpeech.LANG_COUNTRY_AVAILABLE &&
        			mySpeaker.setOnUtteranceCompletedListener(this) == TextToSpeech.SUCCESS) {
            Toast.makeText(this,"Now you can talk",Toast.LENGTH_SHORT).show();
        } else {
        	//if not, display an error and exit
            Toast.makeText(this,"You need to install TextToSpeech",
            		Toast.LENGTH_LONG).show();
            finish();
        }
    }
//----------------------------------------------------------------------------
    @Override
	public void onUtteranceCompleted(String utteranceId) {

    	File audioFile;

    	//if the recording is null
    	if(dialogAudioFileName == null){
    		return;
    	}
    	//if the file exists
    	audioFile = new File(dialogAudioFileName);
    	if(audioFile.exists()){
    		recordingPlayer.reset();
    		try {
    			recordingPlayer.setDataSource(dialogAudioFileName);
    			recordingPlayer.prepare();
            } catch (IOException e) {
            	Toast.makeText(this,"There was an error playing the recording",
            			Toast.LENGTH_LONG).show();
            }
    		recordingPlayer.start();
    	}
    	else{
    		dialogAudioFileName = null;
    	}
    }
//-----------------------------------------------------------------------------
    //Scan for new images and add them to the database
    private void updateImageDBFromContent() {

        String[] queryFields = {
            BaseColumns._ID,
            //The data field will be used later to obtain the fileName
            MediaColumns.DATA
        };
        ContentValues imageData;
        int imageMediaId;

        //Setup the query
        imageMediaCursor = managedQuery(
        			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,queryFields,null,null,
        			MediaStore.Images.Media.DEFAULT_SORT_ORDER);
        startManagingCursor(imageMediaCursor);

        //Iterate through the list, adding any images not already there
        if (imageMediaCursor.moveToFirst()) {
            do {
                imageMediaId = imageMediaCursor.getInt(
                		imageMediaCursor.getColumnIndex(BaseColumns._ID));
                //Check if the image_media_id doesn't exist in the database
                if (phloggingDatabase.getImageByImageMediaId(imageMediaId) == null) {
                	//If not, add a new entry with this image_media_id
                    imageData = new ContentValues();
                    imageData.put("image_media_id",imageMediaId);

                    phloggingDatabase.addRowData(imageData);
                }
            } while (imageMediaCursor.moveToNext());
        }
    }
//-----------------------------------------------------------------------------
    //Intercept the creation of each item in the listView
    @Override
	public boolean setViewValue(View view,Cursor cursor,int columnIndex) {
        int imageIndex;
        String title;
        long timeSinceEpoch;
        Bitmap thumbnailBitmap;
        ImageView thumbnailView;
        TextView titleView;
        TextView timeView;

        //if on the image id column
        if (columnIndex == cursor.getColumnIndex("image_media_id")) {
        	//Get the thumbnail view and the thumbnail
        	thumbnailView = (ImageView)view.findViewById(R.id.image_thumbnail);
        	imageIndex = cursor.getInt(columnIndex);
        	thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(
        			getContentResolver(),imageIndex,
        			MediaStore.Images.Thumbnails.MICRO_KIND,null);

        	if (thumbnailBitmap != null) {
        		recycleView(thumbnailView);
        		//set the thumbnailView's image if not null
        		thumbnailView.setImageBitmap(thumbnailBitmap);
        	}
            return(true);
        }
        //if on the description column
        else if(columnIndex == cursor.getColumnIndex("title")){
        	//Get the description view and the description
        	titleView = (TextView)view.findViewById(R.id.title);
        	title = cursor.getString(columnIndex);

        	//Set the description, including default if description is null
        	if(title != null){
        		titleView.setText(title);
        	}
        	else{
        		titleView.setText("Untitled");
        	}
        	return(true);
        }
        //if on the audio column
        else if(columnIndex == cursor.getColumnIndex("time")){
        	//Get the checkbox view and the description
        	timeView = (TextView)view.findViewById(R.id.time);
        	timeSinceEpoch = cursor.getLong(columnIndex);

        	//Check the checkbox if the value isn't null
        	if(timeSinceEpoch > 0){
        		//Time timeFormatter = new Time();
        		//timeFormatter.set(timeSinceEpoch);
        		//Set the formatted time to the timeView
        		//timeView.setText(timeFormatter.format("MM-dd-yyyy hh:mm"));

        		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm");
        		timeView.setText(df.format(new Date(timeSinceEpoch)));
        	}

        	return(true);
        }
        else {
            return(false);
        }
    }
//-----------------------------------------------------------------------------
    //Play a random song from the music library, or included song if none
    private void playRandomSong(){
    	Cursor audioCursor;
    	String[] queryFields = {
                MediaColumns.DATA
         };
    	Random random;
    	int randomPosition;
    	int audioDataIndex;
    	String audioFilename;

    	//create the default player
    	musicPlayer = MediaPlayer.create(this,R.raw.passionpit);

    	//Setup the query
    	audioCursor = managedQuery(
    			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,queryFields,null,null,
    			MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    	startManagingCursor(audioCursor);

    	//if the user has at least 1 song
    	if(audioCursor.getCount()>0){
    		random = new Random();
    		//generate a random position from [0 to count)
    		randomPosition = random.nextInt(audioCursor.getCount());

    		//Get the correct column, move to random position, and obtain filename
    		audioDataIndex = audioCursor.getColumnIndex(MediaColumns.DATA);
    		audioCursor.moveToPosition(randomPosition);
    		audioFilename = audioCursor.getString(audioDataIndex);

    		try {
    			musicPlayer.reset();
                musicPlayer.setDataSource(audioFilename);
                musicPlayer.prepare();
            } catch (IOException e) {
                //if there is a problem loading the file, play default
            	//reset first just to be safe
            	musicPlayer.reset();
            	musicPlayer = MediaPlayer.create(this,R.raw.passionpit);
            }
    	}
    	//play the loaded file (and loop)
    	musicPlayer.setLooping(true);
    	musicPlayer.start();

    	//Stop managing the cursor, it's no longer needed
    	stopManagingCursor(audioCursor);
    }
//-----------------------------------------------------------------------------
    @Override
    public void onItemClick(AdapterView<?> parent,View view,int position, long rowId) {
    	//get the dialog bitmap
    	dialogImageBitmap = getBitmapFromRowId(rowId);

    	if(dialogImageBitmap != null){
    		//get the dialog description
    		dialogDescription = getDescriptionFromRowId(rowId);

    		//get the dialog audio filename
    		dialogAudioFileName = getAudioFileNameFromRowId(rowId);

    		//Pause the music
    		if(musicPlayer.isPlaying()){
    			musicPlayer.pause();
    		}

        	//Show dialog
        	showDialog(PICTURE_DIALOG);
    	}
    }
//-----------------------------------------------------------------------------
    @Override
    public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long rowId) {
    	String rowImageFilename;
    	String description;
    	Intent nextActivity;

    	//Get the full size bitmap
    	rowImageFilename = getFilenameFromRowId(rowId);
    	if(rowImageFilename == null){
    		return true;
    	}

    	//Get the description
    	description = getDescriptionFromRowId(rowId);

    	//Put filename, description, and rowId on new activity
    	nextActivity = new Intent();
        nextActivity.setClassName("edu.miami.c06804728.phlogging",
        		"edu.miami.c06804728.phlogging.EditDataView");
        nextActivity.putExtra("edu.miami.c06804728.phlogging.image_file_name", rowImageFilename);
        nextActivity.putExtra("edu.miami.c06804728.phlogging.description", description);
        nextActivity.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

    	//Pause the music
    	if(musicPlayer.isPlaying()){
    		musicPlayer.pause();
    	}

    	//Start the Edit activity
    	startActivityForResult(nextActivity,ACTIVITY_EDIT);

    	return true;
    }
//-----------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	String description;
    	long rowId;
    	String recordFileName;
    	ContentValues imageData;

    	switch (requestCode) {
        case ACTIVITY_EDIT:
        	if (resultCode == Activity.RESULT_OK) {
        		Log.v("Brian", "RESULT OKAY");
        		//Get the description and rowId values
//        		description = data.getStringExtra("edu.miami.c06804728.phlogging.description");
//        		rowId = data.getLongExtra("edu.miami.c06804728.phlogging.rowId", -1);
//        		recordFileName = data.getStringExtra("edu.miami.c06804728.phlogging.recordFileName");
//
//        		//Wrong rowId
//                if(rowId==-1){
//                	break;
//                }
//
//                //Update the description and recordFileName based on the rowId
//                imageData = phloggingDatabase.getImageById(rowId);
//                imageData.put("description", description);
//                imageData.put("audio_file_name", recordFileName);
//                phloggingDatabase.updateRowData(rowId, imageData);

                //Refresh the listView
                entryCursor.requery();
        	}
        	break;
        default:
        	break;
    	}
    	//Start the music again
    	//musicPlayer.start();
    }
//-----------------------------------------------------------------------------
    //Convenience method to get the image filename from a rowId
    public String getFilenameFromRowId(long rowId){
    	boolean imageFound;
    	int imageIDIndex;
    	int imageDataIndex;
    	String imageFileName;
    	ContentValues imageData;
    	int imageMediaId;

    	imageFound = false;
    	//Get the relevant MediaStore column indexes
    	imageIDIndex = imageMediaCursor.getColumnIndex(BaseColumns._ID);
    	imageDataIndex = imageMediaCursor.getColumnIndex(MediaColumns.DATA);

    	//Get the image_media_id from the rowId (_id)
    	imageData = phloggingDatabase.getImageById(rowId);
    	imageMediaId = imageData.getAsInteger("image_media_id").intValue();

    	//Use the cursor to iterate through images to find one that matches the id
        if (imageMediaCursor.moveToFirst()) {
            do {
            	//Compare the imageMediaId with id's in the media store
                imageFound = (imageMediaId == imageMediaCursor.getLong(imageIDIndex));
            } while (!imageFound && imageMediaCursor.moveToNext());
        }
        if (imageFound) {
        	//if the image was found, get the fileName
        	imageFileName = imageMediaCursor.getString(imageDataIndex);

        	return imageFileName;
        }
        else {
        	//Delete any image entry that can't be found
        	phloggingDatabase.deleteRowData(rowId);
        	Toast.makeText(this, "The image has been deleted",
        			Toast.LENGTH_LONG).show();
        	entryCursor.requery();
        	return null;
        }
    }
//-----------------------------------------------------------------------------
    //Convenience method to get a Bitmap from a rowId
    public Bitmap getBitmapFromRowId(long rowId){
    	Bitmap rowImageBitmap;
    	String imageFileName;
    	int maxWidth, maxHeight;

    	//Get the filename from the rowId
    	imageFileName = getFilenameFromRowId(rowId);
    	if(imageFileName!=null){
    		//Get and return the Bitmap using the filename
    		maxWidth = getResources().getInteger(R.integer.image_max_width);
    		maxHeight = getResources().getInteger(R.integer.dialog_image_max_height);

    		rowImageBitmap = loadResizedBitmap(imageFileName,
    				maxWidth, maxHeight, false);
    		return rowImageBitmap;
    	}

    	return null;
    }
//-----------------------------------------------------------------------------
    //Convenience method to get a description from a rowId
    public String getDescriptionFromRowId(long rowId){
    	ContentValues imageData;
    	String description;

    	//get the description
    	imageData = phloggingDatabase.getImageById((int) rowId);
    	description = imageData.getAsString("description");

    	return description; //can be null
    }
//-----------------------------------------------------------------------------
    public String getAudioFileNameFromRowId(long rowId){
    	ContentValues imageData;

    	imageData = phloggingDatabase.getImageById(rowId);
    	return imageData.getAsString("audio_file_name");
    }
//-----------------------------------------------------------------------------
    public void myClickHandler(View view) {
    	Intent nextActivity;

        switch (view.getId()) {
        case R.id.settings_button:
        	showDialog(SETTINGS_DIALOG);
            break;
        case R.id.create_button:
        	nextActivity = new Intent();
        	nextActivity.setClassName("edu.miami.c06804728.phlogging",
            		"edu.miami.c06804728.phlogging.EditDataView");
            //nextActivity.putExtra("edu.miami.c06804728.phlogging.image_file_name", rowImageFilename);
            //nextActivity.putExtra("edu.miami.c06804728.phlogging.description", description);
            //nextActivity.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

        	//Start the Edit activity
        	startActivityForResult(nextActivity,ACTIVITY_EDIT);

            break;
        case R.id.settings_button_dismiss:
        	dismissDialog(SETTINGS_DIALOG);
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
    		//dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);


    	//	dialogView = dialogInflator.inflate(R.layout.ui_pcture_dialog_layout,
    	//			(ViewGroup)findViewById(R.id.dialog_root));
    	//	dialogBuilder.setView(dialogView);

    		break;
    	case SETTINGS_DIALOG:
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_settings_dialog_layout,
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
    	/*old code for other dialog - reuse later in DisplayView
    	ImageView imageView;
    	HashMap<String,String> speechParameters;

    	imageView = (ImageView)dialog.findViewById(R.id.image_full_size);

    	//Display the bitmap image in the dialog
    	if (dialogImageBitmap != null) {
    		recycleView(imageView);
    		imageView.setImageBitmap(dialogImageBitmap);
    	}

    	//Speak the description
    	speechParameters = new HashMap<String,String>();
        speechParameters.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"WHAT_I_SAID");

    	if (dialogDescription != null && dialogDescription.length() > 0) {
            mySpeaker.speak(dialogDescription,TextToSpeech.QUEUE_ADD,speechParameters);
        } else {
        	mySpeaker.speak("No description yet.",TextToSpeech.QUEUE_ADD,
        			speechParameters);
        }
        */
    }
//-----------------------------------------------------------------------------
    @Override
	public void onDismiss (DialogInterface dialog){
    	/*old code for other dialog - reuse later in DisplayView
    	//Reset the dialog properties
    	dialogImageBitmap = null;
    	dialogDescription = null;
    	dialogAudioFileName = null;

    	//Stop TTS
    	if(mySpeaker.isSpeaking()){
    		mySpeaker.stop();
    	}

    	//Stop recording
    	if(recordingPlayer.isPlaying()){
    		recordingPlayer.stop();
    	}

    	//Start the music again
    	musicPlayer.start();
    	*/
    }
//-----------------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        //Start music, if it was playing
        if(musicWasPlaying){
        	musicPlayer.start();
        }
    }

//-----------------------------------------------------------------------------
//    @Override
//    protected void onPause() {
//        super.onPause();
//        musicWasPlaying = musicPlayer.isPlaying();
//        //Pause playing music
//        if(musicWasPlaying){
//        	musicPlayer.pause();
//        }
//        //Stop TTS
//        if(mySpeaker.isSpeaking()){
//        	mySpeaker.stop();
//        }
//    }
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
    //where if the bitmap is too big, it just crashes, getting
    //the dreaded java.lang.OutOfMemoryError, even with recycling the view.
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
}
//=============================================================================