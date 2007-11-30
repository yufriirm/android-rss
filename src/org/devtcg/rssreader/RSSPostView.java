/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.TextView;

public class RSSPostView extends Activity
{
	private final static int NEXT_POST_ID = Menu.FIRST;
	private final static int PREV_POST_ID = Menu.FIRST + 1;
	
	private static final String[] PROJECTION = new String[] {
		RSSReader.Posts._ID, RSSReader.Posts.CHANNEL_ID,
		RSSReader.Posts.TITLE, RSSReader.Posts.BODY, RSSReader.Posts.READ,
		RSSReader.Posts.URL };
	
	private long mChannelID = -1;
	
	private Cursor mCursor;
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);		
		setContentView(R.layout.post_view);

		mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null, null);
		
		/* TODO: Should this be in onStart() or onResume() or something?  */
		initWithData();
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();

		assert(mCursor.count() == 1);
		mCursor.first();

		/* Set the post to read. */
		mCursor.updateInt(mCursor.getColumnIndex(RSSReader.Posts.READ), 1);
		mCursor.commitUpdates();
	}
	
	private void initWithData()
	{	
		ContentResolver cr = getContentResolver();

		assert(mCursor.count() == 1);
		mCursor.first();

		/* Resolve the channel title by CHANNEL_ID. */
		if (mChannelID < 0)
		{
			mChannelID = new Long
			  (mCursor.getString(mCursor.getColumnIndex(RSSReader.Posts.CHANNEL_ID))).
			    longValue();
		}

		Cursor cChannel = cr.query(RSSReader.Channels.CONTENT_URI.addId(mChannelID),
		  new String[] { RSSReader.Channels.ICON, RSSReader.Channels.LOGO, RSSReader.Channels.TITLE }, null, null, null);

		assert(cChannel.count() == 1);
		cChannel.first();
		
		/* Make the view useful. */
		RSSChannelHead head = (RSSChannelHead)findViewById(R.id.postViewHead);
		head.setLogo(cChannel);
		
		TextView postTitle = (TextView)findViewById(R.id.postTitle);
		postTitle.setText(mCursor, mCursor.getColumnIndex(RSSReader.Posts.TITLE));
		
		WebView postText = (WebView)findViewById(R.id.postText);
		
		/* TODO: I want the background transparent, but that doesn't seem 
		 * possible.  Black will do for now. */
		String html =
			"<html><head><style type=\"text/css\">body { background-color: #201c19; color: white; } a { color: #ddf; }</style></head><body>" +
			getBody() +
			"</body></html>";

		postText.loadData(html, "text/html", "utf-8");
	}
	
	/* Apply some simple heuristics to the post text to determine what special
	 * features we want to show. */
	private String getBody()
	{
		String body =
		  mCursor.getString(mCursor.getColumnIndex(RSSReader.Posts.BODY));
		
		String url =
		  mCursor.getString(mCursor.getColumnIndex(RSSReader.Posts.URL));
	
		if (hasMoreLink(body, url) == false)
			body += "<p><a href=\"" + url + "\">Read more...</a></p>";
		
		/* TODO: We should add a check for "posted by", "written by",
		 * "posted on", etc, and optionally add our own tagline if
		 * the information is in the feed. */
		return body;
	}
	
	private boolean hasMoreLink(String body, String url)
	{
		int urlpos;
		
		/* Check if the body contains an anchor reference with the
		 * destination of the read more URL we got from the feed. */
		if ((urlpos = body.indexOf(url)) > 0)
		{
			try
			{
				/* TODO: Improve this check with a full look-behind parse. */
				if (body.charAt(urlpos - 1) != '>')
					return false;
			
				if (body.charAt(urlpos + url.length() + 1) != '<')
					return false;
			}
			catch (IndexOutOfBoundsException e)
			{
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

//    	menu.add(0, NEXT_POST_ID, "Next Post").
//    	  setShortcut(KeyEvent.KEYCODE_3, 0, KeyEvent.KEYCODE_N);
//		menu.add(0, PREV_POST_ID, "Previous Post").
//		  setShortcut(KeyEvent.KEYCODE_1, 0, KeyEvent.KEYCODE_P);

		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch(item.getId())
    	{
    	case NEXT_POST_ID:
    		return true;
    		
    	case PREV_POST_ID:
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
}
