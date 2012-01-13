package com.androzic.overlay;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.preference.PreferenceManager;

import com.androzic.Androzic;
import com.androzic.MapView;
import com.androzic.R;
import com.androzic.data.Waypoint;

public class WaypointsOverlay extends MapOverlay
{
	Paint borderPaint;
	Paint fillPaint;
	Paint textPaint;
	Paint textFillPaint;
	List<Waypoint> waypoints;
	Map<Waypoint,Bitmap> bitmaps;
	
	// TODO replace with enabled
	boolean visible;
	int pointWidth;
	boolean showNames;
	
    public WaypointsOverlay(final Activity mapActivity)
    {
        super(mapActivity);
        visible = true;
        
        fillPaint = new Paint();
        fillPaint.setAntiAlias(false);
        fillPaint.setStrokeWidth(1);
        fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fillPaint.setColor(context.getResources().getColor(R.color.waypoint));
        borderPaint = new Paint();
        borderPaint.setAntiAlias(false);
        borderPaint.setStrokeWidth(1);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(context.getResources().getColor(R.color.waypointtext));
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStrokeWidth(2);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Align.LEFT);
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setColor(context.getResources().getColor(R.color.waypointtext));
        textFillPaint = new Paint();
        textFillPaint.setAntiAlias(false);
        textFillPaint.setStrokeWidth(1);
        textFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textFillPaint.setColor(context.getResources().getColor(R.color.waypointbg));

    	bitmaps = new WeakHashMap<Waypoint, Bitmap>();

    	onPreferencesChanged(PreferenceManager.getDefaultSharedPreferences(context));

