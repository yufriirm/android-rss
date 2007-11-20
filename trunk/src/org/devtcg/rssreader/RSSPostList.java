/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
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
}
