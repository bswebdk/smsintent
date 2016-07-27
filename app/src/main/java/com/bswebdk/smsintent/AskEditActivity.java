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
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

//Activity used to edit a question that can be implemented in an
//intent / template. This activity is launched from EditActivity

public class AskEditActivity extends Activity implements OnClickListener {

	//Constants used to identify values passed in the startActivity intent
	public static final String ASK_VALUE = "ask_val";
	public static final String ASK_FOR = "ask_for";
	
	private Button btnOk;
	private EditText askFor;
	private Spinner askAs;
	private String[] askValues;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_ask_edit);
		
		//Initialize variables and set listeners
		btnOk = (Button)findViewById(R.id.add_ask_ok_button);
		btnOk.setOnClickListener(this);
		askFor = (EditText)findViewById(R.id.ask_for_text);
		askAs = (Spinner)findViewById(R.id.ask_for_as);

		//Get array of textual question types
		askValues = getResources().getStringArray(R.array.ask_as_values);
		
		//Has a question been passed in the intent?
		if (getIntent().hasExtra(ASK_FOR)) {
			
			//Yes, get it..
			String s = getIntent().getExtras().getString(ASK_FOR);
			
			//Check if the question has a type
			if (s.startsWith("{?") && s.endsWith("}")) {
				
				//Yes it has, split it into type and text
				int p = s.indexOf('=');
				String t = s.substring(2, p); //Skip "{?"

				//Remove type from "s"
				s = s.substring(p+1, s.length() - 1); //Skip "{?..=" and "}"

				//Find the index of question type and adjust control accordingly
				for (int i = 0; i < askValues.length; i++) if (askValues[i].equalsIgnoreCase(t)) {
					askAs.setSelection(i);
					break;
				}
				
			}

			//Set question text
			askFor.setText(s);
			
		}
		
	}

	@Override
	public void onClick(View view) {
		
		if (view == btnOk) {
			
			//Get question from control
			String t = askFor.getText().toString().trim();
			
			//Validate the question
			if (t.length() == 0) {
				
				//Empty
				askFor.requestFocus();
				Toast.makeText(this, getString(R.string.ask_for_err_missing), Toast.LENGTH_SHORT).show();
				return;
				
			} else if (t.indexOf("}") >= 0) {
				
				//Invalid
				askFor.requestFocus();
				Toast.makeText(this, getString(R.string.ask_for_err_invalid), Toast.LENGTH_SHORT).show();
				return;
				
			}
			
			//All great, set result and finish
			Intent i = new Intent();
			i.putExtra(ASK_VALUE, "{?" + askValues[askAs.getSelectedItemPosition()] + "=" + t + "}");
			setResult(RESULT_OK, i);
			finish();
			
		}
		
	}

}
