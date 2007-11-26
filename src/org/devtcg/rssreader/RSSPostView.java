/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class RSSPostView extends Activity
{
	private static final String[] PROJECTION = new String[] {
		RSSReader.Posts._ID, RSSReader.Posts.CHANNEL_ID,
		RSSReader.Posts.TITLE, RSSReader.Posts.BODY, RSSReader.Posts.READ };
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);		
		setContentView(R.layout.post_view);
		
		/* TODO: Should this be in onStart() or onResume() or something? */
		initWithData();
	}
	
	private final void initWithData()
	{	
		ContentResolver cr = getContentResolver();

		/* Get the post data, including a reference to the CHANNEL_ID. */
		Cursor cPost = cr.query(getIntent().getData(), PROJECTION, null, null, null);

		/* TODO: How are we supposed to manage this?  What possible cases can
		 * get us here with no real data? */
		assert(cPost.count() == 1);
		cPost.first();
		
		/* Set the post to read. */
		cPost.updateInt(cPost.getColumnIndex(RSSReader.Posts.READ), 1);
		cPost.commitUpdates();
		
		/* Resolve the channel title by CHANNEL_ID. */
		long channelId = new Long
		  (cPost.getString(cPost.getColumnIndex(RSSReader.Posts.CHANNEL_ID))).
		    longValue();
		
		Cursor cChannel = cr.query(RSSReader.Channels.CONTENT_URI.addId(channelId),
		  new String[] { RSSReader.Channels.ICON, RSSReader.Channels.LOGO, RSSReader.Channels.TITLE }, null, null, null);
				
		assert(cChannel.count() == 1);
		cChannel.first();
		
		/* Make the view useful. */
		RSSChannelHead head = (RSSChannelHead)findViewById(R.id.postViewHead);
		head.setLogo(cChannel);
		
		TextView postTitle = (TextView)findViewById(R.id.postTitle);
		postTitle.setText(cPost, cPost.getColumnIndex(RSSReader.Posts.TITLE));
		
		WebView postText = (WebView)findViewById(R.id.postText);
		
		/* TODO: I want the background transparent, but that doesn't seem 
		 * possible.  Black will do for now. */
		String html =
			"<html><head><style type=\"text/css\">body { background-color: #201c19; color: white; } a { color: #ddf; }</style></head><body>" +
			cPost.getString(cPost.getColumnIndex(RSSReader.Posts.BODY)) +
			"</body></html>";

		postText.loadData(html, "text/html", "utf-8");
	}
}
