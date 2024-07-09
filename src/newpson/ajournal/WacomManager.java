package newpson.ajournal;

import android.os.Handler;
import android.os.Message;

import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.hardware.usb.*;
import java.util.Iterator;

import android.util.Log;

public class WacomManager
{
	private static final int TABLET_VID = 0x056a;
	private static final int TABLET_PID = 0x037a;
	private static final float TABLET_WIDTH = 15200.0f;
	private static final float TABLET_HEIGHT = 9500.0f;
	private static final float TABLET_SENSITIVITY = 2048.0f;

	private UsbManager manager;
	private UsbDevice wacomDevice;
	private UsbDeviceConnection wacomConnection;
	private UsbInterface wacomInterface;
	private UsbEndpoint wacomEndpoint;

	private static final String ACTION_PERMISSION = "newpson.ajournal.WACOM_USB_PERMISSION";

	private int status;
	public static final int DATA = 0;
	public static final int PERMISSION_GRANTED = 1;
	public static final int PERMISSION_DENIED = 2;
	public static final int DEVICE_READY = 3;
	public static final int DEVICE_LISTENING = 4;
	public static final int DEVICE_BROKEN = 5;
	public static final int DEVICE_CLOSED = 6;

	/* See https://github.com/linuxwacom/input-wacom/tree/master/3.7 for details */
	private static final int WAC_HID_FEATURE_REPORT = 0x03;
	private static final int USB_REQ_SET_REPORT = 0x09;
	private static final int WACOM_PACKAGE_SIZE = 9;

	/* array indicies */
	public static final int USB_VID = 0;
	public static final int USB_PID = 1;
	public static final int DATA_X = 0;
	public static final int DATA_Y = 1;
	public static final int DATA_PRESSURE = 2;
	public static final int DATA_TIP = 3;
	public static final int DATA_BUTTON = 4;
	public static final int BUTTON_1 = 0b01;
	public static final int BUTTON_2 = 0b10;

	private Handler handler;
	private UsbListener usbListener;

	private Context context;
	private Message message;

	private WacomManager(Context context, Handler handler)
	{
		this.context = context;
		this.handler = handler;
		this.manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		message = new Message();
	}

	public WacomManager(Context context, Handler handler, int[][] ids)
	{
		this(context, handler);
		this.wacomDevice = findWacomDevice(ids);
		askPermission();
	}

	public WacomManager(Context context, Handler handler, UsbDevice wacomDevice)
	{
		this(context, handler);
		this.wacomDevice = wacomDevice;
		setStatus(PERMISSION_GRANTED);
	}

	private void askPermission()
	{
		Log.d("AJDBG", "Asking permission");
		if (wacomDevice != null)
		{
			Log.d("AJDBG", "wacomDevice is nonnull");
			PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PERMISSION), 0);
			PermissionReceiver receiver = new PermissionReceiver(handler);
			context.registerReceiver(receiver, new IntentFilter(ACTION_PERMISSION));
			manager.requestPermission(wacomDevice, intent);
		}
	}
	
	private void setStatus(int status)
	{
		this.status = status;
		handler.sendMessage(message.obtain(handler, status));
	}

	public int getStatus()
	{
		return status;
	}

	private UsbDevice findWacomDevice(int[][] ids)
	{
		Iterator<UsbDevice> deviceList = manager.getDeviceList().values().iterator();
		while (deviceList.hasNext())
		{
			wacomDevice = deviceList.next();
			for (int i = 0; i < ids.length; ++i)
			{
				if (wacomDevice.getVendorId() == ids[i][USB_VID] && wacomDevice.getProductId() == ids[i][USB_PID])
				{
					return wacomDevice;
				}
			}
		}
		return null;
	}

	public void deviceAttach()
	{
		wacomConnection = manager.openDevice(wacomDevice);
		if (wacomConnection != null)
		{
			wacomInterface = wacomDevice.getInterface(0);
			wacomEndpoint = wacomInterface.getEndpoint(0);
			wacomConnection.claimInterface(wacomInterface, true);
			usbListener = new WacomManager.UsbListener();
			setStatus(DEVICE_READY);
		}
		else
		{
			setStatus(DEVICE_BROKEN);
		}
	}

	public void listen()
	{
		if (status == DEVICE_READY)
		{
			usbListener.start();
			setStatus(DEVICE_LISTENING);
		}
	}

	public void stop()
	{
		if (status == DEVICE_LISTENING)
		{
			usbListener.interrupt();
			wacomConnection.close();
			setStatus(DEVICE_CLOSED);
		}
	}

	private class PermissionReceiver extends BroadcastReceiver
	{
		Handler handler;

		public PermissionReceiver(Handler handler)
		{
			this.handler = handler;
		}

		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.d("AJDBG", "Recieved permission");
			if (intent.getAction().equals(ACTION_PERMISSION))
			{
				Log.d("AJDBG", "equals to " + ACTION_PERMISSION);
				setStatus(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) ? PERMISSION_GRANTED : PERMISSION_DENIED);
			}
		}
	}

	private class UsbListener extends Thread
	{
		private byte[] buffer;
		int[] data;

		public UsbListener()
		{
			buffer = new byte[WACOM_PACKAGE_SIZE];
			data = new int[5]; /* x, y, pressure, tip, button */
		}

		@Override
		public void run()
		{
			/* Switch Wacom tablet to full range mode (non HID compliant) */
			wacomConnection.controlTransfer(
				UsbConstants.USB_TYPE_CLASS | UsbConstants.USB_INTERFACE_SUBCLASS_BOOT, /* request type */
				USB_REQ_SET_REPORT, /* request ID */
				(WAC_HID_FEATURE_REPORT << 8) + 2, 0, /* request value, request index*/
				new byte[] {2, 2}, /* report ID, mode */
				2, 0 /* data length, timeout */
			);
	
			while (wacomConnection.bulkTransfer(wacomEndpoint, buffer, WACOM_PACKAGE_SIZE, 0) >= 0)
			{
				/* See https://github.com/jigpu/linuxwacom-wiki-archive/blob/master/wiki/USB_Protocol.md#bamboo-stylus-event-packets
				 * for data stream scheme explanation.
				 */
				/* `& 0xFF' is applied to buffer because Java fills new bits with MSB value */
				data[DATA_X] = ((int) buffer[3] & 0xFF) << 8;
				data[DATA_X] |= ((int) buffer[2] & 0xFF);
				data[DATA_Y] = ((int) buffer[5] & 0xFF) << 8;
				data[DATA_Y] |= ((int) buffer[4] & 0xFF);
				data[DATA_PRESSURE] = ((int) buffer[7] & 0xFF) << 8;
				data[DATA_PRESSURE] |= ((int) buffer[6] & 0xFF);
				data[DATA_TIP] = (buffer[1]) & 0x1;
				data[DATA_BUTTON] = (buffer[1] >> 1) & 0b11;
	
				handler.sendMessage(message.obtain(handler, DATA, data));
			}
		}
	}
}
