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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

//Activity used to display an activity that asks the user for values
//used in the intent that has been selected by the user

public class AskActivity extends Activity implements OnClickListener, OnGlobalLayoutListener {

	//Constants that reflect the items in the "ask_as_values" array
	public static final int ASK_AS_TEXT = 0;
	public static final int ASK_AS_DATE = 1;
	public static final int ASK_AS_TIME = 2;
	
	//Base view id for controls used for asking
	public static final int VIEW_ID_BASE = 1000;
	
	//Used to identify the sms body in the intent that invokes this activity
	public static final String INQUERY = "inquery";

	private static void smsAppendString(ArrayList<String> to, String s) {
		//Prevent multiple consecutive string chunks by checking
		//the last item in "to" - if the last item is a string,
		//append "s" to last item, else add "s" to "to"
		int as = to.size();
		if (as > 0) {
			String t = to.get(as-1);
			if (t.charAt(0) == 's') {
				to.set(as-1, t + s);
				return;
			}
		}
		to.add('s' + s);
	}
	
	public static String[] parseSmsQuestions(Context context, String sms) {
		
		//Parse "sms" and compile it to an array of questions and strings

		//Get array of question types
		String[] ask_vals = context.getResources().getStringArray(R.array.ask_as_values);

		//Temporary list for chunks
		ArrayList<String> temp = new ArrayList<String>();
		
		while (sms.length() > 0) {
			

			//Find the beginning of next question
			int p1 = sms.indexOf("{?");

			//Find the end of next question - if possible
			int p2 = (p1 < 0) ? p1 : sms.indexOf('}', p1);
			
			if (p2 <= p1) {
				
				//No question found, append the rest as a string
				smsAppendString(temp, sms);

				//Set sms to empty to stop iteration
				sms = "";
				
			} else {
				
				//A question was found, validate it
				boolean valid = false;
				
				String before = sms.substring(0, p1);
				String askfor = sms.substring(p1+2, p2);
				String after = sms.substring(p2+1);
				
				//Log for debugging
				Global.LogI("before=\"" + before + "\"");
				Global.LogI("askfor=\"" + askfor + "\"");
				Global.LogI("after=\"" + after + "\"");
				
				//Append text before the question as a string
				smsAppendString(temp, before);
				
				//Find question separator
				int sp = askfor.indexOf('=');
				if (sp > 0) {
					
					//Separator found, split into type and text
					String as = askfor.substring(0, sp); //Question type
					askfor = askfor.substring(sp+1); //Question text

					//Convert the textual type into an integer type
					int askAs = -1;
					for (int i = 0; i < ask_vals.length; i++) if (ask_vals[i].equalsIgnoreCase(as)) {
						askAs = i;
						break;
					}
					
					if (askAs != -1) {
						
						//The question is valid, add it to the temp list
						temp.add("q" + askAs + "," + askfor);

						//Set valid
						valid = true;
						
					}
					
				}
				
				//If the question is not valid, add it as a string
				if (!valid)	smsAppendString(temp, "{?" + askfor + "}");

				//Set "sms" to whatever is left to compile
				sms = after;
				
			}
			
		}
		
		//Return the temporary list as an array of strings
		String[] res = new String[temp.size()];
		return temp.toArray(res);
		
	}

	private LinearLayout list;
	private Button okBtn;
	private String[] inQuery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_ask);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);

		//Get views and assign listeners
		list = (LinearLayout)findViewById(R.id.ask_layout);
		okBtn = (Button)findViewById(R.id.ask_ok);
		okBtn.setOnClickListener(this);
		
		//Default layout params for dynamically added views
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

		//Get values from the intent (generated with parseSmsQuestion)
		inQuery = getIntent().getStringArrayExtra(INQUERY);
		
		//Use 24h time format or AM/PM?
		boolean is24h = android.text.format.DateFormat.is24HourFormat(this);
		
		for (int i = 0; i < inQuery.length; i++) {
			
			//Get current item
			String s = inQuery[i];
			
			//Log for debugging
			Global.LogI("INQUERY=" + s);
			
			//Check if "s" holds a question
			if (s.charAt(0) == 'q') {
				
				//Yep, convert to question type and text
				int p = s.indexOf(',');
				int as = Integer.parseInt(s.substring(1, p));
				s = s.substring(p+1);
				
				//View to add
				View v = null;
				
				//Make the correct view
				switch (as) {


					case (ASK_AS_TEXT): //Plain text
						v = new EditText(this);
						break;

					case (ASK_AS_DATE): //Date
						DatePicker dp = new DatePicker(this);
						dp.setCalendarViewShown(false);
						v = dp;
						break;

					case (ASK_AS_TIME): //Time
						TimePicker tp = new TimePicker(this);
						tp.setIs24HourView(is24h);
						v = tp;
						break;

				}
				
				//Did we got a view?
				if (v != null) {
					
					//Yep, create a label for it
					TextView label = new TextView(this);
					label.setLayoutParams(lp);
					label.setText(s + ":");
					//label.setLabelFor(1000 + i);

					//Set layout params and id of the view
					v.setId(VIEW_ID_BASE + i);
					v.setLayoutParams(lp);

					//Add the label and the view
					list.addView(label);
					list.addView(v);
					
				}
				
			}
			
		}
		
	}

	@Override
	public void onClick(View view) {

		if (view == okBtn) {
			
			//Ok button clicked, construct the SMS
			StringBuilder sb = new StringBuilder();
			Calendar cal = Calendar.getInstance();
			
			for (int i = 0; i < inQuery.length; i++) {
				
				//Get the current chunk and the chunk type
				String s = inQuery[i];
				char c = s.charAt(0);
				
				//Append string
				if (c == 's') sb.append(s.substring(1));
				
				else {
					
					//Find the view of the question
					View v = findViewById(VIEW_ID_BASE + i);
					
					//Append text
					if (v instanceof EditText) sb.append(((EditText)v).getText().toString().trim());
					
					else if (v instanceof DatePicker) {
						
						//Append the date picked
						DatePicker dp = (DatePicker)v;
						cal.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
						sb.append(DateFormat.getDateInstance().format(cal.getTime()));
						
					}
					
					else if (v instanceof TimePicker) {
						
						//Append the time picked
						TimePicker tp = (TimePicker)v;
						cal.set(1, 1, 1970, tp.getCurrentHour(), tp.getCurrentMinute(), 0);
						String t = DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.getTime());
						if (t.charAt(0) == '0') t = t.substring(1);
						sb.append(t);
						
					}
					
				}
				
			}
			
			//Set the result and finish the activity
			Intent intent = new Intent();
			intent.putExtra(INQUERY, sb.toString());
			setResult(RESULT_OK, intent);
			finish();
			
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//Touch outside the dialog = cancel
		if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
			setResult(0);
			finish();
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onGlobalLayout() {
		//Resize the dialog to match its content
		ViewParent vp = list.getParent();
		if (vp instanceof ScrollView) {
			ScrollView sv = (ScrollView)vp;
			int h = list.getMeasuredHeight() + sv.getPaddingBottom() + sv.getPaddingTop();
			if (sv.getMeasuredHeight() > h) {
				View dv = getWindow().getDecorView();
				getWindow().setLayout(dv.getMeasuredWidth(), dv.getMeasuredHeight() - (sv.getMeasuredHeight() - h));
			}
		}
	}

}
