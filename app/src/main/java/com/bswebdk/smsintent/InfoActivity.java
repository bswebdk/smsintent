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

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

//Used to show information about an intent (long tap in list -> Information)

public class InfoActivity extends Activity implements OnClickListener {

	//Used to define the info passed in the intent
	public static final String INFO = "inf";
	
	private Calendar cal = Calendar.getInstance();

	private void setDateField(int fieldId, String time) {

		//Set the view with id "fieldId" to a formatted representation
		//of "time" (which is a db-compatible long)

		long t = Long.parseLong(time);

		if (t == 0) ((TextView)findViewById(fieldId)).setText("-"); //Not available

		else {

			//Set calendar time
			cal.setTimeInMillis(Long.parseLong(time));

			//Format the time
			String s = DateFormat.getDateFormat(this).format(cal.getTime()) + " " + DateFormat.getTimeFormat(this).format(cal.getTime());

			//Set to field
			((TextView)findViewById(fieldId)).setText(s);

		}

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		
		//Get info from intent and split into values
		String info = getIntent().getExtras().getString(INFO);
		String[] iv = info.split(",");
		
		//Set timestamps
		setDateField(R.id.info_created, iv[0]);
		setDateField(R.id.info_modified, iv[1]);
		setDateField(R.id.info_lastuse, iv[2]);
		
		//Set number of usages (hit counter)
		((TextView)findViewById(R.id.info_usages)).setText(iv[3]);
		
		//Set listener to close button
		((Button)findViewById(R.id.info_close)).setOnClickListener(this);
		
	}

	@Override
	public void onClick(View arg0) {

		//Set result and finish
		setResult(arg0.getId());
		finish();

	}

}
