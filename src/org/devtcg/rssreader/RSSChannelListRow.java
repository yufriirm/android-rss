/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RSSChannelListRow extends RelativeLayout
{
	private ImageView mIcon;
	private TextView mName;
	private TextView mCount;
	private ProgressBar mRefresh;

	private static final int CHANNEL_NAME = 1;
	private static final int CHANNEL_RIGHT = 2;
	private static final int CHANNEL_ICON = 3;

	public RSSChannelListRow(Context context)
	{
		super(context);

		setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		mIcon = new ImageView(context);
		mIcon.setPadding(0, 2, 3, 2);
		mIcon.setId(CHANNEL_ICON);

		RelativeLayout.LayoutParams iconRules =
		  new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		iconRules.addRule(ALIGN_WITH_PARENT_LEFT);
		addView(mIcon, iconRules);
		
		LinearLayout rightSide = new LinearLayout(context);
		rightSide.setId(CHANNEL_RIGHT);
		
		/* <right> */
		mRefresh = new ProgressBar(context);
		mRefresh.setIndeterminate(true);
		mRefresh.setVisibility(GONE);
		
		LinearLayout.LayoutParams refreshRules = new LinearLayout.LayoutParams(18, 18);
		refreshRules.gravity = Gravity.CENTER_VERTICAL;
		rightSide.addView(mRefresh, refreshRules);

		mCount = new TextView(context);
		mCount.setGravity(Gravity.CENTER_VERTICAL);
		rightSide.addView(mCount, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		/* </right> */
		
		RelativeLayout.LayoutParams rightRules =
		  new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		rightRules.addRule(ALIGN_WITH_PARENT_RIGHT);
		
		addView(rightSide, rightRules);
		
		mName = new TextView(context);
		mName.setPadding(3, 0, 0, 0);
		mName.setId(CHANNEL_NAME);

		RelativeLayout.LayoutParams nameRules =
		  new RelativeLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);

		nameRules.addRule(POSITION_TO_LEFT, CHANNEL_RIGHT);
		nameRules.addRule(POSITION_TO_RIGHT, CHANNEL_ICON);
		addView(mName, nameRules);    		
	}

	public void bindView(Cursor cursor)
	{
		ContentResolver content = getContext().getContentResolver();
		
		long channelId = 
		  cursor.getLong(cursor.getColumnIndex(RSSReader.Channels._ID));

		/* Determine number of unread posts. */
		Cursor unread = 
		  content.query(RSSReader.Posts.CONTENT_URI_LIST.addId(channelId), 
		    new String[] { RSSReader.Posts._ID }, "read=0", null, null);

		Typeface tf;

		int unreadCount = unread.count();

		if (unreadCount > 0)
			tf = Typeface.DEFAULT_BOLD;
		else
			tf = Typeface.DEFAULT;

		String iconData =
		  cursor.getString(cursor.getColumnIndex(RSSReader.Channels.ICON));

		if (iconData != null)
		{
			byte[] raw = iconData.getBytes();

			mIcon.setImageBitmap
			(BitmapFactory.decodeByteArray(raw, 0, raw.length));
		}
		else
		{			
			mIcon.setImageResource(R.drawable.feedicon);
		}

		mName.setTypeface(tf);
		mName.setText(cursor, cursor.getColumnIndex(RSSReader.Channels.TITLE));

		mCount.setTypeface(tf);
		mCount.setText(new Integer(unreadCount).toString());
	}
	
	public void startRefresh()
	{
		Log.d("RSSChannelListRow", "Start refresh...");
		mCount.setVisibility(GONE);
		mRefresh.setVisibility(VISIBLE);
	}
	
	public void updateRefresh(int progress)
	{
		Log.d("RSSChannelListRow", "Switch to value-based, update to: " + progress);
	}
	
	public void finishRefresh()
	{
		Log.d("RSSChannelListRow", "Finished refresh, reset original view...");
		mCount.setVisibility(VISIBLE);
		mRefresh.setVisibility(GONE);
	}
}
