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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Telephony;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//Activity used to display information about the package / app
//and to add extra features like import / export and for testing
//different intent formats

public class AboutActivity extends Activity implements OnClickListener {

	//Header used for exports (and to verify imports)
	private final String EXPORT_HEADER = "SMSIntent.Export.Version=";

	//Export version
	private final int EXPORT_VERSION = 1;

	//Name of the file exports are stored to
	private static final String EXPORT_FILE = "intents.lst";

	//The line break used for exports
	private final String EXPORT_CRLF = "\r\n";

	//The encoding set used for export
	private final String EXPORT_ENCODING = "UTF-8";

	//Number of methods for creating intents
	private final int DEBUG_METHOD_COUNT = 6;

	//The number of taps to the package name required to show debug buttons
	private final int DEBUG_TAP_COUNT = 6;

	//The maximum delay for each debug tab
	private final int DEBUG_TAP_DELAY = 1000;


	private long lastClick = 0; //Time for last click (debugging only
	private int clickCount; //Number of consecutive clicks
	private boolean debugShowing = false; //Are we showing debug buttons?
	private long importCount = 0; //Number of intents imported
	private long exportCount = 0; //Number of intents exported
	private String version = null; //Package version (see getVersion)
	private String buildDate = null; //Package build date (see getBuildDate)
	private Button exportBtn = null; //Export button
	private Button importBtn = null; //Import button

