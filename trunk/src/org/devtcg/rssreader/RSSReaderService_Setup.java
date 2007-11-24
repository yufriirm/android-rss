/*
 * $Id$
 */

package org.devtcg.rssreader;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentReceiver;
import android.os.SystemClock;
import android.util.Log;

public class RSSReaderService_Setup extends IntentReceiver
{
	public static final String TAG = "RSSReaderService_Setup";

	@Override
	public void onReceiveIntent(Context context, Intent intent)
	{
		Log.d(TAG, "onReceiveIntent");		
		setupAlarm(context);
	}
	
	public static void setupAlarm(Context context)
	{
		Log.d(TAG, "setupAlarm");
		
		/* Start our service via the IntentFilter dummy class. */
		Intent start = new Intent(context, RSSReaderService_Alarm.class);
		
		/* Every hour. */
		long interval = 50 * 1000;

		AlarmManager amStart = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		amStart.setRepeating(AlarmManager.ELAPSED_REALTIME,
		  SystemClock.elapsedRealtime() + interval, interval, start);
	}
}
