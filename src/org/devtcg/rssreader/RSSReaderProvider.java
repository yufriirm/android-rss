/* 
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.content.ContentProvider;
import android.content.ContentProviderDatabaseHelper;
import android.content.ContentURIParser;
import android.content.ContentValues;
import android.content.QueryBuilder;
import android.content.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ContentURI;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class RSSReaderProvider extends ContentProvider {
	private SQLiteDatabase mDB;
	
	private static final String TAG = "RSSReaderProvider";
	private static final String DATABASE_NAME = "rss_reader.db";
	private static final int DATABASE_VERSION = 2;
	
	private static HashMap<String, String> CHANNEL_LIST_PROJECTION_MAP;
	
	private static final int CHANNELS = 1;
	private static final int CHANNEL_ID = 2;
	
	private static final ContentURIParser URL_MATCHER;
	
	private static class DatabaseHelper extends ContentProviderDatabaseHelper
	{
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE rssreader_channel (_id INTEGER PRIMARY KEY," +
			           "	title TEXT, url TEXT);");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS rssreader_channel");
			onCreate(db);
		}
	}
	
	@Override
	public boolean onCreate()
	{
		DatabaseHelper dbHelper = new DatabaseHelper();
		mDB = dbHelper.openDatabase(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
		
		return (mDB == null) ? false : true;
	}
	
	@Override
	public Cursor query(ContentURI url, String[] projection, String selection,
			String[] selectionArgs, String groupBy, String having, String sort)
	{
		QueryBuilder qb = new QueryBuilder();

		qb.setTables("rssreader_channel");
		
		switch(URL_MATCHER.match(url))
		{
		case CHANNELS:
			qb.setTables("rssreader_channel");
			qb.setProjectionMap(CHANNEL_LIST_PROJECTION_MAP);
			break;
			
		case CHANNEL_ID:
			qb.setTables("rssreader_channel");
			qb.appendWhere("_id=" + url.getPathSegment(1));
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		String orderBy;
		if (TextUtils.isEmpty(sort))
			orderBy = RSSReader.Channels.DEFAULT_SORT_ORDER;
		else
			orderBy = sort;
		
		Cursor c = qb.query(mDB, projection, selection, selectionArgs,
				groupBy, having, orderBy);
		
		c.setNotificationUri(getContext().getContentResolver(), url);
		
		return c;
	}
	
	@Override
	public String getType(ContentURI url)
	{
		switch(URL_MATCHER.match(url))
		{
		case CHANNELS:
			return "vnd.android.cursor.dir/vnd.rssreader.channel";
		case CHANNEL_ID:
			return "vnd.android.cursor.item/vnd.rssreader.channel";
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
	}
	
	@Override
	public ContentURI insert(ContentURI url, ContentValues initialValues)
	{
		long rowID;
		ContentValues values;
		
		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();
		
		if (URL_MATCHER.match(url) != CHANNELS)
			throw new IllegalArgumentException("Unknown URL " + url);
		
		Resources r = Resources.getSystem();
		
		if (values.containsKey(RSSReader.Channels.TITLE) == false)
			values.put(RSSReader.Channels.TITLE, r.getString(android.R.string.untitled));
		
		rowID = mDB.insert("rssreader_channel", "title", values);

		if (rowID > 0)
		{
			ContentURI uri = RSSReader.Channels.CONTENT_URI.addId(rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		
		throw new SQLException("Failed to insert row into " + url);
	}
	
	@Override
	public int delete(ContentURI url, String where, String[] whereArgs)
	{
		int count;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.delete("rssreader_channel", where, whereArgs);
			break;
			
		case CHANNEL_ID:
			String segment = url.getPathSegment(1);
			String myWhere = "_id=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.delete("rssreader_channel", myWhere, whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}
	
	@Override
	public int update(ContentURI url, ContentValues values, String where, String[] whereArgs)
	{
		int count;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.update("rssreader_channel", values, where, whereArgs);
			break;
			
		case CHANNEL_ID:
			String segment = url.getPathSegment(1);
			String myWhere = "_id=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.update("rssreader_channel", values, myWhere, whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}
	
	static
	{
		URL_MATCHER = new ContentURIParser(ContentURIParser.NO_MATCH);
		URL_MATCHER.addURI("org.devtcg.rssprovider.RSSReader", "channels", CHANNELS);
		URL_MATCHER.addURI("org.devtcg.rssprovider.RSSReader", "channels/#", CHANNELS);
		
		CHANNEL_LIST_PROJECTION_MAP = new HashMap<String, String>();
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels._ID, "_id");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.TITLE, "title");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.URL, "url");
	}
}
