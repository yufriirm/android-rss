/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class RSSChannelAdd extends Activity
{
	public EditText mTitleText;
	public EditText mURLText;
	public ImageView mIcon;
	
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
	
	public Bitmap mIconData;
	private Runnable mSetIcon = new Runnable()
	{
		public void run()
		{
			mBusy.dismiss();
			
			if (mIconData != null)
				mIcon.setImageBitmap(mIconData);
			else
				mIcon.setImageResource(R.drawable.feedicon);
		}
	};
	
	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.channel_add);
		
		mTitleText = (EditText)findViewById(R.id.name);
		mURLText = (EditText)findViewById(R.id.url);
		mIcon = (ImageView)findViewById(R.id.icon);
		
		mIcon.setImageResource(R.drawable.feedicon);
		
		Button downloadIcon = (Button)findViewById(R.id.download_icon);
		downloadIcon.setOnClickListener(mDownloadIconListener);
		
		Button downloadName = (Button)findViewById(R.id.download_name);
		downloadName.setOnClickListener(mDownloadNameListener);
		
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

			if (mIconData != null)
				values.put(RSSReader.Channels.ICON, mIconData.toString());

			ContentURI uri = getContentResolver().insert(getIntent().getData(), values);
			
			setResult(RESULT_OK, uri.toString());
			finish();
		}
	};
	
	private OnClickListener mDownloadIconListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			mIconData = null;
			
			mBusy = ProgressDialog.show(RSSChannelAdd.this,
			  "Downloading", "Accessing feed icon...", true, false);
			
			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						URL fullUrl = new URL(mURLText.getText().toString());
						URL iconUrl = 
						  new URL(fullUrl.getProtocol(), 
						    fullUrl.getHost(), fullUrl.getPort(),
						    "/favicon.ico");
						
						Log.d("RSSChannelAdd", "Fetching: " + iconUrl.toString() + "...");
						
						/* TODO: Harrass Google to fix this.  It just
						 * won't work and there's no good reason why not. */
						mIconData = BitmapFactory.decodeStream(iconUrl.openStream());
						
						if (mIconData == null)
							Log.d("RSSChannelAdd", "TODO: Harrass Google about this bug!");
						else
							Log.d("RSSChannelAdd", "Sweet, bug fixed!");
					}
					catch (Exception e)
					{						
						/* TODO: Do something... */
						Log.d("RSSChannelAdd", "Bollocks", e);
					}
					finally
					{
						mHandler.post(mSetIcon);
					}
				}
			};
			
			t.start();
		}
	};
	
	private OnClickListener mDownloadNameListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			mFeedName = null;
			mFeedNameError = null;
			
			mBusy = ProgressDialog.show(RSSChannelAdd.this,
			  "Downloading", "Accessing feed XML...", true, false);
			
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
