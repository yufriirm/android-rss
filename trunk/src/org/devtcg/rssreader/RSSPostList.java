/*
 * $Id$
 *
 * Copyright (C) 2007 Josh Guilfoyle <jasta@devtcg.org>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;

public class RSSPostList extends ListActivity
{
	private static final String[] PROJECTION = new String[] {
	  RSSReader.Posts._ID, RSSReader.Posts.CHANNEL_ID,
	  RSSReader.Posts.TITLE, RSSReader.Posts.READ,
	  RSSReader.Posts.DATE };
	
	private Cursor mCursor;

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		
		setContentView(R.layout.post_list);
		
		mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);
		
		ListAdapter adapter = new RSSPostListAdapter(mCursor, this);
        setListAdapter(adapter);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		
		long channelId = new Long(getIntent().getData().getPathSegment(1));
		
		ContentResolver cr = getContentResolver();		
		Cursor cChannel = cr.query(RSSReader.Channels.CONTENT_URI.addId(channelId),
		  new String[] { RSSReader.Channels.LOGO, RSSReader.Channels.ICON, RSSReader.Channels.TITLE }, null, null, null);

		assert(cChannel.count() == 1);
		cChannel.first();

		/* TODO: Check if RSSReader.Channels.LOGO exists and use it. */
		RSSChannelHead head = (RSSChannelHead)findViewById(R.id.postListHead);
		head.setLogo(cChannel);
		
		cChannel.close();
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
		ContentURI uri = RSSReader.Posts.CONTENT_URI.addId(getSelectionRowID());
    	String action = getIntent().getAction();
    	
    	if (action.equals(Intent.PICK_ACTION) ||
    	    action.equals(Intent.GET_CONTENT_ACTION))
    	{
    		setResult(RESULT_OK, uri.toString());
    	}
    	else
    	{
    		startActivity(new Intent(Intent.VIEW_ACTION, uri));
    	}
    }

    private static class RSSPostListAdapter extends CursorAdapter implements Filterable
    {
		public RSSPostListAdapter(Cursor c, Context context)
		{
			super(c, context);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			((RSSPostListRow)view).bindView(cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			RSSPostListRow post = new RSSPostListRow(context);
			post.bindView(cursor);
			return post;
		}
    }
}
