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
import android.text.Html;
import android.widget.TextView;
import android.app.Activity;

import java.io.File;

//Used to show the help_html string

public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		//Replace $EXP$ in the help with the export file
		String help = getString(R.string.help_html);
		File ef = AboutActivity.getExportFile(this, true);
		help = help.replace("$EXP$", ef == null ? getString(R.string.unknown) : ef.toString());

		//Set to view
		((TextView)findViewById(R.id.help_view)).setText(Html.fromHtml(help));

	}

}
