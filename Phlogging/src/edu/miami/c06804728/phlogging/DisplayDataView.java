package edu.miami.c06804728.phlogging;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayDataView extends Activity {
//-----------------------------------------------------------------------------	
	private DataSQLiteDB phloggingDatabase;
	private long rowId;
	
    private MediaPlayer recordingPlayer;
    private String recordingFileName;

//-----------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.display_entry);
        
        ContentValues entryData;
        String title;
        String entryText;
        int mainImageId;
        Bitmap mainImageThumbnail;
        long timeSinceEpoch;
        
        TextView titleView;
        TextView entryTextView;
        ImageView mainImageView;
        
      	//Open database
        phloggingDatabase = new DataSQLiteDB(this);
        
        //Get the rowId from the Intent
        rowId = this.getIntent().getLongExtra("edu.miami.c06804728.phlogging.rowId", -1);

        //Something went wrong
        if(rowId==-1){
        	finish();
        }
        
        //Get the ContentValues and corresponding data
        entryData = phloggingDatabase.getEntryByRowId(rowId);
        title = entryData.getAsString("title");
        entryText = entryData.getAsString("description");
        mainImageId = entryData.getAsInteger("image_media_id");
        timeSinceEpoch = entryData.getAsLong("time");
        recordingFileName = entryData.getAsString("audio_file_name");
        Log.v("Brian", "recording name = "+recordingFileName);
        //TODO: get and display location and orientation
        
        //Set the imageView image
        if(mainImageId != -1){
        	mainImageView = (ImageView) findViewById(R.id.image_thumbnail);
        	mainImageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
        			getContentResolver(),mainImageId,
        			MediaStore.Images.Thumbnails.MICRO_KIND,null);
        	mainImageView.setImageBitmap(mainImageThumbnail);
        }
        
        //Set the titleView title
        titleView = (TextView) findViewById(R.id.title);
        titleView.setText(title);
        
        //Set the entryView entry
        entryTextView = (TextView) findViewById(R.id.entry_text);
        entryTextView.setText(entryText);
        
        //Setup the audio recorder player
        recordingPlayer = new MediaPlayer();
		recordingPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
//-----------------------------------------------------------------------------
	public void myClickHandler(View view) {
		File audioFile;
		
        switch (view.getId()) {
        case R.id.close_button:
        	finish();
            break;
        case R.id.edit_button:
        	//TODO: start the EditData Intent
        	break;
        case R.id.play_recording:
        	//if it's playing already, stop
        	if(recordingPlayer.isPlaying()){
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
        default:
        	break;
        }
	}
//-----------------------------------------------------------------------------

}
