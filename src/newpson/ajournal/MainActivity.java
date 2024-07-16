package newpson.ajournal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import android.view.View;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.content.Intent;

import newpson.ajournal.WacomManager;
import newpson.ajournal.AJSurface;

public class MainActivity extends Activity implements View.OnClickListener
{
	private Handler dataHandler;
	private WacomManager wacomManager;
	private AJSurface surface;
	private HorizontalScrollView toolbar;
	private ImageButton fold;

	/* FIXME move from MainActivity */
	private final int wacomIds[][] = new int[][] {{1386, 890}};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		surface = (AJSurface) findViewById(R.id.surface);

		toolbar = (HorizontalScrollView) findViewById(R.id.toolbar);
		fold = (ImageButton) findViewById(R.id.button_fold);
		fold.setOnClickListener(this);

		dataHandler = new Handler(Looper.getMainLooper())
		{
			private int[] data;
			private int usage;
			private boolean drag = false;
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
						surface.move(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
						if (data[WacomManager.DATA_TIP] > 0)
						{
							if (drag)
							{
								surface.drag(data[WacomManager.DATA_X], data[WacomManager.DATA_Y], data[WacomManager.DATA_PRESSURE]);
							}
							else
							{
								drag = true;
								surface.dragStart(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
							}
						}
						else
						{
							if (drag)
							{
								drag = false;
								usage = surface.getMemoryUsage();
								surface.dragStop(data[WacomManager.DATA_X], data[WacomManager.DATA_Y]);
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
	}
}

