package edu.miami.c06804728.phlogging;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
//=============================================================================
public class ContactsEmailView extends Activity {
//-----------------------------------------------------------------------------
    private static final int ACTIVITY_SEND_EMAIL = 2;
    
    private String title;
    private String entryText;
    private String imageFileName;
    private Uri imageUri;
//-----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_email_layout);
        
        //Get the values from the last intent
        title = this.getIntent().getStringExtra("edu.miami.c06804728.phlogging.title");
        entryText = this.getIntent().getStringExtra("edu.miami.c06804728.phlogging.entryText");
        imageFileName = this.getIntent().getStringExtra("edu.miami.c06804728.phlogging.imageFileName");
    	
        //Parse the filename into a Uri
        //file:// prefix needed for proper functionality
        imageUri = Uri.parse("file://"+ imageFileName);
   }
//-----------------------------------------------------------------------------
    public void myClickHandler(View view) {

        String[] emailToSendTo = new String[1];
        Intent nextIntent;

        switch (view.getId()) {
        case R.id.email:
        	//Pre-fill the fields and image attachment from intent
            emailToSendTo[0] = ((EditText)findViewById(R.id.email_address)).
            	getText().toString();
            nextIntent = new Intent(Intent.ACTION_SEND);
            nextIntent.setType("plain/text");
            if (emailToSendTo[0] != null && emailToSendTo[0].length() > 0) {
                nextIntent.putExtra(Intent.EXTRA_EMAIL,emailToSendTo);
                nextIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                nextIntent.putExtra(Intent.EXTRA_TEXT, entryText);
                if(imageFileName!=null){
                	nextIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                }
            }
            
            //Start email intent with data
            startActivityForResult(Intent.createChooser(nextIntent,
            	"Choose ..."),ACTIVITY_SEND_EMAIL);
            break;
        default:
            break;
        }
    }
//-----------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode,int resultCode,
Intent returnedIntent) {
    	//Exit upon return
    	finish();
    }
//-----------------------------------------------------------------------------
}
//=============================================================================