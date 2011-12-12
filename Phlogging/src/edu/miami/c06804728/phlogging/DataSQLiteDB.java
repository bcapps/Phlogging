package edu.miami.c06804728.phlogging;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
//=============================================================================
public class DataSQLiteDB {
//-----------------------------------------------------------------------------
    public static final String DATABASE_NAME = "PhloggingDB.db";
    private static final int DATABASE_VERSION = 2;
    private static final String PHLOGGING_TABLE = "ImageDescriptions";
    private static final String CREATE_PHLOGGING_TABLE =
"CREATE TABLE IF NOT EXISTS " + PHLOGGING_TABLE +
"(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
"title TEXT," +
"description TEXT," +
"image_media_id INTEGER, " +
"secondary_image_media_id INTEGER, " +
"audio_file_name TEXT," +
"time REAL, " +
"location TEXT, "+
"orientation TEXT"+
");";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase theDB;
//-----------------------------------------------------------------------------
    public DataSQLiteDB(Context theContext) {

        dbHelper = new DatabaseHelper(theContext);
        theDB = dbHelper.getWritableDatabase();
   }
//-----------------------------------------------------------------------------
    public void close() {

        dbHelper.close();
        theDB.close();
    }
//-----------------------------------------------------------------------------
    public boolean addRowData(ContentValues rowData) {

        return(theDB.insert(PHLOGGING_TABLE,null,rowData) >= 0);
    }
//-----------------------------------------------------------------------------
    public boolean updateRowData(long rowID,ContentValues rowData) {

        return(theDB.update(PHLOGGING_TABLE,rowData,
"_id =" + rowID,null) > 0);
    }
//-----------------------------------------------------------------------------
    public boolean deleteRowData(long rowID) {
    	ContentValues rowData;
    	String audioFileName;

    	//Delete the audio file if it exists
    	rowData = getImageById(rowID);
    	audioFileName = rowData.getAsString("audio_file_name");
    	//if it isn't null
    	if(audioFileName!=null){
    		File recordingFile = new File(audioFileName);
    		//and the file is still there
    		if(recordingFile.exists()){
    			//delete it
    			recordingFile.delete();
    		}
    	}

    	//delete the table entry
        return(theDB.delete(PHLOGGING_TABLE,"_id =" + rowID,
null) > 0);
    }
//-----------------------------------------------------------------------------
    public Cursor fetchAllData() {

        String[] fieldNames = {"_id","title","description","image_media_id", "secondary_image_media_id",
        						"audio_file_name", "time", "location", "orientation"};

        return(theDB.query(PHLOGGING_TABLE,fieldNames,null,null,
null,null,"image_media_id"));
    }
//-----------------------------------------------------------------------------
    public ContentValues getImageByImageMediaId(int imageMediaId) {

        Cursor cursor;
        ContentValues rowData;

        cursor = theDB.query(PHLOGGING_TABLE,null,
"image_media_id = " + imageMediaId,null,null,null,null);
        rowData = dataFromCursor(cursor);
        cursor.close();
        return(rowData);
    }
//-----------------------------------------------------------------------------
    public ContentValues getImageById(long id) {

        Cursor cursor;
        ContentValues rowData;

        cursor = theDB.query(PHLOGGING_TABLE,null,
"_id = " + id,null,null,null,null);
        rowData = dataFromCursor(cursor);
        cursor.close();
        return(rowData);
    }
//-----------------------------------------------------------------------------
    private ContentValues dataFromCursor(Cursor cursor) {

        String[] fieldNames;
        int index;
        ContentValues rowData;

        if (cursor != null && cursor.moveToFirst()) {
            fieldNames = cursor.getColumnNames();
            rowData = new ContentValues();
            for (index=0;index < fieldNames.length;index++) {
                if (fieldNames[index].equals("_id")) {
                    rowData.put("_id",cursor.getInt(index));
                } else if (fieldNames[index].equals("image_media_id")) {
                    rowData.put("image_media_id",cursor.getInt(index));
                } else if (fieldNames[index].equals("secondary_image_media_id")) {
                    rowData.put("secondary_image_media_id",cursor.getInt(index));
                } else if (fieldNames[index].equals("title")) {
                    rowData.put("title",cursor.getString(index));
                } else if (fieldNames[index].equals("description")) {
                    rowData.put("description",cursor.getString(index));
                }else if (fieldNames[index].equals("audio_file_name")) {
                    rowData.put("audio_file_name",cursor.getInt(index));
                }else if (fieldNames[index].equals("time")) {
                    rowData.put("time",cursor.getLong(index));
                } else if (fieldNames[index].equals("location")) {
                    rowData.put("location",cursor.getString(index));
                } else if (fieldNames[index].equals("orientation")) {
                    rowData.put("orientation",cursor.getString(index));
                }
            }
            return(rowData);
        } else {
            return(null);
        }
    }
//=============================================================================
    private static class DatabaseHelper extends SQLiteOpenHelper {
     //-------------------------------------------------------------------------
        private Context userContext;
    //-------------------------------------------------------------------------
        public DatabaseHelper(Context context) {

            super(context,DATABASE_NAME,null,DATABASE_VERSION);
            userContext = context;
        }
    //-------------------------------------------------------------------------
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(CREATE_PHLOGGING_TABLE);
        }
    //-------------------------------------------------------------------------
        @Override
        public void onOpen(SQLiteDatabase db) {

            super.onOpen(db);
        }
    //-------------------------------------------------------------------------
        public void onUpgrade(SQLiteDatabase db,int oldVersion,
int newVersion) {

            (new ContextWrapper(userContext)).deleteDatabase(DATABASE_NAME);
        }
    //-------------------------------------------------------------------------
    }
//=============================================================================
}
//=============================================================================

