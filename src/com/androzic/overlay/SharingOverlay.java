package com.androzic.overlay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.androzic.Androzic;
import com.androzic.MapActivity;
import com.androzic.MapView;
import com.androzic.R;
import com.androzic.data.Situation;
import com.androzic.location.ILocationCallback;
import com.androzic.location.ILocationRemoteService;

public class SharingOverlay extends MapOverlay
{
	Paint linePaint;
	Paint textPaint;
	Paint textFillPaint;
	Map<String, Situation> situations;

	int pointWidth;

	private ILocationRemoteService locationService = null;
	private boolean isBound = false;
	protected ExecutorService executorThread = Executors.newSingleThreadExecutor();

	private String session;
	private String user;
	private int updateInterval = 10000; // 10 seconds (default)
	private long lastShareTime = 0;
	private int timeoutInterval = 600000; // 10 minutes (default)
	private double speedFactor = 1;

	public SharingOverlay(final Activity activity)
	{
		super(activity);

		linePaint = new Paint();
		linePaint.setAntiAlias(false);
		linePaint.setStrokeWidth(2);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setColor(context.getResources().getColor(R.color.usertag));
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setStrokeWidth(2);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTextAlign(Align.LEFT);
		textPaint.setTextSize(20);
		textPaint.setTypeface(Typeface.SANS_SERIF);
		textPaint.setColor(context.getResources().getColor(R.color.usertag));
        textFillPaint = new Paint();
        textFillPaint.setAntiAlias(false);
        textFillPaint.setStrokeWidth(1);
        textFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textFillPaint.setColor(context.getResources().getColor(R.color.usertagwithalpha));

		situations = new WeakHashMap<String, Situation>();

		onPreferencesChanged(PreferenceManager.getDefaultSharedPreferences(context));

		enabled = true;
	}

	public void setMapContext(final Activity activity)
	{
		super.setMapContext(activity);
		if (isBound)
		{
			unbind();
		}
		isBound = context.bindService(new Intent(ILocationRemoteService.class.getName()), connection, 0);
		if (isBound)
		{
			Androzic application = (Androzic) context.getApplication();
			updateSituation(application.getLocationAsLocation());
		}
	}

	public void setIdentity(String session, String user)
	{
		this.session = session;
		this.user = user;
	}

	@Override
	public void onMapChanged()
	{
	}

