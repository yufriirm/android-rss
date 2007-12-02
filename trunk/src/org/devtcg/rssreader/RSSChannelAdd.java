/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class RSSChannelAdd extends Activity
{
	public EditText mURLText;

	/* We need this to not block when accessing the RSS feed for validation
	 * and for name downloads. */
	protected ProgressDialog mBusy;	
	final Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.channel_add);
		
		mURLText = (EditText)findViewById(R.id.url);
		
		Button add = (Button)findViewById(R.id.add);
		add.setOnClickListener(mAddListener);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		
		/* Position cursor at the end of the widget; seems easier to use
		 * that way. */
		Bundle state = mURLText.saveState();

		Integer endpos = mURLText.length();
		state.putInteger("sel-start", endpos);
		state.putInteger("sel-end", endpos);
		
		mURLText.restoreState(state);
	}

	private OnClickListener mAddListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			final String rssurl = mURLText.getText().toString();

			mBusy = ProgressDialog.show(RSSChannelAdd.this,
			  "Downloading", "Accessing XML feed...", true, false);

			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						final long id = (new RSSChannelRefresh(getContentResolver())).
						  syncDB(null, -1, rssurl);
						
				    	mHandler.post(new Runnable() {
				    		public void run()
				    		{
				    			mBusy.dismiss();

				    			ContentURI uri = RSSReader.Channels.CONTENT_URI.addId(id);
				    			setResult(RESULT_OK, uri.toString());
				    			finish();
				    		}
				    	});
					}
					catch(Exception e)
					{
						final String errmsg = e.getMessage();
						final String errmsgFull = e.toString();

			    		mHandler.post(new Runnable() {
			    			public void run()
			    			{
			    				mBusy.dismiss();

			    				String errstr = ((errmsgFull != null) ? errmsgFull : errmsg);

			    				AlertDialog.show(RSSChannelAdd.this,
			    				  "Feed error", "An error was encountered while accessing the feed: " + errstr,
			    				  "OK", true);
			    			}
			    		});
					}			    	
				}
			};

			t.start();
		}
	};
}
