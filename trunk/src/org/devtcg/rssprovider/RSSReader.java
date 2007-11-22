/*
 * $Id$
 */

package org.devtcg.rssprovider;

import android.net.ContentURI;
import android.provider.BaseColumns;

public final class RSSReader
{
	public static final class Channels implements BaseColumns
	{
		/* URI for accessing a specific channel.  See Posts.CONTENT_URI_LIST
		 * for how to "view" the channel posts. */
		public static final ContentURI CONTENT_URI = 
			ContentURI.create("content://org.devtcg.rssprovider.RSSReader/channels");
		
		public static final String DEFAULT_SORT_ORDER = "title ASC";
		
		/* User-controllable RSS channel name. */
		public static final String TITLE = "title";
		
		/* RSS Feed URL. */
		public static final String URL = "url";
		
		/* Site's favicon; usually a guess. */
		public static final String ICON = "icon";
		
		/* Site's formal logo; derived from the XML feed. */
		public static final String LOGO = "logo";
	}
	
	public static final class Posts implements BaseColumns
	{
		/* URI for accessing a specific post. */
		public static final ContentURI CONTENT_URI = 
			ContentURI.create("content://org.devtcg.rssprovider.RSSReader/posts");
		
		/* URI for accessing a list of posts on a particular channel. */
		public static final ContentURI CONTENT_URI_LIST =
			ContentURI.create("content://org.devtcg.rssprovider.RSSReader/postlist");
		
		public static final String DEFAULT_SORT_ORDER = "posted_on DESC";
		
		/* Reference to the channel _ID to which this post belongs. */
		public static final String CHANNEL_ID = "channel_id";
		
		/* Boolean read value. */
		public static final String READ = "read"; 
		
		/* Post subject. */
		public static final String TITLE = "title";
		
		/* Post author. */
		public static final String AUTHOR = "author";
		
		/* "Read more..." URL. */
		public static final String URL = "url";
		
		/* Post text. */
		public static final String BODY = "body";
	
		/* Date of the post. */
		public static final String DATE = "posted_on";
	}
}
