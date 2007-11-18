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
		public static final ContentURI CONTENT_URI = 
			ContentURI.create("content://org.devtcg.rssprovider.RSSReader/channels");
		
		public static final String DEFAULT_SORT_ORDER = "title ASC";
		
		/* User-controllable RSS channel name. */
		public static final String TITLE = "title";
		
		/* RSS Feed URL. */
		public static final String URL = "url";
	}
	
	public static final class Posts implements BaseColumns
	{
		public static final ContentURI CONTENT_URI = 
			ContentURI.create("content://org.devtcg.rssprovider.RSSReader/posts");
		
		public static final String DEFAULT_SORT_ORDER = "date DESC";
		
		/* Reference to the channel _ID to which this post belongs. */
		public static final String CHANNEL_ID = "channel_id";
		
		/* Post subject. */
		public static final String TITLE = "title";
		
		/* Post author. */
		public static final String AUTHOR = "author";
		
		/* "Read more..." URL. */
		public static final String URL = "url";
		
		/* Post text. */
		public static final String BODY = "body";
	
		/* Date of the post. */
		public static final String DATE = "posted";
	}
}
