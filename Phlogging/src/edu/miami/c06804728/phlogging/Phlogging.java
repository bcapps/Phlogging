package edu.miami.c06804728.phlogging;

import java.sql.Date;
import java.text.SimpleDateFormat;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

//=============================================================================
public class Phlogging extends Activity
implements OnItemClickListener, SimpleCursorAdapter.ViewBinder,
DialogInterface.OnDismissListener{
//-----------------------------------------------------------------------------
    private DataSQLiteDB phloggingDatabase;
    private Cursor entryCursor;
    private Cursor imageMediaCursor;

    private MediaPlayer musicPlayer;

    private static final int SETTINGS_DIALOG = 1;
    private static final int HELP_DIALOG = 2;
    private static final int ACTIVITY_EDIT = 3;

    private TextToSpeech mySpeaker;
    private MediaPlayer recordingPlayer;

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

        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
   }
//-----------------------------------------------------------------------------
    @Override
    public void onDestroy() {
        super.onDestroy();
        recordingPlayer.release();
        entryCursor.close();
        phloggingDatabase.close();
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
        	//if there is no image, simply return
        	if(imageIndex == -1){
        		recycleView(thumbnailView);
        		return(true);
        	}
        	
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
        	if(title.length()>0){
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
    @Override
    public void onItemClick(AdapterView<?> parent,View view,int position, long rowId) {
    	Intent nextActivity;
    	nextActivity = new Intent();
    	nextActivity.setClassName("edu.miami.c06804728.phlogging",
        		"edu.miami.c06804728.phlogging.DisplayDataView");
        nextActivity.putExtra("edu.miami.c06804728.phlogging.rowId", rowId);

    	//Start the Edit activity
    	startActivityForResult(nextActivity,ACTIVITY_EDIT);
    }
//-----------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	switch (requestCode) {
        case ACTIVITY_EDIT:
        	if (resultCode == Activity.RESULT_OK) {
                //Refresh the listView
                entryCursor.requery();
        	}
        	break;
        default:
        	break;
    	}
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
    	imageData = phloggingDatabase.getEntryByRowId(rowId);
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
    	imageData = phloggingDatabase.getEntryByRowId((int) rowId);
    	description = imageData.getAsString("description");

    	return description; //can be null
    }
//-----------------------------------------------------------------------------
    public String getAudioFileNameFromRowId(long rowId){
    	ContentValues imageData;

    	imageData = phloggingDatabase.getEntryByRowId(rowId);
    	return imageData.getAsString("audio_file_name");
    }
//-----------------------------------------------------------------------------
    public void myClickHandler(View view) {
    	Intent nextActivity;
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

        switch (view.getId()) {
        case R.id.settings_button:
        	showDialog(SETTINGS_DIALOG);
            break;
        case R.id.create_button:
        	nextActivity = new Intent();
        	nextActivity.setClassName("edu.miami.c06804728.phlogging",
            		"edu.miami.c06804728.phlogging.EditDataView");
        	//Start the Edit activity
        	startActivityForResult(nextActivity,ACTIVITY_EDIT);

            break;
        case R.id.sort_title:
        	//create a new cursor and refetch the data with sort order
        	entryCursor = phloggingDatabase.fetchAllData("title");
        	
        	//create a new adapter and set it to the list
        	theList = (ListView)findViewById(R.id.the_list);
            cursorAdapter = new SimpleCursorAdapter(this,
              R.layout.list_item_layout,
                  entryCursor,displayFields,displayViews);
            cursorAdapter.setViewBinder(this);
            theList.setAdapter(cursorAdapter);
        	break;
        case R.id.sort_date:
        	//create a new cursor and refetch the data with sort order
        	entryCursor = phloggingDatabase.fetchAllData("time");
        	
        	//create a new adapter and set it to the list
        	theList = (ListView)findViewById(R.id.the_list);
            cursorAdapter = new SimpleCursorAdapter(this,
              R.layout.list_item_layout,
                  entryCursor,displayFields,displayViews);
            cursorAdapter.setViewBinder(this);
            theList.setAdapter(cursorAdapter);
        	break;
        case R.id.settings_button_dismiss:
        	dismissDialog(SETTINGS_DIALOG);
            break;
        case R.id.help_button:
        	showDialog(HELP_DIALOG);
        	break;
        case R.id.help_dismiss:
        	dismissDialog(HELP_DIALOG);
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
    	case SETTINGS_DIALOG:
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_settings_dialog_layout,
    				(ViewGroup)findViewById(R.id.dialog_root));
    		dialogBuilder.setView(dialogView);
    		break;
    	case HELP_DIALOG:
    		dialogInflator = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

    		dialogView = dialogInflator.inflate(R.layout.ui_help_dialog_layout,
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