/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.devtcg.rssprovider.RSSReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class RSSChannelList extends ListActivity {
	
	public static final int DELETE_ID = Menu.FIRST;
	public static final int INSERT_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	
	private Cursor mCursor;
	
    private static final String[] PROJECTION = new String[] {
    	RSSReader.Channels._ID, RSSReader.Channels.ICON,
    	RSSReader.Channels.TITLE, RSSReader.Channels.URL };
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);
        
        if (intent.getAction() == null)
        	intent.setAction(Intent.VIEW_ACTION);
        
        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);
        
        ListAdapter adapter = new RSSChannelListAdapter(mCursor, this);        
        setListAdapter(adapter);
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);
    	
    	menu.add(0, INSERT_ID, "New Channel").
    	  setShortcut(KeyEvent.KEYCODE_3, 0, KeyEvent.KEYCODE_A);
    	
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
    	final boolean haveItems = mCursor.count() > 0;
    	
		menu.removeGroup(Menu.SELECTED_ALTERNATIVE);
    	
    	/* If there are items in the list, add the extra context menu entries
    	 * available on each channel listed. */
    	if (haveItems)
    	{
    		/* Get initially selected item... 
    		 * TODO: Use this for Intent.EDIT_ACTION */
//    		ContentURI uri = getIntent().getData().addId(getSelectionRowID());

    		menu.add(Menu.SELECTED_ALTERNATIVE, REFRESH_ID, "Refresh Channel").
    		  setShortcut(0, 0, KeyEvent.KEYCODE_R);
    		
    		menu.add(Menu.SELECTED_ALTERNATIVE, DELETE_ID, "Delete Channel").
    		  setShortcut(KeyEvent.KEYCODE_2, 0, KeyEvent.KEYCODE_D);
    		
    		menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);
    	}
    	
    	/* TODO: What does this do?  Found it in the NotesList demo so I just
    	 * decided to copy it for good measure. */
//    	menu.setGroupShown(Menu.SELECTED_ALTERNATIVE, haveItems);

    	return true;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
    	String action = getIntent().getAction();
    	
    	if (action.equals(Intent.PICK_ACTION) ||
    	    action.equals(Intent.GET_CONTENT_ACTION))
    	{
    		ContentURI uri = getIntent().getData().addId(getSelectionRowID());
    		setResult(RESULT_OK, uri.toString());
    	}
    	else
    	{
    		ContentURI uri = RSSReader.Posts.CONTENT_URI_LIST.addId(getSelectionRowID());
    		startActivity(new Intent(Intent.VIEW_ACTION, uri));
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch(item.getId())
    	{
    	case INSERT_ID:
    		startActivity(new Intent(Intent.INSERT_ACTION, getIntent().getData()));
    		return true;
    		
    	case DELETE_ID:
    		deleteChannel();
    		return true;
    		
    	case REFRESH_ID:
    		refreshChannel();
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    private final void deleteChannel()
    {
    	String channelId;
    	
		mCursor.moveTo(getSelection());
		
		channelId = mCursor.getString
		  (mCursor.getColumnIndex(RSSReader.Channels._ID));

		/* Delete related posts. */
		getContentResolver().delete(RSSReader.Posts.CONTENT_URI,
    			"channel_id=?", new String[] { channelId });

		mCursor.deleteRow();
    }
    
    private final void refreshChannel()
    {
    	mCursor.moveTo(getSelection());    	
    	
    	final String id = mCursor.getString(mCursor.getColumnIndex(RSSReader.Channels._ID));
    	final String rssurl = mCursor.getString(mCursor.getColumnIndex(RSSReader.Channels.URL));

    	long channelId =
    	  mCursor.getInt(mCursor.getColumnIndex(RSSReader.Channels._ID));

    	final RSSChannelListRow row =
    	  ((RSSChannelListAdapter)getListAdapter()).getViewByRowID(channelId);
    	
		final RSSChannelRefreshHandler handler =
		  new RSSChannelRefreshHandler(row);		

    	assert(row != null);  
    	row.startRefresh();
    	
    	Thread t = new Thread()
    	{
    		public void run()
    		{
    			Log.e("RSSChannelList", "Here we go: " + rssurl + "...");
    			
    	    	new RSSChannelRefresh(getContentResolver()).
    	    	  syncDB(handler, id, rssurl);
    	    	
    	    	Message done = handler.obtainMessage();
    	    	done.arg1 = 0xDEADBEEF;
    	    	handler.sendMessage(done);
    	    	
    	    	Log.i("RSSChannelList", "Sent 0xDEADBEEF");
       		}
    	};
    	
    	t.start();
    }
    
    private static class RSSChannelListAdapter extends CursorAdapter implements Filterable
    {
    	private ContentResolver mContent;
    	
    	/* TODO: Android should provide a way to look up a View by row, but
    	 * it does not currently.  Hopefully this will be fixed in future
    	 * releases. */
    	private HashMap<Long, RSSChannelListRow> rowMap;
    	
		public RSSChannelListAdapter(Cursor c, Context context)
		{
			super(c, context);
			mContent = context.getContentResolver();
			rowMap = new HashMap<Long, RSSChannelListRow>();
		}
		
		protected void updateRowMap(Cursor cursor, RSSChannelListRow row)
		{
			Long channelId =
			  new Long(cursor.getLong(cursor.getColumnIndex(RSSReader.Channels._ID)));
			
			rowMap.put(channelId, row);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			RSSChannelListRow row = (RSSChannelListRow)view;
			row.bindView(mContent, cursor);
			updateRowMap(cursor, row);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			RSSChannelListRow row = new RSSChannelListRow(context);
			row.bindView(mContent, cursor);
			updateRowMap(cursor, row);
			return row;
		}
		
		public RSSChannelListRow getViewByRowID(long id)
		{
			return rowMap.get(new Long(id));
		}
    }
    
    private class RSSChannelRefreshHandler extends Handler
    {
    	RSSChannelListRow mRow;
    	
    	public RSSChannelRefreshHandler(RSSChannelListRow row)
    	{
    		super();
    		mRow = row;
    	}
    	
    	public void handleMessage(Message msg)
    	{
    		if (msg.arg1 == 0xDEADBEEF)
    		{
    			Log.i("RSSChannelList", "Got 0xDEADBEEF");
    			mRow.finishRefresh();
    			
    			/* TODO: We should use the notification system (setNotificationUri)
    			 * in the provider to help facilitate this. */
    			mCursor.requery();
    		}
    		else
    		{
        		Log.d("RSSChannelList", "Got a message: " + msg.arg1);
    			mRow.updateRefresh(msg.arg1);
    		}
    	}    	 
    }
}