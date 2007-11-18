/*
 * $Id$
 */

package org.devtcg.rssreader;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.devtcg.rssprovider.RSSReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class RSSChannelList extends ListActivity {
	
	public static final int DELETE_ID = Menu.FIRST;
	public static final int INSERT_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	
	private Cursor mCursor;
	
    private static final String[] PROJECTION = new String[] {
    	RSSReader.Channels._ID, RSSReader.Channels.TITLE, RSSReader.Channels.URL };
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);

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
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch(item.getId())
    	{
    	case INSERT_ID:
    		startActivity(new Intent(Intent.INSERT_ACTION, getIntent().getData()));
    		return true;
    		
    	case DELETE_ID:
    		mCursor.moveTo(getSelection());
    		mCursor.deleteRow();
    		return true;
    		
    	case REFRESH_ID:
    		refreshChannel();
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    private final void refreshChannel()
    {
    	mCursor.moveTo(getSelection());    	
    	String rssurl = mCursor.getString(mCursor.getColumnIndex(RSSReader.Channels.URL));
    	
    	XMLReader xr;
		try {
			xr = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			Log.e("Test2", e.getMessage());
			return;
		}
		
    	RSSChannelRefresh test = new RSSChannelRefresh();
    	xr.setContentHandler(test);
    	xr.setErrorHandler(test);
    	
    	try
    	{
    		URL url = new URL(rssurl);
    		xr.parse(new InputSource(new InputStreamReader(url.openStream())));
    	}
    	catch (Exception e)
    	{
    		Log.e("Test", e.getMessage());
    	}
    	
    	Log.i("Test", rssurl);
    }
}