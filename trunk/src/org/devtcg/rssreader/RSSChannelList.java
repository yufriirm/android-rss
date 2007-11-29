/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.util.HashMap;

import org.devtcg.rssprovider.RSSReader;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;

public class RSSChannelList extends ListActivity
{
	public static final String TAG = "RSSChannelList";
	public static final String TAG_PREFS = "RSSReader";
	
	public static final int DELETE_ID = Menu.FIRST;
	public static final int INSERT_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	public static final int REFRESH_ALL_ID = Menu.FIRST + 3;
	
	private Cursor mCursor;
	
//	private boolean mFirstTime;
	
//	private IRSSReaderService mService;
	
	private final Handler mRefreshHandler = new Handler();
	
    private static final String[] PROJECTION = new String[] {
      RSSReader.Channels._ID, RSSReader.Channels.ICON,
      RSSReader.Channels.TITLE, RSSReader.Channels.URL };
    
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);

        if (intent.getAction() == null)
        	intent.setAction(Intent.VIEW_ACTION);

        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);

//        bindService(new Intent(this, RSSReaderService.class),
//          null, mServiceConn, Context.BIND_AUTO_CREATE);
        
//        SharedPreferences settings = getSharedPreferences(TAG_PREFS, 0);
//        mFirstTime = settings.getBoolean("firstTime", true);
        
        /*
         * If this is the first time the user has opened our app ever, 
         * install the RSSReaderService_Alarm as has been scheduled for
         * next device reboot.  After `firstTime`, RSSReaderService_Setup
         * will handle our setup at BOOT_COMPLETED.
         * 
         * TODO: There must be a more appropriate way to handle this?
         */
//        if (mFirstTime == true)
//        	RSSReaderService_Setup.setupAlarm(this);

        ListAdapter adapter = new RSSChannelListAdapter(mCursor, this);     
        setListAdapter(adapter);
    }
    
    @Override
    protected void onStop()
    {
    	super.onStop();
    	
//    	if (mFirstTime == true)
//    	{
//    		SharedPreferences settings = getSharedPreferences(TAG_PREFS, 0);
//    		SharedPreferences.Editor editor = settings.edit();
//    		editor.putBoolean("firstTime", false);
//    		editor.commit();
//    	}
    }

//    private ServiceConnection mServiceConn = new ServiceConnection()
//    {
//    	public void onServiceConnected(ComponentName className, IBinder service)
//    	{
//    		Log.d(TAG, "onServiceConnected");
//    		mService = IRSSReaderService.Stub.asInterface((IBinder)service);
//    	}
//
//    	public void onServiceDisconnected(ComponentName className)
//    	{
//    		Log.d(TAG, "onServiceDisconnected");
//    		mService = null;
//    	}
//    };

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

    		menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);

    		menu.add(Menu.SELECTED_ALTERNATIVE, REFRESH_ALL_ID, "Refresh All");
  		
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
        	mCursor.moveTo(getSelection());    	        	
    		refreshChannel();
    		return true;
    		
    	case REFRESH_ALL_ID:
    		refreshAllChannels();
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

    private final void refreshAllChannels()
    {
    	if (mCursor.first() == false)
    		return;

    	do {
    		refreshChannel();
    	} while (mCursor.next() == true);
    }

    /* This method assumes that `mCursor` has been positioned on the record
     * we want to refresh. */
    private final void refreshChannel()
    {
    	String rssurl = mCursor.getString(mCursor.getColumnIndex(RSSReader.Channels.URL));

    	long channelId =
    	  mCursor.getInt(mCursor.getColumnIndex(RSSReader.Channels._ID));

    	/* TODO: Is there a generalization of getListView().getSelectedView() we can use here?
    	 * http://groups.google.com/group/android-developers/browse_thread/thread/4070126fd996001c */
    	RSSChannelListRow row =
    	  ((RSSChannelListAdapter)getListAdapter()).getViewByRowID(channelId);
    	
    	assert(row != null);
    	
		Runnable refresh = new RefreshRunnable(mRefreshHandler, row, channelId, rssurl); 

    	(new Thread(refresh)).start();
    }
    
    private static class RSSChannelListAdapter extends CursorAdapter implements Filterable
    {
    	/* TODO: Android should provide a way to look up a View by row, but
    	 * it does not currently.  Hopefully this will be fixed in future
    	 * releases. */
    	private HashMap<Long, RSSChannelListRow> rowMap;

		public RSSChannelListAdapter(Cursor c, Context context)
		{
			super(c, context);
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
			row.bindView(cursor);
			updateRowMap(cursor, row);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			RSSChannelListRow row = new RSSChannelListRow(context);
			row.bindView(cursor);
			updateRowMap(cursor, row);
			return row;
		}
		
		public RSSChannelListRow getViewByRowID(long id)
		{
			return rowMap.get(new Long(id));
		}
    }
    
    private class RefreshRunnable implements Runnable
    {
    	private Handler mHandler;
    	private RSSChannelListRow mRow;
    	private long mChannelID;
    	private String mRSSURL;

    	public RefreshRunnable(Handler handler, RSSChannelListRow row, long channelId, String rssurl)
    	{
    		mHandler = handler;
    		mRow = row;
    		mChannelID = channelId;
    		mRSSURL = rssurl;
    	}

    	public void run()
    	{
			Log.e("RSSChannelList", "Here we go: " + mRSSURL + "...");
			
			mHandler.post(new Runnable() {
				public void run()
				{
					mRow.startRefresh();
				}
			});
			
	    	new RSSChannelRefresh(getContentResolver()).
	    	  syncDB(mHandler, mChannelID, mRSSURL);
	    	
	    	mHandler.post(new Runnable() {
	    		public void run()
	    		{
	    			mRow.finishRefresh();
	    		}
	    	});
    	}
    }
}