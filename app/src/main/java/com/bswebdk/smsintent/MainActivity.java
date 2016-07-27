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
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

//The main activity that launches when the app is started. It holds
//a list of intents and groups that can be selected. It also holds
//features to create new intents and groups and so on...

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

	//Static instance used by AboutActivity to refresh
	//the list after an import
	public static MainActivity instance = null;

	//Constants used to define actions in the lists context menu (per item)
	private final int ACTION_EDIT = 123;
	private final int ACTION_DELETE = 124;
	private final int ACTION_INFO = 125;
	
	//Constants used to identify activity results
	private final int REQUEST_EDIT = 567;
	private final int REQUEST_ASK = 568;
	//private final int REQUEST_ABOUT = 569;
	
	//Inden of the add group item in the menu
	private final int ADD_GROUP_ITEM_INDEX = 1;
	
	private DbOpenHelper helper;
	private DbAdapter adapter;
	private ListView list;
	private View ctxtItem;
	private String address, grpAddr;
	private int inGroup, itemId;
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		//An item in the context menu has been selected
		switch (item.getItemId()) {

			case (ACTION_INFO): //Show item info
				info(ctxtItem.getId());
				break;

			case (ACTION_EDIT): //Edit item
				edit(ctxtItem.getId(), ((DbAdapter.ViewCtrls)ctxtItem.getTag()).isGroup);
				break;

			case (ACTION_DELETE): //Delete item
				if (helper.deleteRow(ctxtItem.getId()) > 0) refreshList();
				break;
			
		}

		return true;

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		if (v.getId() == R.id.list_view) {

			//Create context menu for the list view

			AdapterView.AdapterContextMenuInfo minfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
			ctxtItem = list.getChildAt(minfo.position);
			DbAdapter.ViewCtrls ctrls = (DbAdapter.ViewCtrls)ctxtItem.getTag();
			menu.setHeaderTitle(ctrls.title.getText());
			menu.add(0, ACTION_INFO, 0, getString(R.string.information));
			menu.add(0, ACTION_EDIT, 1, getString(R.string.edit));
			menu.add(0, ACTION_DELETE, 2, getString(R.string.delete));
			
		} else super.onCreateContextMenu(menu, v, menuInfo);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		//Handle results from activities

		if ((requestCode == REQUEST_EDIT) && (resultCode == EditActivity.RESULT_OK)) {
			
			if (data.hasExtra(EditActivity.EDIT_ID)) {
				
				//An item has been edited, try to update only that single item
				//and if that is not possible, refresh the entire list

				int editId = data.getExtras().getInt(EditActivity.EDIT_ID);
				View v = list.findViewById(editId);
				
				if (v != null) {
					
					Cursor c = helper.openEditCursor(editId);
					if (c.moveToFirst()) adapter.bindView(v, this, c);
					c.close();
					
				} else refreshList();
				
			} else {
				
				//A new item has been added, refresh the list
				refreshList();
				
			}
			
		} else if ((requestCode == REQUEST_ASK) && (resultCode == AskActivity.RESULT_OK)) {
			
			//The AskActivity has returned successfull, launch the intent
			smsIntent(data.getExtras().getString(AskActivity.INQUERY));
			
		} /*else if ((requestCode == REQUEST_ABOUT) && (resultCode != 0)) {

			//Deprecated functionality?
			refreshList();

		}*/

		//adapter.notifyDataSetChanged();
		super.onActivityResult(requestCode, resultCode, data);
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		MainActivity.instance = this; //Set the static instance
		setContentView(R.layout.activity_main);

		//Initialize variables and set listeners
		inGroup = 0;
		grpAddr = "";
		address = "";
		adapter = new DbAdapter(this, null);
		helper = new DbOpenHelper(this);
		list = (ListView)findViewById(R.id.list_view);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		list.setEmptyView(findViewById(android.R.id.empty));

		//Register a context menu
		registerForContextMenu(list);

		//Init a loader
		getLoaderManager().initLoader(0, null, this);

	}

	@Override
	protected void onDestroy() {
		//Nullify the static instance
		MainActivity.instance = null;
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		//Make sure that the add group item is not available when in a group
		menu.getItem(ADD_GROUP_ITEM_INDEX).setVisible(inGroup == 0);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		//An item in the options menu has been selected
		switch (item.getItemId()) {
		
			case (R.id.action_add): //Add new intent
				edit(-1, false);
				return true;
			
			case (R.id.action_add_grp): //Add new group
				if (inGroup == 0) edit(-1, true);
				return true;
			
			/*case (R.id.action_settings):
				Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
				return true;*/
		
			case (R.id.action_about): //Show AboutActivity
				startActivity(new Intent(this, AboutActivity.class));
				return true;
		
			case (R.id.action_help): //Show HelpActivity
				startActivity(new Intent(this, HelpActivity.class));
				return true;
		
		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		//Create a cursor loader for the list
		CursorLoader cl = new CursorLoader(this) {
			public Cursor loadInBackground() {
	            return helper.openListCursor(0, inGroup); 
	        }			
		};
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		//Swap the cursor and invalidate the options menu
		adapter.swapCursor(cursor);
		invalidateOptionsMenu();
	}

	@Override

	public void onLoaderReset(Loader<Cursor> loader) {
		//Null swap the cursor
		adapter.swapCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> list, View item, int position, long rowId) {
		
		//An item has been clicked in the list, get its id
		itemId = item.getId();

		//And open a cursor for the item
		Cursor c = helper.openEditCursor(itemId);
		
		try {
			
			if (c.moveToFirst()) {
				
				if (c.getInt(c.getColumnIndex(DbOpenHelper.GROUP)) > 0) {

					//If the item is a group, we must set the group id
					//and refresh the list to load the items in that group

					inGroup = itemId;
					grpAddr = c.getString(c.getColumnIndex(DbOpenHelper.ADDRESS));
					refreshList();
					return;

				}
				
				//If not a group, we must get more values for the intent
				String s = c.getString(c.getColumnIndex(DbOpenHelper.VALUE));
				address = c.getString(c.getColumnIndex(DbOpenHelper.ADDRESS));

				//Parse the questions in the value
				String[] sq = AskActivity.parseSmsQuestions(this, s);

				//If only one chunk is available, launch the intent
				if (sq.length == 1) smsIntent(s);

				//Else launch the AskActivity to handle the questions in the value
				else {
					Intent intent = new Intent(this, AskActivity.class);
					intent.putExtra(AskActivity.INQUERY, sq);
					startActivityForResult(intent, REQUEST_ASK);
				}
				
			}
			
		} finally {
			
			c.close();
			
		}
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if ((keyCode == KeyEvent.KEYCODE_BACK) && (inGroup > 0)) {

			//If the back button is pressed whilst in a group, we must
			//exit that group by nullifying it and refresh the list

			inGroup = 0;
			grpAddr = "";
			refreshList();
			return true;

		}

		return super.onKeyDown(keyCode, event);

	}

	private void edit(int id, boolean isGroup) {

		//Create an intent for the EditActivity and
		//launch the editor for a result
		Intent i = new Intent(this, EditActivity.class);
		i.putExtra(EditActivity.IS_GROUP, isGroup);
		i.putExtra(EditActivity.EDIT_ID, id);
		i.putExtra(EditActivity.GROUP_ID, inGroup);
		i.putExtra(EditActivity.ADDRESS, grpAddr);
		startActivityForResult(i, REQUEST_EDIT);

	}
	
	private void info(int id) {

		//Show information about an intent
		Cursor c = helper.openEditCursor(id);

		try {

			if (c.moveToFirst()) {

				String s = "";
				s += c.getLong(c.getColumnIndex(DbOpenHelper.CREATED)) + ",";
				s += c.getLong(c.getColumnIndex(DbOpenHelper.MODIFIED)) + ",";
				s += c.getLong(c.getColumnIndex(DbOpenHelper.LAST_USED)) + ",";
				s += c.getInt(c.getColumnIndex(DbOpenHelper.HITS)) + ",";
				Intent i = new Intent(this, InfoActivity.class);
				i.putExtra(InfoActivity.INFO, s);
				startActivity(i);

			}

		} finally {

			c.close();

		}

	}
	
	public void refreshList() { refreshList(false); }
	public void refreshList(boolean root) {

		//Refresh the items in the list
		if (root) inGroup = 0;
		getLoaderManager().restartLoader(0, null, this);

	}

	public int itemCount() {

		//Return the number of items in the list
		return list.getCount();

	}
	
	private void smsIntent(String body) {
		
		//Launch an intent with the specified body.

		if (helper != null) {

			//Increment the hit counter for the intent
			if (!helper.addHitForRow(itemId, inGroup)) Global.LogE("Cannot update hits for " + itemId);

		}
		
		//Create the intent
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + address));
		if (address.length() > 0) intent.putExtra(Intent.EXTRA_PHONE_NUMBER, address);
	
		//Set body
		if ((body != null) && (body.length() > 0)) {
			intent.putExtra("sms_body", body);
			intent.putExtra(Intent.EXTRA_TEXT, body);
		}
		
		//Set task flag and launch SMS activity
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		
		//Finish!
		finish();
		
	}
		

}
