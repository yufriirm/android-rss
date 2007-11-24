/*
 * $Id$
 */

package org.devtcg.rssreader;

import android.content.Context;
import android.content.Intent;
import android.content.IntentReceiver;

public class RSSReaderService_Alarm extends IntentReceiver
{
	@Override
	public void onReceiveIntent(Context context, Intent intent)
	{
		context.startService(new Intent(context, RSSReaderService.class), null);
	}
}
