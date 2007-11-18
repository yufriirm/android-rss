/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RSSChannelRefresh extends DefaultHandler
{
	final String TAG = "RSSChannelRefresh";
	
	public RSSChannelRefresh()
	{
		super();
	}
	
	public void startDocument()
	{
		Log.i(TAG, "Start document");
	}
	
	public void endDocument()
	{
		Log.i(TAG, "End document");
	}
	
	public void startElement(String uri, String name, String qName,
		Attributes attrs)
	{
		if ("".equals(uri))
			Log.i(TAG, "Start element: " + qName);
		else
			Log.i(TAG, "Start element: {" + uri + "}, " + name);
	}
	
	public void endElement(String uri, String name, String qName)
	{
		if ("".equals(uri))
			Log.i(TAG, "End element: " + qName);
		else
			Log.i(TAG, "End element: {" + uri + "}, " + name);
	}
	
	public void characters(char ch[], int start, int length)
	{
		StringBuffer buf = new StringBuffer("Characters: \"");
		
		for (int i = start; i < start + length; i++)
		{
			switch(ch[i])
			{
		    case '\\':
				buf.append("\\\\");
				break;
			case '"':
				buf.append("\\\"");
				break;
			case '\n':
				buf.append("\\n");
				break;
			case '\r':
				buf.append("\\r");
				break;
			case '\t':
				buf.append("\\t");
				break;
			default:
				buf.append(ch[i]);
				break;
			}
		}
		
		buf.append("\"");
		Log.i(TAG, buf.toString());
	}
}