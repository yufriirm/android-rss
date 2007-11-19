/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.devtcg.rssprovider.RSSReader;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RSSChannelRefresh extends DefaultHandler
{
	private static final String TAG = "RSSChannelRefresh";

	private Handler mHandler;
	private String mID;
	private String mRSSURL;
	
	private ContentResolver mContent;
	
	/* Buffer post information as we learn it in STATE_IN_ITEM. */
	private RSSChannelPost mPostBuf;
	
	/* Efficiency is the name of the game here... */
	private int mState;
	private static final int STATE_IN_ITEM = (1 << 2);
	private static final int STATE_IN_ITEM_TITLE = (1 << 3);
	private static final int STATE_IN_ITEM_LINK = (1 << 4);
	private static final int STATE_IN_ITEM_DESC = (1 << 5);
	private static final int STATE_IN_ITEM_DCDATE = (1 << 6);
	private static final int STATE_IN_ITEM_DCAUTHOR = (1 << 7);
	
	private static HashMap<String, Integer> mStateMap;
	
	static
	{
		mStateMap = new HashMap<String, Integer>();		
		mStateMap.put("item", new Integer(STATE_IN_ITEM));
		mStateMap.put("title", new Integer(STATE_IN_ITEM_TITLE));
		mStateMap.put("link", new Integer(STATE_IN_ITEM_LINK));
		mStateMap.put("description", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("dc:date", new Integer(STATE_IN_ITEM_DCDATE));
		mStateMap.put("dc:author", new Integer(STATE_IN_ITEM_DCAUTHOR));
	}

	public RSSChannelRefresh(ContentResolver resolver)
	{
		super();
		
		mContent = resolver;
	}

	public void syncDB(Handler h, String id, String rssurl)
	{
		mHandler = h;
		mID = id;
		mRSSURL = rssurl;

		try
		{
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			xr.setContentHandler(this);
			xr.setErrorHandler(this);

			URL url = new URL(mRSSURL);
			xr.parse(new InputSource(url.openStream()));
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}			
	}

	public void startElement(String uri, String name, String qName,
			Attributes attrs)
	{
		Integer state = mStateMap.get(qName);
		
		if (state != null)
		{
			mState |= state.intValue();

			if (state.intValue() == STATE_IN_ITEM)
				mPostBuf = new RSSChannelPost();
		}
	}

	public void endElement(String uri, String name, String qName)
	{
		Integer state = mStateMap.get(qName);
		
		if (state != null)
		{
			mState &= ~(state.intValue());
			
			if (state.intValue() == STATE_IN_ITEM)
			{
				String[] dupProj = 
				  new String[] { RSSReader.Posts._ID };
				
				ContentURI listURI =
				  RSSReader.Posts.CONTENT_URI_LIST.addId(new Long(mID).longValue());
				
				Cursor dup = mContent.query(listURI,
					dupProj, "title = ? AND url = ?",
					new String[] { mPostBuf.title, mPostBuf.link}, null);

				Log.d(TAG, "Post: " + mPostBuf.title);

				if (dup.count() == 0)
				{
					ContentValues values = new ContentValues();
				
					values.put(RSSReader.Posts.CHANNEL_ID, mID);
					values.put(RSSReader.Posts.TITLE, mPostBuf.title);
					values.put(RSSReader.Posts.URL, mPostBuf.link);
					values.put(RSSReader.Posts.AUTHOR, mPostBuf.author);
					values.put(RSSReader.Posts.DATE, mPostBuf.date);
					values.put(RSSReader.Posts.BODY, mPostBuf.desc);
				
					mContent.insert(RSSReader.Posts.CONTENT_URI, values);
				}
			}
		}
	}

	public void characters(char ch[], int start, int length)
	{
		if ((mState & STATE_IN_ITEM) == 0)
			return;
		
		/* 
		 * We sort of pretended that mState was inclusive, but really only
		 * STATE_IN_ITEM is inclusive here.  This is a goofy design, but it is
		 * done to make this code a bit simpler and more efficient.
		 */
		switch (mState)
		{
		case STATE_IN_ITEM | STATE_IN_ITEM_TITLE:
			mPostBuf.title = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DESC:
			mPostBuf.desc = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_LINK:
			mPostBuf.link = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DCDATE:
			mPostBuf.date = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DCAUTHOR:
			mPostBuf.author = new String(ch, start, length);
			break;
		default:
			/* Don't care... */
		}
	}
	
	private class RSSChannelPost
	{
		public String title;
		public String date;
		public String desc;
		public String link;
		public String author;
		
		public RSSChannelPost()
		{
			/* Empty. */
		}		
	}
}

