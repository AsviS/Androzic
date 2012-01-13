package com.androzic.route;

import com.androzic.Androzic;
import com.androzic.R;
import com.androzic.data.Route;
import com.androzic.data.Waypoint;
import com.androzic.waypoint.WaypointProperties;

import android.os.Bundle;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.ericharlow.DragNDrop.DragListener;
import com.ericharlow.DragNDrop.DragNDropAdapter;
import com.ericharlow.DragNDrop.DragNDropListView;
import com.ericharlow.DragNDrop.DropListener;
import com.ericharlow.DragNDrop.RemoveListener;

public class RouteEdit extends ListActivity implements DropListener, OnClickListener, RemoveListener, DragListener
{
	private Route route;
	private int index;

	private int backgroundColor = 0x00000000;
	private int defaultBackgroundColor;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_route_edit);

		index = getIntent().getExtras().getInt("INDEX");

		Androzic application = (Androzic) getApplication();
		route = application.getRoute(index);
		setTitle(route.name);

		ListView listView = getListView();

		if (listView instanceof DragNDropListView)
		{
			((DragNDropListView) listView).setDropListener(this);
			((DragNDropListView) listView).setRemoveListener(this);
			((DragNDropListView) listView).setDragListener(this);
		}

		findViewById(R.id.done_button).setOnClickListener(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		List<Waypoint> waypoints = route.getWaypoints();
		ArrayList<String> content = new ArrayList<String>(waypoints.size());
		for (int i = 0; i < waypoints.size(); i++)
		{
			content.add(waypoints.get(i).name);
		}
		setListAdapter(new DragNDropAdapter(this, new int[] { R.layout.dragitem }, new int[] { R.id.TextView01 }, content));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		startActivity(new Intent(this, WaypointProperties.class).putExtra("INDEX", position).putExtra("ROUTE", index+1));
	}
	
	@Override
	public void onClick(View v)
	{
		setResult(Activity.RESULT_OK);
		finish();
	}

	@Override
	public void onRemove(int which)
	{
		ListAdapter adapter = getListAdapter();
		if (adapter instanceof DragNDropAdapter)
		{
			((DragNDropAdapter) adapter).onRemove(which);
			route.removeWaypoint(route.getWaypoint(which));
			getListView().invalidateViews();
		}
	}

	@Override
	public void onDrop(int from, int to)
	{
		ListAdapter adapter = getListAdapter();
		if (adapter instanceof DragNDropAdapter)
		{
			((DragNDropAdapter) adapter).onDrop(from, to);
			Waypoint wpt = route.getWaypoint(from);
			route.removeWaypoint(wpt);
			route.addWaypoint(from < to ? to - 1 : to, wpt);
			getListView().invalidateViews();
		}
	}

	@Override
	public void onDrag(int x, int y, ListView listView)
	{
	}

	@Override
	public void onStartDrag(View itemView)
	{
		itemView.setVisibility(View.INVISIBLE);
		defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
		itemView.setBackgroundColor(backgroundColor);
	}

	@Override
	public void onStopDrag(View itemView)
	{
		itemView.setVisibility(View.VISIBLE);
		itemView.setBackgroundColor(defaultBackgroundColor);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		route = null;
	}
}