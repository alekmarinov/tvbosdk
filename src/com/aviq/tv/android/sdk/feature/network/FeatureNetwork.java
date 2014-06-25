package com.aviq.tv.android.sdk.feature.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;

public abstract class FeatureNetwork extends FeatureComponent
{
	private static final String TAG = FeatureNetwork.class.getSimpleName();

	public static String getLocalIP()
	{
		try
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
					{
						return inetAddress.getHostAddress();
					}
				}
			}
		}
		catch (SocketException e)
		{
			Log.e(TAG, "Cannot retrieve local IP.", e);
		}
		return null;
	}
}
