/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class RSSChannelAdd extends Activity
{
	public EditText mTitleText;
	public EditText mURLText;
	
	/* We need this to not block when accessing the RSS feed for validation
	 * and for name downloads. */
	protected ProgressDialog mBusy;	
	final Handler mHandler = new Handler();
	
	/* Callback for network response from the "Download Name" button. */
	public String mFeedName;
	public String mFeedNameError;
	private Runnable mSetFeedName = new Runnable()
	{
		public void run()
		{
			mBusy.dismiss();
			
			if (mFeedNameError != null)
			{
				mTitleText.setText("");
				mTitleText.setHint(mFeedNameError);
			}
			else
			{
				mTitleText.setText(mFeedName);
			}
		}
	};
	
	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.channel_add);
		
		mTitleText = (EditText)findViewById(R.id.name);
		mURLText = (EditText)findViewById(R.id.url);
		
		Button download = (Button)findViewById(R.id.download);
		download.setOnClickListener(mDownloadListener);
		
		Button add = (Button)findViewById(R.id.add);
		add.setOnClickListener(mAddListener);
	}
	
	private OnClickListener mAddListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			ContentValues values = new ContentValues();
			values.put(RSSReader.Channels.TITLE, mTitleText.getText().toString());
			values.put(RSSReader.Channels.URL, mURLText.getText().toString());
			
			ContentURI uri = getContentResolver().insert(getIntent().getData(), values);
			setResult(RESULT_OK, uri.toString());
			
			finish();
		}
	};
	
	private OnClickListener mDownloadListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			mFeedName = null;
			mFeedNameError = null;
			
			mBusy = ProgressDialog.show(RSSChannelAdd.this,
				"Downloading", "Accessing RSS feed...", true, false);
			
			Thread t = new Thread()
			{
				public void run()
				{
					String urlStr = mURLText.getText().toString();
					
					try
					{
						mFeedName = getTitleFromFeed(new URL(urlStr));
					}
					catch(Exception e)
					{
						/* TODO: This could be much better... */
						mFeedNameError = e.getMessage().replace(urlStr, "");
					}
					finally
					{
						mHandler.post(mSetFeedName);
					};
				}
			};
			
			t.start();
		}
		
	};
	
	/* TODO: Improve this crappy function. */
	protected static String getTitleFromFeed(URL url)
		throws IOException
	{
		String title = null;
		String firstLine;
		
		/* TODO: We might need to set a more sensible timeout than the default. */
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		
		while (true)
		{
			firstLine = in.readLine();
			
			if (firstLine == null)
				break;
			
			if (firstLine.indexOf("<title") >= 0)
			{
				title = firstLine.trim();
				break;
			}
		}
		
		if (title == null)
			return null;
		
	    String regEx = "<title[^>]*>([^<]*)</title>";
	    Pattern pattern = Pattern.compile(regEx);
	    Matcher match = pattern.matcher(title);

	    if (match.find())
	      title = match.group(1);
	    
	    return title;
	}
}
