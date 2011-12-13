package edu.miami.c06804728.phlogging;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;

public class DisplayDataView extends Activity {
//-----------------------------------------------------------------------------	
	private DataSQLiteDB phloggingDatabase;
	private long rowId;
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
        String recordingFileName;
        
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
	}
}