	private String getBuildDate() {
		//Return the build date for the package
		if (buildDate == null) try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			try {
				ZipEntry ze = zf.getEntry("classes.dex");
				buildDate = DateFormat.getDateFormat(this).format(new Date(ze.getTime()));
			} finally {
				zf.close();
			}
		} catch (Exception e) {
		}
		return (buildDate == null) ? getString(R.string.unknown) : buildDate;
	}

	private String getVersion() {
		//Return the package version
		if (version == null) try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (Exception e) {
		}
		return (version == null) ? getString(R.string.unknown) : version;
	}

	private static boolean isStorageStateValid(String state, boolean forWrite) {

		//Validate storage state
		if (state == null) return false;
		boolean writeable = state.equals(Environment.MEDIA_MOUNTED);
		return forWrite ? writeable : (writeable || state.equals(Environment.MEDIA_MOUNTED_READ_ONLY));

	}

	@SuppressLint("NewApi")
	public static File getExportFile(Context context, boolean forWrite) {

		//Find the optimal location for storing exported intents
		//or (if not forWrite) a previously exported list if intents
		boolean NewAPI = Build.VERSION.SDK_INT >= 19;

		if (NewAPI) {

			//For newer API's we check all available locations
			File fs[] = context.getExternalFilesDirs(null);

			for (int i = fs.length - 1; i >= 0; i--) {

				File f = fs[i];

				if (isStorageStateValid(Environment.getStorageState(f), forWrite)) {

					//If file is supposed to be written, it must exist
					File ef = new File(f, EXPORT_FILE);
					if (forWrite || ((ef != null) && ef.exists())) return ef;

				}

			}

		}

		//On older API's we only check the default external storage
		if (isStorageStateValid(Environment.getExternalStorageState(), forWrite)) {

			File ef = new File(context.getExternalFilesDir(null), EXPORT_FILE);
			if (forWrite || ((ef != null) && ef.exists())) return ef;

		}

		//We have failed!
		return null;

	}

	@SuppressLint("NewApi")
	private void debugIntent(int id) {
		//Used to test how the intent should be formed in order to work properly
		Intent intent = null;
		switch (id - 56789) {
			case (0):
				intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
				break;
			case (1):
				intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"));
				break;
			case (2):
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"));
				break;
			case (3):
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
				break;
			case (4):
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					String smsPack = Telephony.Sms.getDefaultSmsPackage(this);
					intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					if (smsPack != null) intent.setPackage(smsPack);
				} else Toast.makeText(this, "Not KitKat", Toast.LENGTH_SHORT).show();
				break;
			case (5):
				intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.setType("vnd.android-dir/mms-sms");
				break;
		}
		if (intent != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("sms_body", "Test message");
			intent.putExtra(Intent.EXTRA_TEXT, "Test message");
			startActivity(intent);
		}
	}

	private void showDebugButtons() {
		//Display debug buttons if not already showing. Requires DEBUG_TAP_COUNT
		//taps to the package name to show
		if (!debugShowing) {
			RelativeLayout rl = (RelativeLayout) findViewById(R.id.about_rel_layout);
			RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp.addRule(RelativeLayout.BELOW, R.id.about_close);
			LinearLayout ll = new LinearLayout(this);
			ll.setOrientation(LinearLayout.HORIZONTAL);
			rl.addView(ll, rlp);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			llp.setMargins(0, 0, 0, 0);
			for (int i = 0; i < DEBUG_METHOD_COUNT; i++) {
				Button b = new Button(this, null, android.R.attr.buttonStyleSmall);
				b.setOnClickListener(this);
				b.setId(56789 + i);
				b.setText("M" + i);
				ll.addView(b, llp);
			}
			debugShowing = true;
		}
	}

	private String makeExportSafe(String s) {
		//Replace unsafe characters before export
		return s.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
	}

	private String unMakeExportSafe(String s) {
		//Replace unsafe characters after import
		return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
	}

	private void exportSingle(Cursor cursor, Writer writer, boolean hasParent) throws IOException {

		//Export a single intent / template from the cursor to the writer

		//If the intent has a parent the first character is a tab
		String s = hasParent ? "\t" : "";

		//Append the values of the intent, PARENT and COUNT are not exported
		//since they will be determined during import anyway
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.CREATED)) + "\t";
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.MODIFIED)) + "\t";
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.LAST_USED)) + "\t";
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.HITS)) + "\t";
		s += makeExportSafe(cursor.getString(cursor.getColumnIndex(DbOpenHelper.TITLE))) + "\t";
		s += makeExportSafe(cursor.getString(cursor.getColumnIndex(DbOpenHelper.ADDRESS))) + "\t";
		s += makeExportSafe(cursor.getString(cursor.getColumnIndex(DbOpenHelper.VALUE))) + "\t";
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.TYPE)) + "\t";
		s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.GROUP)) + "\t";

		//s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.PARENT)) + "\t";
		//s += cursor.getLong(cursor.getColumnIndex(DbOpenHelper.COUNT)) + "\t";

		s = s.substring(0, s.length() - 1) + EXPORT_CRLF;

		//Write da sh!t to writer
		writer.write(s);

		//Increment counter
		exportCount++;

	}

	private boolean exportIntents() {

		//Get output file
		File outFile = getExportFile(this, true);

		//If it already exists, delete it
		if (outFile.exists() && (!outFile.delete())) {

			//Delete failed
			Global.LogE("Could not delete export file");
			return false;

		}

		//Reset export counter
		exportCount = 0;

		try {

			//Open file for export
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), EXPORT_ENCODING));
			try {

				//Write header
				out.write(EXPORT_HEADER + EXPORT_VERSION + EXPORT_CRLF);

				//Make a database helper
				DbOpenHelper db = new DbOpenHelper(this);
				String order = DbOpenHelper.ID + " ASC";

				//Export groups
				Cursor groups = db.openCustomCursor(DbOpenHelper.GROUP + ">0", order);

				try {

					if (groups.moveToFirst()) do {

						//Export the current group
						exportSingle(groups, out, false);

						//Export the intents belonging to current group
						long id = groups.getLong(groups.getColumnIndex(DbOpenHelper.ID));
						Cursor content = db.openCustomCursor(DbOpenHelper.PARENT + "=" + groups.getLong(groups.getColumnIndex(DbOpenHelper.ID)), order);

						try {

							//Export the content of the group
							if (content.moveToFirst()) do {

								exportSingle(content, out, true);

							} while (content.moveToNext());

						} finally {

							content.close();

						}

					} while (groups.moveToNext());

				} finally {

					groups.close();

				}

				//Export non grouped (top level) intents
				Cursor nogroup = db.openCustomCursor(DbOpenHelper.GROUP + "=0 AND " + DbOpenHelper.PARENT + "=0", order);
				try {

					//Export the content of the group
					if (nogroup.moveToFirst()) do {

						exportSingle(nogroup, out, false);

					} while (nogroup.moveToNext());

				} finally {

					nogroup.close();

				}


			} finally {

				out.close();

				//If nothing was exported, delete the file
				if (exportCount == 0) outFile.delete();

				else {

					//Else make it available to other apps
					outFile.setReadable(true, false);
					outFile.setWritable(true, false);

				}

			}

		} catch (Exception err) {

			//Alert about the error
			Global.LogE(err.getMessage());
			return false;

		}

		//Success if anything was exported
		return exportCount > 0;

	}

	private boolean importIntents() {

		//Number of colums to import
		final int IMPORT_COLUMNS = 9;

		//Clear import counter
		importCount = 0;

		//Get import file and verify
		File inFile = getExportFile(this, false);
		if ((inFile == null) /*|| (!inFile.exists())*/) {

			//Import file does not exist
			Global.LogE("No import file");
			return false;

		}

		try {

			//Open file for export
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
			try {

				//Check header
				String h = in.readLine();
				if (!h.startsWith(EXPORT_HEADER)) {

					//Erroneous header
					Global.LogE("Invalid import header: " + h);
					return false;

				}

				//Check version
				int ver = Integer.parseInt(h.substring(EXPORT_HEADER.length(), h.length()));
				if (ver > EXPORT_VERSION) {

					//Unsupported import version
					Global.LogE("Invalid import version: " + ver);
					return false;

				}

				//Make a database helper
				DbOpenHelper db = new DbOpenHelper(this);

				//Handy variables
				boolean lineOk;
				long groupId = 0;

				do {

					//Get a line and verify
					String line = in.readLine();
					lineOk = (line != null) && (line.length() > 0);

					if (lineOk) {

						//Split line at tabs
						String[] tokens = line.split("[\\t]");

						//Verify that the token count is valid
						if (tokens.length < IMPORT_COLUMNS) {

							//Invalid number of tokens
							Global.LogE("Token count mismatch: " + line);
							return false;

						}

						//Offs is used to determine the offset of values in the
						//array and to determine if the intent belongs to a
						//previously added group
						int offs = (tokens[0].length() == 0) ? 1 : 0;

						//Convert tokens to values
						long created = Long.parseLong(tokens[offs + 0]);
						long modified = Long.parseLong(tokens[offs + 1]);
						long last_used = Long.parseLong(tokens[offs + 2]);
						long hits = Long.parseLong(tokens[offs + 3]);
						String title = unMakeExportSafe(tokens[offs + 4]);
						String address = unMakeExportSafe(tokens[offs + 5]);
						String value = unMakeExportSafe(tokens[offs + 6]);
						long type = Long.parseLong(tokens[offs + 7]);
						long group = Long.parseLong(tokens[offs + 8]);

						//Insert the values to the database
						long newId = db.insertRawRow(
								created, modified, last_used, hits, title, address,
								value, type, group, offs == 1 ? groupId : 0);

						//Increment import counter
						importCount++;

						//If the values inserted was a group, set the groupId
						if (group > 0) groupId = newId;

					} else Global.LogE("Invalid import line: " + line);

				} while (lineOk);


			} finally {

				in.close();

			}

		} catch (Exception err) {

			//Log the error
			Global.LogE(err.getMessage());
			return false;

		}

		//Success at this point
		return importCount > 0;

	}

	private void updateButtons() {

		//Enable export button if it is possible to write to the export file
		//and if anything to export is present
		File ef = getExportFile(this, true);
		exportBtn.setEnabled((ef != null) && (MainActivity.instance.itemCount() > 0));

		//If import has already been performed, disable import button to prevent
		//unintentional double imports
		if (importCount == 0) {

			//Enable the import button if the export file exists and can be read
			if (ef == null) ef = getExportFile(this, false);
			importBtn.setEnabled((ef != null) && ef.exists());

			Global.LogI("exportFileExists=" + ((ef != null) && ef.exists() ? "yes" : "no"));

		} else importBtn.setEnabled(false);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		//Set values in the about box and assign click listeners
		TextView tv = (TextView) findViewById(R.id.show_app_package);
		tv.setText(getPackageName());
		tv.setOnClickListener(this);

		((TextView) findViewById(R.id.show_app_copyright)).setText("Torben Bruchhaus");
		((TextView) findViewById(R.id.show_app_version)).setText(getVersion());
		((TextView) findViewById(R.id.show_app_date)).setText(getBuildDate());

		((Button) findViewById(R.id.about_close)).setOnClickListener(this);

		importBtn = (Button) findViewById(R.id.about_import);
		importBtn.setOnClickListener(this);

		exportBtn = (Button) findViewById(R.id.about_export);
		exportBtn.setOnClickListener(this);

		//Update button states
		updateButtons();

	}

	@Override
	public void onClick(View view) {

		//Click handler

		if (view.getId() == R.id.show_app_package) {

			//If the package name is clicked DEBUG_TAP_COUNT times
			//with max. DEBUG_TAP_DELAY between each click, we must
			//display the debug buttons

			long time = System.currentTimeMillis();
			if (time - lastClick < DEBUG_TAP_DELAY) {
				clickCount++;
				if (clickCount >= DEBUG_TAP_COUNT) showDebugButtons();
			} else {
				clickCount = 1;
			}
			lastClick = time;
			return;

		} else if (view.getId() == R.id.about_close) {

			//Set result (not used anymore) and finish
			//setResult(view.getId());
			finish();

		} else if (view.getId() == R.id.about_import) {

			//Import intents and show result
			Global.ToastS(this, importIntents() ? R.string.import_success : R.string.import_failed);

			//Update buttons
			updateButtons();

			//If anything was imported, main activity must be refreshed
			if (importCount > 0) MainActivity.instance.refreshList(true);


		} else if (view.getId() == R.id.about_export) {

			//Export intents and show result
			Global.ToastS(this, exportIntents() ? R.string.export_success : R.string.export_failed);

			//Update buttons
			updateButtons();

		} else if (debugShowing) {

			//If debug buttons are showing, change color and test the clicked intent
			view.setBackgroundColor(Color.CYAN);
			debugIntent(view.getId());

		}

	}

}
