package edu.miami.c06804728.phlogging;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
//=============================================================================
public class ContactsEmailView extends Activity {
//-----------------------------------------------------------------------------
    private static final int ACTIVITY_SELECT_CONTACT = 1;
    private static final int ACTIVITY_SEND_EMAIL = 2;
//-----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_email_layout);
        
   }
//-----------------------------------------------------------------------------
    public void myClickHandler(View view) {

        String[] emailToSendTo = new String[1];
        Intent nextIntent;

        switch (view.getId()) {
        case R.id.email:
            emailToSendTo[0] = ((EditText)findViewById(R.id.email_address)).
            	getText().toString();
            nextIntent = new Intent(Intent.ACTION_SEND);
            nextIntent.setType("plain/text");
            if (emailToSendTo[0] != null && emailToSendTo[0].length() > 0) {
                nextIntent.putExtra(Intent.EXTRA_EMAIL,emailToSendTo);
            }
            startActivityForResult(Intent.createChooser(nextIntent,
            	"Choose ..."),ACTIVITY_SEND_EMAIL);
            break;
        default:
            break;
        }
    }
//-----------------------------------------------------------------------------
    public void selectEmailAddress() {
        
        Intent nextIntent;
        
        nextIntent = new Intent(Intent.ACTION_PICK,
ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(nextIntent,ACTIVITY_SELECT_CONTACT);
    }
//-----------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode,int resultCode,
Intent returnedIntent) {
        
        Uri contactData;
        Cursor contactsCursor;
        String contactName;
        TextView emailAddress;
        
        super.onActivityResult(requestCode,resultCode,returnedIntent);
        emailAddress = (TextView)findViewById(R.id.email_address);
        switch (requestCode) {
        case ACTIVITY_SELECT_CONTACT:
            if (resultCode == Activity.RESULT_OK){
                contactData = returnedIntent.getData();
                contactsCursor = managedQuery(contactData,null,null,null,null);
                if (contactsCursor.moveToFirst()){
                    contactName = contactsCursor.getString(
                    		contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                } else {
                    emailAddress.setHint("WEIRD");
                }
                contactsCursor.close();
            } else {
                emailAddress.setHint("None selected");
            }
            break;
        }
    }
//-----------------------------------------------------------------------------
}
//=============================================================================