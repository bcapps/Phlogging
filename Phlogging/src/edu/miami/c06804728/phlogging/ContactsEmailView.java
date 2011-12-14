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

        String nameToSearchFor;
        String[] emailToSendTo = new String[1];
        Intent nextIntent;

        switch (view.getId()) {
        case R.id.search:
            ((TextView)findViewById(R.id.email_address)).setText(null);
            nameToSearchFor = ((EditText)findViewById(R.id.search_for)).
            	getText().toString();
            if (nameToSearchFor != null && nameToSearchFor.length() > 0) {
                searchForEmailAddress(nameToSearchFor);
            } else {
                selectEmailAddress();
            }
            break;
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
    private void searchForEmailAddress(String nameToSearchFor) {
        
        String[] projection = new String[] {
        		ContactsContract.CommonDataKinds.Email.DATA,
        		ContactsContract.Contacts.DISPLAY_NAME
        };
        Cursor contactsCursor;
        String contactName;
        int nameIndex;
        boolean contactFound;
        TextView emailAddress;
        
        contactsCursor = managedQuery(
        		ContactsContract.CommonDataKinds.Email.CONTENT_URI,projection,null,null,null);
        startManagingCursor(contactsCursor);
        contactFound = false;
        emailAddress = (TextView)findViewById(R.id.email_address);
        if (contactsCursor.moveToFirst()) {
            nameIndex = contactsCursor.getColumnIndex(
            		ContactsContract.Contacts.DISPLAY_NAME);
            do {
                contactName = contactsCursor.getString(nameIndex);
                if (contactName.contains(nameToSearchFor)) {
                    contactFound = true;
                }
            } while (!contactFound && contactsCursor.moveToNext());
        }
        stopManagingCursor(contactsCursor);
        if (contactFound) {
            emailAddress.setText(
            		contactsCursor.getString(contactsCursor.getColumnIndex(
            		ContactsContract.CommonDataKinds.Email.DATA)));
        } else {
            emailAddress.setHint(nameToSearchFor +" not found in emails");
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
                    if (contactName != null && contactName.length() > 0) {
                        ((TextView)findViewById(R.id.search_for)).setText(contactName);
                        searchForEmailAddress(contactName);
                    } else {
                        emailAddress.setHint("No name for contact");
                    }
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