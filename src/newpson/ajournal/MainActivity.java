package newpson.ajournal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import android.view.View;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import android.widget.HorizontalScrollView;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.content.Intent;

import newpson.ajournal.WacomManager;
import newpson.ajournal.WacomSurface;

import android.util.Log;
import java.lang.Math;

public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener, OnCheckedChangeListener
{
	private Handler dataHandler;
	// private WacomManager wacomManager;
	private ImageButton fold;
	private HorizontalScrollView toolbar;
	private WacomSurface surface;


	/* FIXME move from MainActivity */
	private final int wacomIds[][] = new int[][] {{1386, 890}};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		stop.setOnClickListener(this);
		surface = (WacomSurface) findViewById(R.id.wacomSurface);
		surface.setOnTouchListener(this);
		toolbar = (HorizontalScrollView) findViewById(R.id.toolbar);
		fold = (ImageButton) findViewById(R.id.button_fold);
		fold.setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		if (id == R.id.button_fold)
		{
			toolbar.setVisibility(toolbar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
		}
	}

	private float last_x = 0;
	private float last_y = 0;
	private int usage;
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		float x = (2.0f*event.getX() / (float)surface.getWidth()) - 1.0f;
		float y = 1.0f - (2.0f*event.getY() / (float)surface.getHeight());
		float distance = (x-last_x)*(x-last_x)+(y-last_y)*(y-last_y);
		if (distance > 0.1f) distance = 0.1f;
		if (distance < 0.01f) distance = 0.01f;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				surface.move(x, y);
				surface.dragStart(x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				surface.move(x, y);
				/* use drag speed (distance) as pressure value */
				surface.drag(x, y, distance);
				// log.setText(String.format("(x,y)=(%f,%f), d=%f", x, y, distance));
				break;
			case MotionEvent.ACTION_UP:
				surface.move(x, y);
				// usage = surface.getMemoryUsage();
				surface.dragStop(x, y);
				// log.setText(String.format("%dx%d, Buffer memory usage: %d B (%d%%)", surface.getWidth(), surface.getHeight(), usage, usage*100/WacomSurface.MAX_MEMORY_RATE));
				break;
		}

		last_x = x;
		last_y = y;
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton button, boolean checked)
	{
		surface.hideCursor(checked);
	}
}