		enabled = true;
    }

    public void setWaypoints(final List<Waypoint> wpt)
    {
    	waypoints = wpt;
    	clear();
    }
    
    public void clear()
    {
    	bitmaps.clear();
    }
    
    public void setVisible(final boolean v)
    {
    	visible = v;
    }
    
    public int waypointTapped(int x, int y, int centerX, int centerY)
    {
		Androzic application = (Androzic) context.getApplication();
		
        synchronized (waypoints)
        {
	        for (int i = waypoints.size()-1; i >= 0; i--)
	        {
	        	Waypoint wpt = waypoints.get(i);
	        	
   				int[] pointXY = application.getXYbyLatLon(wpt.latitude, wpt.longitude);

   				int screenX = pointXY[0] - centerX;
                int screenY = pointXY[1] - centerY;

   				int halfPointWidth = pointWidth/2 + 15;
   				final Rect pointBounds = new Rect(screenX - halfPointWidth, screenY - halfPointWidth, screenX + halfPointWidth, screenY + halfPointWidth);

   				if(pointBounds.contains(x, y))
   				{
   					return i;
   				}
   			}
   		}
        return -1;    	
    }
    
	public void onBeforeDestroy()
	{
		super.onBeforeDestroy();
		bitmaps.clear();
    }

	@Override
	protected void onDraw(final Canvas c, final MapView mapView)
	{
	}

	@Override
	protected void onDrawFinished(final Canvas c, final MapView mapView)
	{
		if (! visible)
			return;

		Androzic application = (Androzic) context.getApplication();

        synchronized (waypoints)
        {  
	        for (Waypoint wpt : waypoints)
	        {
	        	Bitmap bitmap = bitmaps.get(wpt);
	        	if (bitmap == null)
	        	{
	        		int width = pointWidth;
	        		int height = pointWidth;
	        		
	        		Bitmap icon = null;
	        		if (! "".equals(wpt.image) && application.iconsEnabled)
	        		{
	        			icon = BitmapFactory.decodeFile(application.iconPath + File.separator + wpt.image);
	        			if (icon == null)
	        			{
	        				wpt.drawImage = false;
	        			}
	        			else
	        			{
	        				width = icon.getWidth();
	        				height = icon.getHeight();
	        				wpt.drawImage = true;
	        			}
	        		}

	        		Rect rect = null;
	        		rect = new Rect(0, 0, width, height);

	        		Rect bounds = new Rect();
	        		
		            if (showNames)
		            {
		            	textPaint.getTextBounds(wpt.name, 0, wpt.name.length(), bounds);
			            bounds.right = bounds.right + 4;
			            bounds.bottom = bounds.bottom + 4;
		            	width += 6 + bounds.width();
		            	if (height < bounds.height())
		            		height = bounds.height();
		            }

	        		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	        		Canvas bc = new Canvas(bitmap);

	        		if (wpt.drawImage)
	        		{
			    		bc.drawBitmap(icon, 0, icon.getHeight() > bounds.height() ? 0 : (bounds.height() - icon.getHeight()) / 2, null);
	        		}
	        		else
	        		{
	        			int tc = 0, bgc = 0;
	        			if (wpt.textcolor != Integer.MIN_VALUE)
	        			{
	        				tc = borderPaint.getColor();
	        				borderPaint.setColor(wpt.textcolor);
	        			}
	        			if (wpt.backcolor != Integer.MIN_VALUE)
	        			{
	        				bgc = fillPaint.getColor();
	        				fillPaint.setColor(wpt.backcolor);
	        			}
	        			bc.save();
		        		bc.translate(0, pointWidth > bounds.height() ? 0 : (bounds.height() - pointWidth) / 2);
			            bc.drawRect(rect, borderPaint);
			            rect.inset(1, 1);
			            bc.drawRect(rect, fillPaint);
			            bc.restore();
	        			if (wpt.textcolor != Integer.MIN_VALUE)
	        			{
	        				borderPaint.setColor(tc);
	        			}
	        			if (wpt.backcolor != Integer.MIN_VALUE)
	        			{
	        				fillPaint.setColor(bgc);
	        			}
	        		}
		            
		            if (showNames)
		            {
	        			int tc = 0;
	        			if (wpt.textcolor != Integer.MIN_VALUE)
	        			{
	        				tc = textPaint.getColor();
	        				textPaint.setColor(wpt.textcolor);
	        			}
		        		bc.translate(width - bounds.right, -bounds.top + (height - bounds.height()) / 2);
			            bc.drawRect(bounds, textFillPaint);
		            	bc.drawText(wpt.name, 2, 2, textPaint);
	        			if (wpt.textcolor != Integer.MIN_VALUE)
	        			{
	        				textPaint.setColor(tc);
	        			}
		            }
		            bitmaps.put(wpt, bitmap);
	        	}
	            int[] xy = application.getXYbyLatLon(wpt.latitude,wpt.longitude);
	            int dx = wpt.drawImage ? application.iconX : pointWidth / 2;
	            int dy = wpt.drawImage ? application.iconY : bitmap.getHeight() / 2;
	        	c.drawBitmap(bitmap, xy[0] - dx, xy[1] - dy, null);
	        }
        }
	}

	@Override
	public void onPreferencesChanged(SharedPreferences settings)
	{
        pointWidth = settings.getInt(context.getString(R.string.pref_waypoint_width), context.getResources().getInteger(R.integer.def_waypoint_width));
        showNames = settings.getBoolean(context.getString(R.string.pref_waypoint_showname), true);
        fillPaint.setColor(settings.getInt(context.getString(R.string.pref_waypoint_color), context.getResources().getColor(R.color.waypoint)));
        int alpha = textFillPaint.getAlpha();
        textFillPaint.setColor(settings.getInt(context.getString(R.string.pref_waypoint_bgcolor), context.getResources().getColor(R.color.waypointbg)));
        textFillPaint.setAlpha(alpha);
        borderPaint.setColor(settings.getInt(context.getString(R.string.pref_waypoint_namecolor), context.getResources().getColor(R.color.waypointtext)));
        textPaint.setColor(settings.getInt(context.getString(R.string.pref_waypoint_namecolor), context.getResources().getColor(R.color.waypointtext)));
        textPaint.setTextSize(pointWidth * 1.5f);
       	clear();
	}

}