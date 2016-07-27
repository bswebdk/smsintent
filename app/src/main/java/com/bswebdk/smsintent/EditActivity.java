/*
    Copyright © 2016 BSWeb.DK <bsweb@bruchhaus.dk>
    This file is part of SMSIntent (com.bswebdk.smsintent).

    SMSIntent is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SMSIntent is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SMSIntent.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.bswebdk.smsintent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;

//Activity used to edit the properties of Intents

public class EditActivity extends Activity implements OnClickListener {

	//Constants used to define values in intents used to start the
	//activity and to set a result from the activity
	public static final String EDIT_ID = "edit_id";
	public static final String GROUP_ID = "grp_id";
	public static final String IS_GROUP = "is_grp";
	public static final String ADDRESS = "addr";


	private DbOpenHelper dbHelper;
	private EditText title, address, value;
	private Button btnSave, btnCancel, btnAddAsk;
	private int editId, groupId, selStart, selEnd;
	private boolean isGroup;
	
	//Ensure that dbHelper is available
	private void requestDb() { if (dbHelper == null) dbHelper = new DbOpenHelper(this);	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		
		//Initialize values from the intent
		Bundle e = getIntent().getExtras();
		isGroup = e.getBoolean(IS_GROUP);
		editId = e.getInt(EDIT_ID);
		groupId = e.getInt(GROUP_ID);

		//Initialize view variables and set their values and listeners
		title = (EditText)findViewById(R.id.edit_title);
		address = (EditText)findViewById(R.id.edit_address);
		String addr = e.getString(ADDRESS);
		address.setText(addr == null ? "" : addr);
		value = (EditText)findViewById(R.id.edit_value);
		btnCancel = (Button)findViewById(R.id.edit_no_button);
		btnCancel.setOnClickListener(this);
		btnSave = (Button)findViewById(R.id.edit_ok_button);
		btnSave.setOnClickListener(this);
		btnAddAsk = (Button)findViewById(R.id.add_ask_button);
		btnAddAsk.setOnClickListener(this);
		
		if (isGroup) {
			
			//If we are editing a group, we must hide intent value
			//and the button used to insert a question to the value
			View v = findViewById(R.id.edit_vsep);
			v.setVisibility(View.GONE);
			value.setVisibility(View.GONE);
			btnAddAsk.setVisibility(View.GONE);

			//.. and adjust the title
			setTitle(R.string.title_activity_edit_grp);
			
		}
		
		//Set to null and create when required
		dbHelper = null;
		
	}

	@Override
	protected void onDestroy() {
		//Release the dbHelper if it is not null
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		
		if (editId > 0) {
			
			//If an editId was passed with the start intent we
			//must load the values of that intent into the fields
			requestDb();
			Cursor c = dbHelper.openEditCursor(editId);
			
			try {
				
				if (c.moveToFirst()) {
					
					title.setText(c.getString(c.getColumnIndex(DbOpenHelper.TITLE)));
					address.setText(c.getString(c.getColumnIndex(DbOpenHelper.ADDRESS)));
					
					if (!isGroup) {
						
						value.setText(c.getString(c.getColumnIndex(DbOpenHelper.VALUE)));
						
					}
					
				} else {
					
					Global.LogE("EditActivity: Invalid item id: " + editId);
					
				}
				
			} finally {
				
				c.close();
				
			}
			
		}
		
		super.onStart();

	}

	@Override
	public void onClick(View view) {
		
		if (view == btnSave) {
			
			//Request a dbHelper
			requestDb();
			
			//Validate the title
			String tit = title.getText().toString().trim();
			if (tit.length() == 0) {
				title.requestFocus();
				Global.ToastS(this, getString(R.string.edit_title_missing));
				return;
			}
			
			//Validate the value if not a group
			String val = isGroup ? "" : value.getText().toString().trim();;
			if ((!isGroup) && (val.length() == 0)) {
				value.requestFocus();
				Global.ToastS(this, getString(R.string.edit_value_missing));
				return;
			}
			
			//Address is not validated - can be empty or whatever
			String adr = address.getText().toString().trim();
			
			//Determine the type of the intent
			int typ = 0;
			if (!isGroup) {
				if (val.indexOf("{?") >= 0) {
					typ = (adr.length() > 0) ? DbOpenHelper.TYPE_RECIPIENT_QUERY : DbOpenHelper.TYPE_QUERY;;
				} else if (adr.length() > 0) typ = DbOpenHelper.TYPE_RECIPIENT;
			}
			
			//Create an Intent to use as result
			Intent i = new Intent();
			i.putExtra(IS_GROUP, isGroup);
			i.putExtra(GROUP_ID, groupId);
			
			//Store the values to the database
			if (editId < 0) editId = (int)dbHelper.insertNewRow(title.getText().toString().trim(), adr, val, typ, groupId, isGroup);
			else dbHelper.updateRow(editId, title.getText().toString().trim(), adr, val, typ);

			i.putExtra(EDIT_ID,  editId);
			
			//Set result and finish
			setResult(RESULT_OK, i);
			finish();
			
			
		} else if (view == btnCancel) {
			
			//Dismiss
			setResult(0);
			finish();
			
		} else if (view == btnAddAsk) {
			
			//If a question is selected, that question will be edited
			//and if not a new question will be inserted
			selStart = value.getSelectionStart();
			selEnd = value.getSelectionEnd();
			Intent i = new Intent(this, AskEditActivity.class);
			if (selEnd > selStart) {
				String s = value.getText().subSequence(selStart, selEnd).toString();
				i.putExtra(AskEditActivity.ASK_FOR, s);
			}

			//Launch AskEditActivity
			startActivityForResult(i, 0xFFFF);
			
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		//A result has been received from an activity
		if ((requestCode == 0xFFFF) && (resultCode == AskEditActivity.RESULT_OK)) {
			
			//It was from the AskEditActivity; get the question
			String s = data.getExtras().getString(AskEditActivity.ASK_VALUE);
			Global.LogI("EditActivity: ActivityResult = " + s);
			
			//Insert the question to the value field
			String v = value.getText().toString();
			v = v.substring(0, selStart) + s + v.substring(selEnd);
			Global.LogI("EditActivity: New value = " + v);
			value.setText(v);
				
			//And set the selection
			value.setSelection(selStart + s.length());
			
		} else {

			//Unknown result received
			Global.LogI("EditActivity: Bad ActivityResult");
			super.onActivityResult(requestCode, resultCode, data);

		}

	}

}
