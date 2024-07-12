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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.content.Intent;

import newpson.ajournal.WacomManager;
import newpson.ajournal.WacomSurface;

import android.util.Log;
import java.lang.Math;

public class MainActivity extends Activity implements View.OnTouchListener, OnCheckedChangeListener
{
	private TextView log;
	private Button stop;
	private ToggleButton hide;
	private Handler dataHandler;
	// private WacomManager wacomManager;
	private WacomSurface surface;

	/* FIXME move from MainActivity */
	private final int wacomIds[][] = new int[][] {{1386, 890}};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		log = (TextView) findViewById(R.id.out_log);
		stop = (Button) findViewById(R.id.in_stop);
		hide = (ToggleButton) findViewById(R.id.in_hide);
//		stop.setOnClickListener(this);
		hide.setOnCheckedChangeListener(this);
		surface = (WacomSurface) findViewById(R.id.out_surface);
		surface.setOnTouchListener(this);

		/* FIXME move constants from MainActivity */

//		dataHandler = new Handler(Looper.getMainLooper())
//		{
//			private int[] data;
//			private int usage;
//			private boolean drag = false;
//			@Override
//			public void handleMessage(Message message)
//			{
//				switch (message.what)
//				{
//					case WacomManager.DATA:
//						data = (int[]) message.obj;
//						if (data[WacomManager.DATA_X] == 0 && data[WacomManager.DATA_Y] == 0)
//						{
//							break;
//						}
//						surface.move(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
//						if (data[WacomManager.DATA_TIP] > 0)
//						{
//							if (drag)
//							{
//								surface.drag(data[WacomManager.DATA_X], data[WacomManager.DATA_Y], data[WacomManager.DATA_PRESSURE]);
//							}
//							else
//							{
//								drag = true;
//								surface.dragStart(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
//							}
//						}
//						else
//						{
//							if (drag)
//							{
//								drag = false;
//								usage = surface.getMemoryUsage();
//								surface.dragStop(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
//								log.setText(String.format("Buffer memory usage: %d B (%d%%)", usage, usage*100/WacomSurface.MAX_MEMORY_RATE));
//							}
//						}
//						break;
//					case WacomManager.PERMISSION_GRANTED:
//						log.setText("Permission granted. Opening device...");
//						wacomManager.deviceAttach();
//						break;
//					case WacomManager.DEVICE_READY:
//						log.setText("Listening...");
//						wacomManager.listen();
//						break;
//					case WacomManager.DEVICE_BROKEN:
//						log.setText("Can't attach device.");
//						break;
//					case WacomManager.PERMISSION_DENIED:
//						log.setText("Device usage permission denied.");
//						break;
//					case WacomManager.DEVICE_CLOSED:
//						log.setText("Device closed.");
//						break;
//				}
//			}
//		};

		// Intent from = getIntent();
		// wacomManager = from.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED") ?
		// 	new WacomManager(this, dataHandler, (UsbDevice) from.getParcelableExtra(UsbManager.EXTRA_DEVICE)) :
		// 	new WacomManager(this, dataHandler, wacomIds);
	}

	// @Override
	// public void onClick(View v)
	// {
	// 	wacomManager.stop();
	// }

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
				log.setText(String.format("(x,y)=(%f,%f), d=%f", x, y, distance));
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

