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
import android.util.Log;
import android.widget.Toast;

import java.io.File;

//Convenient static features...

public class Global {

    //Global tag for logging
    public static final String LOG_TAG = "SMSIntent";

    //Static methods for restricting logging to debug build only
    public static void LogI(String msg) { if (BuildConfig.DEBUG) Log.i(LOG_TAG, msg); }
    public static void LogE(String msg) { if (BuildConfig.DEBUG) Log.e(LOG_TAG, msg); }

    //Quick toasting
    public static void ToastS(Context context, String msg) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); }
    public static void ToastS(Context context, int sid) { ToastS(context, context.getString(sid)); }
    public static void ToastL(Context context, String msg) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show(); }
    public static void ToastL(Context context, int sid) { ToastL(context, context.getString(sid)); }

}
