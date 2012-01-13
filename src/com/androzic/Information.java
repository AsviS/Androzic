package com.androzic;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.androzic.location.ILocationCallback;
import com.androzic.location.ILocationRemoteService;
import com.androzic.location.LocationService;
import com.androzic.util.Astro;
import com.androzic.util.StringFormatter;

public class Information extends Activity
{
	private ILocationRemoteService locationService = null;
	private TextView satsValue;
	private TextView lastfixValue;
	private TextView providerValue;
	private TextView latitudeValue;
	private TextView longitudeValue;
	private TextView accuracyValue;
	private TextView sunriseValue;
	private TextView sunsetValue;
	private TextView declinationValue;

	protected Androzic application;

	protected Animation shake;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_information);

		application = (Androzic) getApplication();
        shake = AnimationUtils.loadAnimation(Information.this, R.anim.shake);

		satsValue = (TextView) findViewById(R.id.sats);
    	lastfixValue = (TextView) findViewById(R.id.lastfix);
    	accuracyValue = (TextView) findViewById(R.id.accuracy);
    	providerValue = (TextView) findViewById(R.id.provider);
    	latitudeValue = (TextView) findViewById(R.id.latitude);
    	longitudeValue = (TextView) findViewById(R.id.longitude);
    	sunriseValue = (TextView) findViewById(R.id.sunrise);
    	sunsetValue = (TextView) findViewById(R.id.sunset);
    	declinationValue = (TextView) findViewById(R.id.declination);

	    Button update = (Button) findViewById(R.id.almanac_button);
	    update.setOnClickListener(updateOnClickListener);

		bindService(new Intent(ILocationRemoteService.class.getName()), locationConnection, BIND_AUTO_CREATE);
    }
    
	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		if (locationService != null)
		{
			try
			{
				locationService.unregisterCallback(locationCallback);
			}
			catch (RemoteException e)
			{
			}
			locationService = null;
		}
		unbindService(locationConnection);

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menuGPS:
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				return true;
			case R.id.menuPreferences:
				startActivity(new Intent(this, Preferences.class));
				return true;
		}
		return false;
	}

	private ServiceConnection locationConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			locationService = ILocationRemoteService.Stub.asInterface(service);

			try
			{
				locationService.registerCallback(locationCallback);
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

	private ILocationCallback locationCallback = new ILocationCallback.Stub()
	{

		@Override
		public void onGpsStatusChanged(final String provider, final int status, final int fsats, final int tsats) throws RemoteException
		{
			runOnUiThread(new Runnable() {
				public void run()
				{
					switch (status)
					{
						case LocationService.GPS_OK:
					    	satsValue.setText(String.valueOf(fsats) + "/" + String.valueOf(tsats));
							break;
						case LocationService.GPS_OFF:
							satsValue.setText(R.string.sat_stop);
							break;
						case LocationService.GPS_SEARCHING:
							satsValue.setText(String.valueOf(fsats) + "/" + String.valueOf(tsats));
							satsValue.startAnimation(shake);
							break;
					}
				}
			});
		}

		@Override
		public void onLocationChanged(final Location loc, boolean continous, float smoothspeed, float avgspeed) throws RemoteException
		{
			runOnUiThread(new Runnable() {
				public void run()
				{
			    	lastfixValue.setText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM).format(new Date(loc.getTime())));
			    	providerValue.setText(loc.getProvider() != null ? loc.getProvider() : "N/A");
			    	// FIXME Needs UTM support here
			    	latitudeValue.setText(StringFormatter.coordinate(application.coordinateFormat, loc.getLatitude()));
			    	longitudeValue.setText(StringFormatter.coordinate(application.coordinateFormat, loc.getLongitude()));
			    	accuracyValue.setText(loc.hasAccuracy() ? StringFormatter.distanceH(loc.getAccuracy(), "%.1f", 1000) : "N/A");

					Calendar now = GregorianCalendar.getInstance(TimeZone.getDefault());
					double sunrise = Astro.computeSunriseTime(application.getZenith(), loc, now);
					double sunset = Astro.computeSunsetTime(application.getZenith(), loc, now);

					if (sunrise == Double.NaN)
					{
						sunriseValue.setText(R.string.never);
					}
					else
					{
						sunriseValue.setText(Astro.getLocalTimeAsString(sunrise));
					}
					if (sunset == Double.NaN)
					{
						sunsetValue.setText(R.string.never);
					}
					else
					{
						sunsetValue.setText(Astro.getLocalTimeAsString(sunset));
					}
					double declination = application.getDeclination();
					declinationValue.setText(String.format("%+.1f�", declination));
				}
			});
		}

		@Override
		public void onProviderChanged(String provider) throws RemoteException
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) throws RemoteException
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) throws RemoteException
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(final float azimuth, final float pitch, final float roll) throws RemoteException
		{
		}
	};
	
	private OnClickListener updateOnClickListener = new OnClickListener()
	{
        public void onClick(View v)
        {
    		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    		if (locationManager != null)
    		{
    			locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", null);
    			locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", null);
    		}        		
        }
    };
}