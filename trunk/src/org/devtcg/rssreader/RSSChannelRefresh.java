/*
 * $Id$
 * 
 * TODO: This class needs to be generalized much better, with specialized
 * parsers for Atom 1.0, Atom 0.3, RSS 0.91, RSS 1.0 and RSS 2.0.  Hell,
 * this whole thing needs to be chucked and redone.
 * 
 * Parser code lifted from Informa <http://informa.sourceforge.net>.
 */

package org.devtcg.rssreader;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.devtcg.rssprovider.RSSReader;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

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
	private static final int STATE_IN_ITEM_DATE = (1 << 6);
	private static final int STATE_IN_ITEM_AUTHOR = (1 << 7);
	
	private static HashMap<String, Integer> mStateMap;
	
	private static final SimpleDateFormat[] dateFormats;
	private static final int dateFormat_default;
	
	static
	{
		mStateMap = new HashMap<String, Integer>();		
		mStateMap.put("item", new Integer(STATE_IN_ITEM));
		mStateMap.put("entry", new Integer(STATE_IN_ITEM));
		mStateMap.put("title", new Integer(STATE_IN_ITEM_TITLE));
		mStateMap.put("link", new Integer(STATE_IN_ITEM_LINK));
		mStateMap.put("description", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("content", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("dc:date", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("updated", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("pubDate", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("dc:author", new Integer(STATE_IN_ITEM_AUTHOR));
		mStateMap.put("author", new Integer(STATE_IN_ITEM_AUTHOR));
		
		dateFormat_default = 6;
	    final String[] possibleDateFormats =
	    {
	    	"EEE, dd MMM yyyy HH:mm:ss z", // RFC_822
	    	"EEE, dd MMM yyyy HH:mm zzzz",
	    	"yyyy-MM-dd'T'HH:mm:ssZ",
	    	"yyyy-MM-dd'T'HH:mm:ss.SSSzzzz", // Blogger Atom feed has millisecs also
	    	"yyyy-MM-dd'T'HH:mm:sszzzz",
	    	"yyyy-MM-dd'T'HH:mm:ss z",
	    	"yyyy-MM-dd'T'HH:mm:ssz", // ISO_8601
	    	"yyyy-MM-dd'T'HH:mm:ss",
	    	"yyyy-MM-dd'T'HHmmss.SSSz",
	    	"yyyy-MM-dd"
	    };

	    dateFormats = new SimpleDateFormat[possibleDateFormats.length];
	    TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
	    
	    for (int i = 0; i < possibleDateFormats.length; i++)
	    {
	    	/* TODO: Support other locales? */
	    	dateFormats[i] = new SimpleDateFormat(possibleDateFormats[i],
	    	  Locale.ENGLISH);
	    	
	    	dateFormats[i].setTimeZone(gmtTZ);
	    }
	}

	public RSSChannelRefresh(ContentResolver resolver)
	{
		super();
		
		mContent = resolver;
	}
	
	/*
	 * Simple wrapper class that shows how much data is being read, 
	 * as it's being read.  This enables us to create a ProgressBar
	 * experience for the user when the Content-Length header is
	 * found on an HTTP stream.
	 */
	private class ProgressInputStream extends InputStream
	{
		private InputStream mWrapped;
		private Handler mAnnounce;
		
		private long mRecvdLast;
		private long mRecvd;
		private long mTotal;
		
		public ProgressInputStream(InputStream in, Handler announce, long total)
		{
			mWrapped = in;
			mAnnounce = announce;
			mRecvd = 0;
			mTotal = total;
		}
		
		protected void announceReceipt(long len)
		{
			mRecvd += len;
			assert(mRecvd <= mTotal);

			/* Only update percentage every 1k. */
			if (mRecvd - mRecvdLast >= 1024)
			{
				Message step = mAnnounce.obtainMessage();
				step.arg1 = (int)(((float)mRecvd / (float)mTotal) * 100.0);
				mAnnounce.sendMessage(step);
				
				mRecvdLast = mRecvd;
			}
		}
		
		public int read() throws IOException
		{
			int b = mWrapped.read();
			
			if (b >= 0) 
				announceReceipt(1);
			
			return b;
		}
		
		public int read(byte[] b, int off, int len) throws IOException
		{
			int n = mWrapped.read(b, off, len);
			
			if (n >= 0)
				announceReceipt(n);
			
			return n;
		}

		public long skip(long n) throws IOException
		{
			long skipped = mWrapped.skip(n);
			
			if (skipped >= 0)
				announceReceipt(skipped);
			
			return skipped;
		}
		
		public void close() throws IOException { mWrapped.close(); }		
		public int read(byte[] b) throws IOException { return read(b, 0, b.length); }
		public int available() throws IOException { return mWrapped.available(); }
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

			//URL url = new URL(mRSSURL);

			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);

			GetMethod get = new GetMethod(mRSSURL);
			get.setFollowRedirects(true);

			int result = client.executeMethod(get);
			Log.d("RSSChannelRefresh", "GET " + mRSSURL + " = " + result);

			long len = get.getResponseContentLength();
			Log.d("RSSChannelRefresh", "Content-Length: " + len);
			
			InputStream stream = get.getResponseBodyAsStream();
			
			if (len > 0)
				stream = new ProgressInputStream(stream, mHandler, len);

			xr.parse(new InputSource(stream));
		}
		catch (Exception e)
		{
			String msg = e.getMessage();
			
			if (msg != null)
				Log.e(TAG, e.getMessage());
			else
				Log.e(TAG, "what the hell?");
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
			else if ((mState & STATE_IN_ITEM) != 0 && state.intValue() == STATE_IN_ITEM_LINK)
			{
				String href = attrs.getValue("href");
				
				if (href != null)
					mPostBuf.link = href;
			}
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
					values.put(RSSReader.Posts.DATE, mPostBuf.getDate());
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
		case STATE_IN_ITEM | STATE_IN_ITEM_DATE:
			mPostBuf.setDate(new String(ch, start, length));
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_AUTHOR:
			mPostBuf.author = new String(ch, start, length);
			break;
		default:
			/* Don't care... */
		}
	}
	
	/* Copied verbatim from Informa 0.7.0-alpha2 ParserUtils.java. */
	private static Date parseDate(String strdate) {
		Date result = null;
		strdate = strdate.trim();
		if (strdate.length() > 10) {

			// TODO deal with +4:00 (no zero before hour)
			if ((strdate.substring(strdate.length() - 5).indexOf("+") == 0 || strdate
					.substring(strdate.length() - 5).indexOf("-") == 0)
					&& strdate.substring(strdate.length() - 5).indexOf(":") == 2) {

				String sign = strdate.substring(strdate.length() - 5,
						strdate.length() - 4);

				strdate = strdate.substring(0, strdate.length() - 5) + sign + "0"
				+ strdate.substring(strdate.length() - 4);
				// logger.debug("CASE1 : new date " + strdate + " ? "
				//    + strdate.substring(0, strdate.length() - 5));

			}

			String dateEnd = strdate.substring(strdate.length() - 6);

			// try to deal with -05:00 or +02:00 at end of date
			// replace with -0500 or +0200
			if ((dateEnd.indexOf("-") == 0 || dateEnd.indexOf("+") == 0)
					&& dateEnd.indexOf(":") == 3) {
				// TODO deal with GMT-00:03
				if ("GMT".equals(strdate.substring(strdate.length() - 9, strdate
						.length() - 6))) {
					Log.d(TAG, "General time zone with offset, no change");
				} else {
					// continue treatment
					String oldDate = strdate;
					String newEnd = dateEnd.substring(0, 3) + dateEnd.substring(4);
					strdate = oldDate.substring(0, oldDate.length() - 6) + newEnd;
					// logger.debug("!!modifying string ->"+strdate);
				}
			}
		}
		int i = 0;
		while (i < dateFormats.length) {
			try {
				result = dateFormats[i].parse(strdate);
				// logger.debug("******Parsing Success "+strdate+"->"+result+" with
				// "+dateFormats[i].toPattern());
				break;
			} catch (java.text.ParseException eA) {
				i++;
			}
		}

		return result;
	}
	
	private class RSSChannelPost
	{
		public String title;
		public Date date;
		public String desc;
		public String link;
		public String author;
		
		public RSSChannelPost()
		{
			/* Empty. */
		}
		
		public void setDate(String str)
		{
			date = parseDate(str);
			
			if (date == null)
				date = new Date();
		}
		
		public String getDate()
		{
			return dateFormats[dateFormat_default].format(mPostBuf.date);
		}
	}
}

