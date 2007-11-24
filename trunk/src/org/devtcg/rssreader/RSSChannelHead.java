package org.devtcg.rssreader;

import java.util.Map;

import org.devtcg.rssprovider.RSSReader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * Simple widget that draws the channel heading for RSSPostView and 
 * RSSPostList.  Will optionally show either the channel logo, or the
 * channel icon + channel text.
 * 
 * TODO: I have no idea what I'm doing here.  The abstraction for
 * RSSChannelHead seems correct, but the implementation is surely all wrong.
 */
public class RSSChannelHead extends LinearLayout
{
	private ImageView mLogo;
	private ImageView mIcon;
	private TextView mLogoText;

	private Rect mRect;
	private Paint mGray;
	private Paint mBlack1;
	private Paint mBlack2;
	
	/* Default padding for each widget. */
	private static final int paddingTop = 4;
	private static final int paddingBottom = 6;
	
	public RSSChannelHead(Context context, AttributeSet attrs, Map inflateParams)
	{
		super(context, attrs, inflateParams);
		
		mRect = new Rect();
		mGray = new Paint();		
		mGray.setStyle(Paint.Style.STROKE);
		mGray.setColor(0xff9c9e9c);
		
		mBlack1 = new Paint();		
		mBlack1.setStyle(Paint.Style.STROKE);
		mBlack1.setColor(0xbb000000);

		mBlack2 = new Paint();		
		mBlack2.setStyle(Paint.Style.STROKE);
		mBlack2.setColor(0x33000000);
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		Log.d("RSSChannelHead", "dispatchDraw");

		Rect r = mRect;

		getDrawingRect(r);
		canvas.drawLine(r.left, r.bottom - 4, r.right, r.bottom - 4, mGray);
		canvas.drawLine(r.left, r.bottom - 3, r.right, r.bottom - 3, mGray);
		canvas.drawLine(r.left, r.bottom - 2, r.right, r.bottom - 2, mBlack1);
		canvas.drawLine(r.left, r.bottom - 1, r.right, r.bottom - 1, mBlack2);

		super.dispatchDraw(canvas);
	}
	
	public void setLogo(String channelName, String iconData, String logoData)
	{
		/* TODO */
		assert(logoData == null);
		
		if (mIcon == null)
		{
			mIcon = new ImageView(getContext());
			mIcon.setPadding(8, paddingTop, 0, paddingBottom);
			
			LayoutParams iconRules =
			  new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			
			iconRules.gravity = Gravity.CENTER_VERTICAL;
			
			addView(mIcon, iconRules);
		}
		
		if (iconData != null)
		{
			byte[] raw = iconData.getBytes();

			mIcon.setImageBitmap
			  (BitmapFactory.decodeByteArray(raw, 0, raw.length));
		}
		else
		{			
			mIcon.setImageResource(R.drawable.feedicon);
		}
		
		if (mLogoText == null)
		{
			mLogoText = new TextView(getContext());
			mLogoText.setPadding(3, paddingTop, 0, paddingBottom);
			mLogoText.setTypeface(Typeface.DEFAULT_BOLD);
			addView(mLogoText, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));			
		}

		mLogoText.setText(channelName);
	}
	
	/* Convenience method to access the logo data from a Channel cursor. */
	public void setLogo(Cursor cursor)
	{
		setLogo(cursor.getString(cursor.getColumnIndex(RSSReader.Channels.TITLE)),
		  cursor.getString(cursor.getColumnIndex(RSSReader.Channels.ICON)),
		  cursor.getString(cursor.getColumnIndex(RSSReader.Channels.LOGO)));
	}
}
