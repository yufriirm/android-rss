/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RSSChannelEdit extends Activity
{
	private TextView mURLText;
	private TextView mTitleText;
	
	private ContentURI mURI;
	
	private Cursor mCursor;
	
	private static final String[] PROJECTION = {
	  RSSReader.Channels._ID,
	  RSSReader.Channels.URL, RSSReader.Channels.TITLE,
	  RSSReader.Channels.ICON };
	
	private static final int URL_INDEX = 1;
	private static final int TITLE_INDEX = 2;
	private static final int ICON_INDEX = 3;
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
				
		mURI = getIntent().getData();
		mCursor = managedQuery(mURI, PROJECTION, null, null);

		setContentView(R.layout.channel_edit);
		
		mURLText = (TextView)findViewById(R.id.channelEditURL);
		mTitleText = (TextView)findViewById(R.id.channelEditName);
		
		Button save = (Button)findViewById(R.id.channelEditSave);
		save.setOnClickListener(mSaveListener);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (mCursor == null)
			return;

		mCursor.first();
		
		mURLText.setText(mCursor, URL_INDEX);
		mTitleText.setText(mCursor, TITLE_INDEX);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();

		if (mCursor == null)
			return;

		updateProvider();
		managedCommitUpdates(mCursor);
	}

	private void updateProvider()
	{
		if (mCursor == null)
			return;

		mCursor.updateString(URL_INDEX, mURLText.getText().toString());
		mCursor.updateString(TITLE_INDEX, mTitleText.getText().toString());
	}

	private OnClickListener mSaveListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			updateProvider();
			mCursor.commitUpdates();
			
			setResult(RESULT_OK, mURI.toString());
			finish();
		}
	};
}
