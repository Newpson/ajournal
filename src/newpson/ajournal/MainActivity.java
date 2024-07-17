package newpson.ajournal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.content.Intent;

import newpson.ajournal.WacomManager;
import newpson.ajournal.AJSurface;
import android.util.Log;

public class MainActivity extends Activity implements View.OnClickListener
{
	private Handler dataHandler;
	private WacomManager wacomManager;
	private AJSurface surface;
	private HorizontalScrollView toolbar;
	private ImageButton buttonFold;
	private ImageButton buttonPageClear;
	private Button buttonColor;

	/* FIXME move from MainActivity */
	private final int wacomIds[][] = new int[][] {{1386, 890}};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		surface = (AJSurface) findViewById(R.id.surface);

		toolbar = (HorizontalScrollView) findViewById(R.id.toolbar);
		buttonFold = (ImageButton) findViewById(R.id.button_fold);
		buttonFold.setOnClickListener(this);

		buttonPageClear = (ImageButton) findViewById(R.id.button_pageClear);
		buttonPageClear.setOnClickListener(this);

		buttonColor = (Button) findViewById(R.id.button_color);
		buttonColor.setOnClickListener(this);

		dataHandler = new Handler(Looper.getMainLooper())
		{
			private int[] data;
			private boolean drag = false;
			float x;
			float y;
			float p;
			@Override
			public void handleMessage(Message message)
			{
				switch (message.what)
				{
					case WacomManager.DATA:
						data = (int[]) message.obj;
						if (data[WacomManager.DATA_X] == 0 && data[WacomManager.DATA_Y] == 0)
						{
							break;
						}
						x = data[WacomManager.DATA_X] / 15200.0f * surface.getWidth();
						y = data[WacomManager.DATA_Y] / 9500.0f * surface.getHeight();
						p = (float) data[WacomManager.DATA_PRESSURE] / 2048.0f * 10.0f;
						surface.move(x, y);
						if (data[WacomManager.DATA_TIP] > 0)
						{
							if (drag)
							{
								surface.drag(x, y, p);
							}
							else
							{
								drag = true;
								surface.dragStart(x, y);
							}
						}
						else
						{
							if (drag)
							{
								drag = false;
								surface.dragStop(x, y);
							}
						}
						break;
					case WacomManager.PERMISSION_GRANTED:
						wacomManager.deviceAttach();
						break;
					case WacomManager.DEVICE_READY:
						wacomManager.listen();
						break;
					case WacomManager.DEVICE_BROKEN:
						break;
					case WacomManager.PERMISSION_DENIED:
						break;
					case WacomManager.DEVICE_CLOSED:
						break;

				}
			}
		};

		Intent from = getIntent();
		wacomManager = from.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED") ?
			new WacomManager(this, dataHandler, (UsbDevice) from.getParcelableExtra(UsbManager.EXTRA_DEVICE)) :
			new WacomManager(this, dataHandler, wacomIds);
	}

	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		if (id == R.id.button_fold)
		{
			toolbar.setVisibility(toolbar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
		}
		else if (id == R.id.button_pageClear)
		{
			surface.clear();
		}
		else if (id == R.id.button_color)
		{
			PopupMenu menu = new PopupMenu(this, buttonColor);
			menu.getMenuInflater().inflate(R.menu.popup_colors, menu.getMenu());
			menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem menuItem)
				{
					/* bruh code */
					int id2 = menuItem.getItemId();
					if (id2 == R.id.popup_color_blue)
					{
						surface.setColor(AJSurface.COLOR_BLUE);
					}
					else if (id2 == R.id.popup_color_orange)
					{
						surface.setColor(AJSurface.COLOR_ORANGE);
					}
					else if (id2 == R.id.popup_color_red)
					{
						surface.setColor(AJSurface.COLOR_RED);
					}
					else if (id2 == R.id.popup_color_cyan)
					{
						surface.setColor(AJSurface.COLOR_CYAN);
					}
					else if (id2 == R.id.popup_color_green)
					{
						surface.setColor(AJSurface.COLOR_GREEN);
					}
					else if (id2 == R.id.popup_color_yellow)
					{
						surface.setColor(AJSurface.COLOR_YELLOW);
					}
					else if (id2 == R.id.popup_color_purple)
					{
						surface.setColor(AJSurface.COLOR_PURPLE);
					}
					else if (id2 == R.id.popup_color_pink)
					{
						surface.setColor(AJSurface.COLOR_PINK);
					}
					else if (id2 == R.id.popup_color_brown)
					{
						surface.setColor(AJSurface.COLOR_BROWN);
					}
					else if (id2 == R.id.popup_color_gray)
					{
						surface.setColor(AJSurface.COLOR_GRAY);
					}
					return true;
				}
			});

			menu.show();
		}
	}
}

