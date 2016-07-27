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

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//Basic adapter that inflates /res/layout/list_item.xml to
//items in MainActivity

public class DbAdapter extends CursorAdapter {

	//CTor - nothing fancy here
	public DbAdapter(Context context, Cursor cursor) {
		super(context, cursor, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		//Get the view holder
		ViewCtrls vc = (ViewCtrls)view.getTag();

		//Set the views id to the intents id
		view.setId(cursor.getInt(cursor.getColumnIndex(DbOpenHelper.ID)));

		//Set view values
		vc.title.setText(cursor.getString(cursor.getColumnIndex(DbOpenHelper.TITLE)));
		vc.address.setText(cursor.getString(cursor.getColumnIndex(DbOpenHelper.ADDRESS)));
		vc.isGroup = cursor.getInt(cursor.getColumnIndex(DbOpenHelper.GROUP)) > 0;

		//Set the icon for the item
		if (vc.isGroup) {

			//Set icon for groups and let snippet tell how many
			//intents the group holds

			vc.image.setImageResource(R.drawable.group);
			//vc.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
			int cnt = cursor.getInt(cursor.getColumnIndex(DbOpenHelper.COUNT));
			vc.snippet.setText(String.format(context.getString(R.string.group_of), cnt));

		}
		else {

			//Set the snippet to hold the value of the intent
			vc.snippet.setText(cursor.getString(cursor.getColumnIndex(DbOpenHelper.VALUE)));
			//vc.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

			//Get intent type and set the icon accordingly
			int type = cursor.getInt(cursor.getColumnIndex(DbOpenHelper.TYPE));

			switch (type) {
				case (DbOpenHelper.TYPE_SIMPLE): vc.image.setImageResource(R.drawable.simple); break;
				case (DbOpenHelper.TYPE_QUERY): vc.image.setImageResource(R.drawable.query); break;
				case (DbOpenHelper.TYPE_RECIPIENT): vc.image.setImageResource(R.drawable.recipient); break;
				case (DbOpenHelper.TYPE_RECIPIENT_QUERY): vc.image.setImageResource(R.drawable.recip_qry); break;
				default: vc.image.setImageResource(R.drawable.simple); break;
			}

		}

		//view.forceLayout();

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		//Create new view, inflate first
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		View res = inflater.inflate(R.layout.list_item, parent, false);

		//Create a view holder for convenience
		ViewCtrls vc = new ViewCtrls();

		//Set the views in the holder
		vc.title = (TextView)res.findViewById(R.id.txt_title);
		vc.address = (TextView)res.findViewById(R.id.txt_addr);
		vc.snippet = (TextView)res.findViewById(R.id.txt_snippet);
		vc.image = (ImageView)res.findViewById(R.id.img_view);

		//Set the holder as the results tag
		res.setTag(vc);

		//Return the constructed view
		return res;

	}
	
	//Basic class that does nothing else than hold the
	//views used in a list item
	public static class ViewCtrls {
		TextView title, address, snippet;
		ImageView image;
		boolean isGroup;
	}

}