	@Override
	protected void onDraw(Canvas c, MapView mapView)
	{
		Androzic application = (Androzic) context.getApplication();

		final int half = Math.round(pointWidth / 2);

		synchronized (situations)
		{
			for (Situation loc : situations.values())
			{
				linePaint.setAlpha(loc.silent ? 128 : 255);
				textPaint.setAlpha(loc.silent ? 128 : 255);
				int[] xy = application.getXYbyLatLon(loc.latitude, loc.longitude);
	    		Rect rect = new Rect(xy[0]-half, xy[1]-half, xy[0]+half, xy[1]+half);
	            c.drawRect(rect, linePaint);
	            c.drawLine(xy[0], xy[1], xy[0]+pointWidth*3, xy[1]-pointWidth*3, linePaint);
            	String tag = String.valueOf(Math.round(loc.speed * speedFactor)) + "  " + String.valueOf(Math.round(loc.track));
            	textPaint.getTextBounds(tag, 0, tag.length(), rect);
            	Rect rect1 = new Rect();
            	textPaint.getTextBounds(loc.name, 0, loc.name.length(), rect1);
            	rect.union(rect1);
            	int height = rect.height();
	            rect.inset(0, -(height+3)/2);
	            rect.inset(-2, -2);
	            rect.offsetTo(xy[0]+pointWidth*3+3, xy[1]-pointWidth*3-height*2-5);
	            c.drawRect(rect, textFillPaint);
            	c.drawText(tag, xy[0]+pointWidth*3+5, xy[1]-pointWidth*3, textPaint);
            	c.drawText(loc.name, xy[0]+pointWidth*3+5, xy[1]-pointWidth*3-height-3, textPaint);
        		c.save();
        		c.rotate((float) loc.track, xy[0], xy[1]);
        		c.drawLine(xy[0], xy[1], xy[0], xy[1]-pointWidth*2, linePaint);
        		c.restore();
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, MapView mapView)
	{
	}

	public void onBeforeDestroy()
	{
		super.onBeforeDestroy();
		unbind();
	}

	private void unbind()
	{
		if (isBound)
		{
			if (locationService != null)
			{
				try
				{
					locationService.unregisterCallback(callback);
				}
				catch (RemoteException e)
				{
				}
			}

			try
			{
				context.unbindService(connection);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			isBound = false;
		}
	}

	@Override
	public void onPreferencesChanged(SharedPreferences settings)
	{
		String oldsession = session;
		String olduser = user;
        session = settings.getString(context.getString(R.string.pref_sharing_session), "");
        user = settings.getString(context.getString(R.string.pref_sharing_user), "");
        if (! session.equals(oldsession) || ! user.equals(olduser))
        {
        	situations.clear();
        }
        //FIXME should halt if any string is empty
        updateInterval = settings.getInt(context.getString(R.string.pref_sharing_updateinterval), context.getResources().getInteger(R.integer.def_sharing_updateinterval)) * 1000;
        linePaint.setColor(settings.getInt(context.getString(R.string.pref_sharing_tagcolor), context.getResources().getColor(R.color.usertag)));
        textPaint.setColor(settings.getInt(context.getString(R.string.pref_sharing_tagcolor), context.getResources().getColor(R.color.usertag)));
        pointWidth = settings.getInt(context.getString(R.string.pref_sharing_tagwidth), context.getResources().getInteger(R.integer.def_sharing_tagwidth));
        timeoutInterval = settings.getInt(context.getString(R.string.pref_sharing_timeout), context.getResources().getInteger(R.integer.def_sharing_timeout)) * 60000;

		int speedIdx = Integer.parseInt(settings.getString(context.getString(R.string.pref_unitspeed), "0"));
		speedFactor = Double.parseDouble(context.getResources().getStringArray(R.array.speed_factors)[speedIdx]);
	}

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			locationService = ILocationRemoteService.Stub.asInterface(service);

			try
			{
				locationService.registerCallback(callback);
			}
			catch (RemoteException e)
			{
			}
		}

		public void onServiceDisconnected(ComponentName className)
		{
			locationService = null;
		}
	};
	
	protected void updateSituation(final Location loc)
	{
		executorThread.execute(new Runnable() {
			public void run()
			{
				URI URL;
				try
				{
					
					String query = "session=" + URLEncoder.encode(session) + ";user=" + URLEncoder.encode(user) + ";lat=" + loc.getLatitude() + ";lon=" + loc.getLongitude() + ";track=" + loc.getBearing() + ";speed=" + loc.getSpeed() + ";ftime=" + loc.getTime();
					URL = new URI("http", null, "androzic.com", 80, "/cgi-bin/loc.cgi", query, null);

					HttpClient httpclient = new DefaultHttpClient();
					HttpResponse response = httpclient.execute(new HttpGet(URL));
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() == HttpStatus.SC_OK)
					{
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						response.getEntity().writeTo(out);
						out.close();
						String responseString = out.toString();
						JSONObject situation = new JSONObject(responseString);
						JSONArray entries = situation.getJSONArray("users");
						
						for (int i = 0; i < entries.length(); i++)
						{
							JSONObject location = entries.getJSONObject(i);
							String name = location.getString("user");
							if (name.equals(user))
								continue;
							synchronized (situations)
							{
								Situation s = situations.get(name);
								if (s == null)
								{
									s = new Situation(name);
									situations.put(name, s);
								}
								s.latitude = location.getDouble("lat");
								s.longitude = location.getDouble("lon");
								s.speed = location.getDouble("speed");
								s.track = location.getDouble("track");
								s.silent = location.getLong("ftime") + timeoutInterval < loc.getTime();
							}
						}
						((MapActivity) context).updateMap();
						lastShareTime = loc.getTime();
					}
					else
					{
						response.getEntity().getContent().close();
						throw new IOException(statusLine.getReasonPhrase());
					}
				}
				catch (URISyntaxException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ClientProtocolException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private ILocationCallback callback = new ILocationCallback.Stub()
	{
		@Override
		public void onGpsStatusChanged(String provider, int status, int fsats, int tsats) throws RemoteException
		{
		}

		@Override
		public void onLocationChanged(Location loc, boolean continous, float smoothspeed, float avgspeed) throws RemoteException
		{
			if (loc.getTime() - lastShareTime > updateInterval)
			{
				updateSituation(loc);
			}
		}

		@Override
		public void onProviderChanged(String provider) throws RemoteException
		{
		}

		@Override
		public void onProviderDisabled(String provider) throws RemoteException
		{
		}

		@Override
		public void onProviderEnabled(String provider) throws RemoteException
		{
		}

		@Override
		public void onSensorChanged(float azimuth, float pitch, float roll) throws RemoteException
		{
		}
	};
}