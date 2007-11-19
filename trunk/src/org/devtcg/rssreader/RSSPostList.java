/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class RSSPostList extends ListActivity
{
	private static final String[] PROJECTION = new String[] {
		RSSReader.Posts._ID, RSSReader.Posts.TITLE };
	
	private Cursor mCursor;

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		
		mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);
        
        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, mCursor,
                new String[] { RSSReader.Posts.TITLE }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
	}
}
