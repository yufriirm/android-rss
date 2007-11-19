/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.devtcg.rssprovider.RSSReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class RSSChannelList extends ListActivity {
	
	public static final int DELETE_ID = Menu.FIRST;
	public static final int INSERT_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	
	private Cursor mCursor;
	
    private static final String[] PROJECTION = new String[] {
    	RSSReader.Channels._ID, RSSReader.Channels.TITLE, RSSReader.Channels.URL };
    
    /* Special handler that can capture progress messages to report back to this GUI. */
	protected ProgressDialog mRefreshBusy;	
	final Handler mRefreshHandler = new Handler()
    {
    	public void handleMessage(Message msg)
    	{
    		if (msg.arg1 == 0xDEADBEEF)
    		{
    			Log.i("RSSChannelList", "Got DEADBEEF");
    			mRefreshBusy.dismiss();
    		}
    		else
    		{
    			String qName = (String)msg.obj;
        		Log.d("RSSChannelList", "Got a message: " + qName);
    		}
    	}
    };
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);
        
        if (intent.getAction() == null)
        	intent.setAction(Intent.VIEW_ACTION);
        
        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);
        
        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, mCursor,
                new String[] { RSSReader.Channels.TITLE }, new int[] { android.R.id.text1 });
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
    	
    	/* TODO: Replace with an interface that does not block the user from
    	 * continuing to use the program to access existing feed posts. */
    	mRefreshBusy = ProgressDialog.show(this,
    		"Downloading", "Synchronizing new RSS posts...", true, false);
 
    	Thread t = new Thread()
    	{
    		public void run()
    		{
    			Log.e("RSSChannelList", "Here we go: " + rssurl + "...");
    			
    	    	new RSSChannelRefresh(getContentResolver()).
    	    	  syncDB(mRefreshHandler, id, rssurl);
    	    	
    	    	Message done = mRefreshHandler.obtainMessage();
    	    	done.arg1 = 0xDEADBEEF;
    	    	mRefreshHandler.sendMessage(done);
    	    	
    	    	Log.i("RSSChannelList", "Sent 0xDEADBEEF");
       		}
    	};
    	
    	t.start();
    }
}