/*
 * $Id$
 */

package org.devtcg.rssreader;

import org.devtcg.rssprovider.RSSReader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RSSPostListRow extends RelativeLayout
{
	private static final int SUBJECT_ID = 1;
	private static final int DATE_ID = 2;

	private TextView mSubject;
	private TextView mDate;
	
	private Rect mRect;
	private Paint mGray;

	public RSSPostListRow(Context context)
	{
		super(context);
		
		mRect = new Rect();
		mGray = new Paint();
		mGray.setStyle(Paint.Style.STROKE);
		mGray.setColor(0xff9c9e9c);
		
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		mSubject = new TextView(context);
		mSubject.setId(SUBJECT_ID);
		
		LayoutParams subjectRules =
		  new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		subjectRules.addRule(ALIGN_WITH_PARENT_LEFT);
		subjectRules.addRule(ALIGN_WITH_PARENT_TOP);
		
		addView(mSubject, subjectRules);

		mDate = new TextView(context);
		mDate.setId(DATE_ID);
		mDate.setTextColor(0xffaaaaaa);

		LayoutParams dateRules =
		  new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		dateRules.addRule(ALIGN_WITH_PARENT_RIGHT);
		//dateRules.addRule(POSITION_TO_RIGHT, SUBJECT_ID);

		addView(mDate, dateRules);
	}

	@Override
	protected void onLayout(boolean changed, int wl, int wt, int l, int t, int r, int b)
	{
		Log.d("RSSPostListView", "onLayout(" + changed + ", " + wl + ", " + wt + ", " + l + ", " + t + ", " + r + ", " + b + ")");
		
		//super.onLayout(changed, wl, wt, l, t, r, b);
		
		//mSubject.layout(wl, wt, l, t, r, b);
		
		assert(getChildCount() == 1);
		assert(getChildAt(0) == mSubject);
		
		mSubject.layout(mWindowLeft, mWindowTop, 0, 0, mSubject.getMeasuredWidth(), mSubject.getMeasuredHeight());
		mDate.layout(mWindowLeft, mWindowTop + mSubject.getLineHeight(), getMeasuredWidth() - mDate.getMeasuredWidth(), mSubject.getLineHeight(), getMeasuredWidth(), mDate.getMeasuredHeight() + mSubject.getLineHeight());
		
		//Log.d("RSSPostListView", "   width=" + mSubject.getMeasuredWidth());
		//Log.d("RSSPostListView", "   height=" + mSubject.getMeasuredHeight());
		
		//mDate.layout(wl + r - mDate.getMeasuredWidth(), wt + b - mDate.getMeasuredHeight(),
		//mDate.layout(wl + 10, wt + 10, l + 10, t + 10, r + 10, b + 10);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		Log.d("RSSPostListView", "onMeasure(" + widthMeasureSpec + ", " + heightMeasureSpec + ")");
		Log.d("RSSPostListView", "   width=" + View.MeasureSpec.toString(widthMeasureSpec));
		Log.d("RSSPostListView", "   height=" + View.MeasureSpec.toString(heightMeasureSpec));
		
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		/* TODO! */
		mSubject.measure(widthMeasureSpec, heightMeasureSpec);
		mDate.measure(getChildMeasureSpec(widthMeasureSpec, 0, mDate.getLayoutParams().width), getChildMeasureSpec(heightMeasureSpec, 0, mDate.getLayoutParams().height));
		setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), 39);
		
		Log.d("RSSPostListView", "   newW=" + getMeasuredWidth());
		Log.d("RSSPostListView", "   newH=" + getMeasuredHeight());
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		Rect r = mRect;

		getDrawingRect(r);
		canvas.drawLine(r.left, r.bottom - 1, r.right, r.bottom - 1, mGray);
		
		super.dispatchDraw(canvas);
	}

	public void bindView(Cursor cursor)
	{
		if (cursor.getInt(cursor.getColumnIndex(RSSReader.Posts.READ)) != 0)
			mSubject.setTypeface(Typeface.DEFAULT);
		else
			mSubject.setTypeface(Typeface.DEFAULT_BOLD);

		mSubject.setText(cursor, cursor.getColumnIndex(RSSReader.Posts.TITLE));

		/* TODO */
		mDate.setText("11/25/2007 11:34AM");
	}
}
