/* 
 * $Id$
 *
 * Copyright (C) 2007 Josh Guilfoyle <jasta@devtcg.org>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * lateeeer version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.devtcg.rssreader.provider;


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

public class RSSReaderProvider extends ContentProvider
{
	private SQLiteDatabase mDB;
	
	private static final String TAG = "RSSReaderProvider";
	private static final String DATABASE_NAME = "rss_reader.db";
	private static final int DATABASE_VERSION = 6;
	
	private static HashMap<String, String> CHANNEL_LIST_PROJECTION_MAP;
	private static HashMap<String, String> POST_LIST_PROJECTION_MAP;
	
	private static final int CHANNELS = 1;
	private static final int CHANNEL_ID = 2;
	private static final int POSTS = 3;
	private static final int POST_ID = 4;
	private static final int CHANNEL_POSTS = 5;
	
	private static final ContentURIParser URL_MATCHER;
	
	private static class DatabaseHelper extends ContentProviderDatabaseHelper
	{
		protected void onCreateChannels(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE rssreader_channel (_id INTEGER PRIMARY KEY," +
	           "	title TEXT UNIQUE, url TEXT UNIQUE, icon BLOB, logo BLOB);");
		}
		
		protected void onCreatePosts(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE rssreader_post (_id INTEGER PRIMARY KEY," +
			           "    channel_id INTEGER, title TEXT, url TEXT, " + 
			           "    posted_on DATETIME, body TEXT, author TEXT, read INTEGER(1) DEFAULT '0');");

			/* TODO: Should we narrow this more to just URL _or_ title? */
			db.execSQL("CREATE UNIQUE INDEX unq_post ON rssreader_post (title, url);");
			
			/* Create an index to efficiently access posts on a particular channel. */
			db.execSQL("CREATE INDEX idx_channel ON rssreader_post (channel_id);");
		}
		
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			onCreateChannels(db);
			onCreatePosts(db);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			assert(newVersion == DATABASE_VERSION);
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + "...");

			switch(oldVersion)
			{
			/* This doesn't work, and I don't know why... */
			case 4:
				/* All we did was add a DEFAULT clause to the read field, 
				 * but SQLite3 does not support column alteration. */
				db.execSQL("UPDATE rssreader_post SET read = 0 WHERE (read IS NULL OR read != 1);");
				db.execSQL("ALTER TABLE rssreader_post RENAME TO rssreader_post_tmp;");
				onCreatePosts(db);
				db.execSQL("INSERT INTO rssreader_post SELECT * FROM rssreader_post_tmp;");
				db.execSQL("DROP TABLE rssreader_post_tmp;");
				break;

			case 5:
				db.execSQL("ALTER TABLE rssreader_channel ADD COLUMN icon BLOB;");
				db.execSQL("ALTER TABLE rssreader_channel ADD COLUMN logo BLOB;");
				break;

			default:
				Log.w(TAG, "Version too old, wiping out database contents...");
				db.execSQL("DROP TABLE IF EXISTS rssreader_channel;");
				db.execSQL("DROP TABLE IF EXISTS rssreader_post;");
				onCreate(db);
				break;
			}
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
		
		String defaultSort;
		
		switch(URL_MATCHER.match(url))
		{
		case CHANNELS:
			qb.setTables("rssreader_channel");
			qb.setProjectionMap(CHANNEL_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Channels.DEFAULT_SORT_ORDER;
			break;
			
		case CHANNEL_ID:
			qb.setTables("rssreader_channel");
			qb.appendWhere("_id=" + url.getPathSegment(1));
			defaultSort = RSSReader.Channels.DEFAULT_SORT_ORDER;
			break;
			
			/*
		case POSTS:
			qb.setTables("rssreader_post");
			qb.setProjectionMap(POST_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
			break;
			*/
		case CHANNEL_POSTS:
			qb.setTables("rssreader_post");
			qb.appendWhere("channel_id=" + url.getPathSegment(1));
			qb.setProjectionMap(POST_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
			break;
			
		case POST_ID:
			qb.setTables("rssreader_post");
			qb.appendWhere("_id=" + url.getPathSegment(1));
			defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		String orderBy;

		if (TextUtils.isEmpty(sort))
			orderBy = defaultSort;
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
		case POSTS:
		case CHANNEL_POSTS:
			return "vnd.android.cursor.dir/vnd.rssreader.post";
		case POST_ID:
			return "vnd.android.cursor.item/vnd.rssreader.post";
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
	}
	
	private long insertChannels(ContentValues values)
	{
		Resources r = Resources.getSystem();
		
		/* TODO: This validation sucks. */
		if (values.containsKey(RSSReader.Channels.TITLE) == false)
			values.put(RSSReader.Channels.TITLE, r.getString(android.R.string.untitled));
		
		return mDB.insert("rssreader_channel", "title", values);
	}
	
	private long insertPosts(ContentValues values)
	{
		/* TODO: Validation? */
		return mDB.insert("rssreader_post", "title", values);
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
		
		ContentURI uri;
		
		if (URL_MATCHER.match(url) == CHANNELS)
		{
			rowID = insertChannels(values);
			uri = RSSReader.Channels.CONTENT_URI.addId(rowID);
		}
		else if (URL_MATCHER.match(url) == POSTS)
		{
			rowID = insertPosts(values);
			uri = RSSReader.Posts.CONTENT_URI.addId(rowID);
		}
		else
		{
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		if (rowID > 0)
		{
			assert(uri != null);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		
		throw new SQLException("Failed to insert row into " + url);
	}
	
	@Override
	public int delete(ContentURI url, String where, String[] whereArgs)
	{
		int count;
		String myWhere;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.delete("rssreader_channel", where, whereArgs);
			break;
			
		case CHANNEL_ID:
			myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.delete("rssreader_channel", myWhere, whereArgs);
			break;
			
		case POSTS:
			count = mDB.delete("rssreader_post", where, whereArgs);
			break;
			
		case POST_ID:
			myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.delete("rssreader_post", myWhere, whereArgs);
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
		String myWhere;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.update("rssreader_channel", values, where, whereArgs);
			break;
			
		case CHANNEL_ID:
			myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.update("rssreader_channel", values, myWhere, whereArgs);
			break;
			
		case POSTS:
			count = mDB.update("rssreader_post", values, where, whereArgs);
			break;
			
		case POST_ID:
			myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.update("rssreader_post", values, myWhere, whereArgs);
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
		URL_MATCHER.addURI("org.devtcg.rssreader.provider.RSSReader", "channels", CHANNELS);
		URL_MATCHER.addURI("org.devtcg.rssreader.provider.RSSReader", "channels/#", CHANNEL_ID);
		URL_MATCHER.addURI("org.devtcg.rssreader.provider.RSSReader", "posts", POSTS);
		URL_MATCHER.addURI("org.devtcg.rssreader.provider.RSSReader", "posts/#", POST_ID);
		URL_MATCHER.addURI("org.devtcg.rssreader.provider.RSSReader", "postlist/#", CHANNEL_POSTS);	
		
		CHANNEL_LIST_PROJECTION_MAP = new HashMap<String, String>();
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels._ID, "_id");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.TITLE, "title");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.URL, "url");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.ICON, "icon");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.LOGO, "logo");
		
		POST_LIST_PROJECTION_MAP = new HashMap<String, String>();
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts._ID, "_id");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.CHANNEL_ID, "channel_id");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.READ, "read");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.TITLE, "title");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.URL, "url");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.AUTHOR, "author");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.DATE, "posted_on");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.BODY, "body");
	}
}
