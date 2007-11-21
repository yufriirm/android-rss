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
    
    /* Special handler that can capture progress messages to report back to this GUI. */
	protected ProgressDialog mRefreshBusy;	
	final Handler mRefreshHandler = new Handler()
    {
    	public void handleMessage(Message msg)
    	{
    		if (msg.arg1 == 0xDEADBEEF)
    		{
    			Log.i("RSSChannelList", "Got 0xDEADBEEF");
    			mRefreshBusy.dismiss();
    			
    			/* TODO: We should use the notification system (setNotificationUri)
    			 * in the provider to help facilitate this. */
    			mCursor.requery();
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
        
        Log.d("RSSChannelList", "onCreate");
        
        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);
        
        if (intent.getAction() == null)
        	intent.setAction(Intent.VIEW_ACTION);
        
        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);
        
//        long channelId =
//          mCursor.getLong(mCursor.getColumnIndex(RSSReader.Channels._ID));
//
//        mCursor.setNotificationUri(getContentResolver(),
//          RSSReader.Posts.CONTENT_URI_LIST.addId(channelId));
        
        ListAdapter adapter = new RSSChannelListAdapter(mCursor, this);
        
//        ListAdapter adapter = new SimpleCursorAdapter(this,
//                android.R.layout.simple_list_item_1, mCursor,
//                new String[] { RSSReader.Channels.TITLE }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
    }
	
    @Override
	protected void onResume()
	{
		super.onResume(); 
		
		Log.d("RSSChannelList", "onResume");
	}
	
    @Override
	protected void onFreeze(Bundle icicle)
	{
		super.onFreeze(icicle); 
		
		Log.d("RSSChannelList", "onFreeze");
	}
	
    @Override
	protected void onPause()
	{
		super.onPause();
		
		Log.d("RSSChannelList", "onPause");
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
    
    private static class RSSChannelListAdapter extends CursorAdapter implements Filterable
    {
    	private ContentResolver mContent;
    	
    	private static final int CHANNEL_NAME = 1;
    	private static final int CHANNEL_COUNT = 2;
    	private static final int CHANNEL_ICON = 3;
    	
		public RSSChannelListAdapter(Cursor c, Context context)
		{
			super(c, context);
			mContent = context.getContentResolver();
		}
		
		private void setViewData(RelativeLayout view, Cursor cursor)
		{
			long channelId = 
			  cursor.getLong(cursor.getColumnIndex(RSSReader.Channels._ID));
			
			/* Determine number of unread posts. */
			Cursor unread = 
			  mContent.query(RSSReader.Posts.CONTENT_URI_LIST.addId(channelId), 
			    new String[] { RSSReader.Posts._ID }, "read=0", null, null);
			
			Typeface tf;

			int unreadCount = unread.count();

			if (unreadCount > 0)
				tf = Typeface.DEFAULT_BOLD;
			else
				tf = Typeface.DEFAULT;
			
			ImageView icon = (ImageView)view.findViewById(CHANNEL_ICON);
			String iconData = cursor.getString(cursor.getColumnIndex(RSSReader.Channels.ICON));
			
			if (iconData != null)
			{
				byte[] raw = iconData.getBytes();
				
				icon.setImageBitmap
				  (BitmapFactory.decodeByteArray(raw, 0, raw.length));
			}
			else
			{			
				icon.setImageResource(R.drawable.feedicon);
			}

			TextView name = (TextView)view.findViewById(CHANNEL_NAME);
			name.setTypeface(tf);
			name.setText(cursor, cursor.getColumnIndex(RSSReader.Channels.TITLE));

			TextView count = (TextView)view.findViewById(CHANNEL_COUNT);
			count.setTypeface(tf);
			count.setText(new Integer(unreadCount).toString());
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			setViewData((RelativeLayout)view, cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			RelativeLayout channel = new RelativeLayout(context);
			
			channel.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			
			ImageView icon = new ImageView(context);
			icon.setPadding(0, 2, 3, 2);
			icon.setId(CHANNEL_ICON);
			
			RelativeLayout.LayoutParams iconRules =
			  new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			
			iconRules.addRule(RelativeLayout.ALIGN_WITH_PARENT_LEFT);
			channel.addView(icon, iconRules);
			
			TextView count = new TextView(context);
			count.setId(CHANNEL_COUNT);
			
			RelativeLayout.LayoutParams countRules =
			  new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			
			countRules.addRule(RelativeLayout.ALIGN_WITH_PARENT_RIGHT);
			channel.addView(count, countRules);
			
			TextView name = new TextView(context);
			name.setPadding(3, 0, 0, 0);
			name.setId(CHANNEL_NAME);
			
			RelativeLayout.LayoutParams nameRules =
			  new RelativeLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);
			
			nameRules.addRule(RelativeLayout.POSITION_TO_LEFT, CHANNEL_COUNT);
			nameRules.addRule(RelativeLayout.POSITION_TO_RIGHT, CHANNEL_ICON);
			channel.addView(name, nameRules);
			
			setViewData(channel, cursor);
			
			return channel;
		}
    }
}