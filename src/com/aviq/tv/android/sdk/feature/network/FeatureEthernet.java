/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEthernet.java
 * Author:      alek
 * Date:        3 Jan 2014
 * Description: Ethernet Settings component
 */

package com.aviq.tv.android.sdk.feature.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Ethernet Settings component
 */
public class FeatureEthernet extends FeatureComponent
{
	public static final String TAG = FeatureEthernet.class.getSimpleName();
	private static final String ETH_SERVICE = "ethernet";

	public enum UserParam
	{
		/**
		 * Ethernet interface name
		 */
		INTERFACE,

		/**
		 * Is DHCP configuration
		 */
		IS_DHCP,

		/**
		 * IP address
		 */
		IP,

		/**
		 * IP mask
		 */
		MASK,

		/**
		 * Network gateway
		 */
		GATEWAY,

		/**
		 * DNS1
		 */
		DNS1,

		/**
		 * DNS2
		 */
		DNS2
	}

	private Prefs _userPrefs;
	private EthernetManagerWrapper _ethernetManagerWrapper;

	public class NetworkConfig
	{
		public String Iface;
		public boolean IsDHCP;
		public boolean IsUp;
		public String Addr;
		public String Mask;
		public String Gateway;
		public String Dns1;
		public String Dns2;

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append(Iface);
			if (IsDHCP)
				sb.append(" dhcp");
			else
				sb.append(" manual");
			if (IsUp)
				sb.append(" up");
			else
				sb.append(" down");
			sb.append(" ip:").append(Addr);
			sb.append(" mask:").append(Mask);
			sb.append(" gw:").append(Gateway);
			sb.append(" dns1:").append(Dns1);
			sb.append(" dns2:").append(Dns2);
			return sb.toString();
		}
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();
		_ethernetManagerWrapper = new EthernetManagerWrapper();
		if (!_ethernetManagerWrapper.isSupported())
		{
			onFeatureInitialized.onInitialized(this, ResultCode.NOT_SUPPORTED);
		}
		else
		{
			_ethernetManagerWrapper.enable(true);
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.ETHERNET;
	}

	public List<NetworkInterface> getNetworkInterfaces()
	{
		return _ethernetManagerWrapper.getNetworkInterfaces();
	}

	public void configureNetwork(NetworkConfig networkConfig) throws SecurityException
	{
		_ethernetManagerWrapper.setConfiguration(networkConfig);
	}

	public NetworkConfig getNetworkConfig()
	{
		return _ethernetManagerWrapper.getConfiguration();
	}

	private class EthernetManagerWrapper
	{
		private boolean _isSupported = false;

		// EthernetManager references
		private Object _ethernetManager;
		private Method _getDeviceNameList;
		private Method _setEthEnabled;
		private Method _updateEthDevInfo;
		private Method _getSavedEthConfig;
		private Method _getDhcpInfo;

		// EthernetDevInfo references
		private Object _ethernetDevInfo;
		private Method _setConnectMode;
		private Method _getConnectMode;
		private Method _setIfName;
		private Method _getIfName;
		private Method _setIpAddress;
		private Method _getIpAddress;
		private Method _setNetMask;
		private Method _getNetMask;
		private Method _setRouteAddr;
		private Method _getRouteAddr;
		private Method _setDnsAddr;
		private Method _getDnsAddr;

		EthernetManagerWrapper()
		{
			_ethernetManager = Environment.getInstance().getSystemService(ETH_SERVICE);
			try
			{
				Class<?> ethernetDevInfoClass = Class.forName("android.net.ethernet.EthernetDevInfo");

				if (_ethernetManager != null)
				{
					_setEthEnabled = _ethernetManager.getClass().getMethod("setEthEnabled", boolean.class);
					_getDeviceNameList = _ethernetManager.getClass().getMethod("getDeviceNameList");
					_updateEthDevInfo = _ethernetManager.getClass().getMethod("updateEthDevInfo", ethernetDevInfoClass);
					_getSavedEthConfig = _ethernetManager.getClass().getMethod("getSavedEthConfig");
					_getDhcpInfo = _ethernetManager.getClass().getMethod("getDhcpInfo");
				}

				_ethernetDevInfo = ethernetDevInfoClass.newInstance();
				_setConnectMode = ethernetDevInfoClass.getMethod("setConnectMode", String.class);
				_getConnectMode = ethernetDevInfoClass.getMethod("getConnectMode");
				_setIfName = ethernetDevInfoClass.getMethod("setIfName", String.class);
				_getIfName = ethernetDevInfoClass.getMethod("getIfName");
				_setIpAddress = ethernetDevInfoClass.getMethod("setIpAddress", String.class);
				_getIpAddress = ethernetDevInfoClass.getMethod("getIpAddress");
				_setNetMask = ethernetDevInfoClass.getMethod("setNetMask", String.class);
				_getNetMask = ethernetDevInfoClass.getMethod("getNetMask");
				_setRouteAddr = ethernetDevInfoClass.getMethod("setRouteAddr", String.class);
				_getRouteAddr = ethernetDevInfoClass.getMethod("getRouteAddr");
				_setDnsAddr = ethernetDevInfoClass.getMethod("setDnsAddr", String.class);
				_getDnsAddr = ethernetDevInfoClass.getMethod("getDnsAddr");

				_isSupported = true;
			}
			catch (NoSuchMethodException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (ClassNotFoundException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (InstantiationException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (IllegalAccessException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}

		private boolean isSupported()
		{
			return _isSupported;
		}

		private List<NetworkInterface> getNetworkInterfaces()
		{
			List<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();
			try
			{
				String[] deviceNames = (String[]) _getDeviceNameList.invoke(_ethernetManager);
				if (deviceNames != null)
				{
					for (String deviceName : deviceNames)
					{
						NetworkInterface networkInterface = NetworkInterface.getByName(deviceName);
						if (networkInterface != null)
							networkInterfaces.add(networkInterface);
					}
				}
			}
			catch (IllegalAccessException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (InvocationTargetException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (SocketException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			return networkInterfaces;
		}

		private void enable(boolean isEnabled)
		{
			if (_setEthEnabled != null)
			{
				try
				{
					_setEthEnabled.invoke(_ethernetManager, Boolean.TRUE);
				}
				catch (IllegalAccessException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				catch (IllegalArgumentException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				catch (InvocationTargetException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}

		private void setConfiguration(NetworkConfig networkConfig) throws SecurityException
		{
			try
			{
				_setIfName.invoke(_ethernetDevInfo, networkConfig.Iface);
				_setConnectMode.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? "dhcp" : "manual");
				_setIpAddress.invoke(_ethernetDevInfo, networkConfig.Addr);
				_setNetMask.invoke(_ethernetDevInfo, networkConfig.Mask);
				_setRouteAddr.invoke(_ethernetDevInfo, networkConfig.Gateway);
				_setDnsAddr.invoke(_ethernetDevInfo, networkConfig.Dns1);
				_updateEthDevInfo.invoke(_ethernetManager, _ethernetDevInfo);
			}
			catch (IllegalAccessException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (InvocationTargetException e)
			{
				if (e.getTargetException() instanceof SecurityException)
					throw (SecurityException) e.getTargetException();
				Log.e(TAG, e.getMessage(), e);
			}
		}

		private NetworkConfig getConfiguration()
		{
			NetworkConfig networkConfig = new NetworkConfig();
			try
			{
				_ethernetDevInfo = _getSavedEthConfig.invoke(_ethernetManager);
				networkConfig.Iface = (String) _getIfName.invoke(_ethernetDevInfo);

				NetworkInterface networkInterface = NetworkInterface.getByName(networkConfig.Iface);
				if (networkInterface != null)
					networkConfig.IsUp = networkInterface.isUp();

				String connectMode = (String) _getConnectMode.invoke(_ethernetDevInfo);
				networkConfig.IsDHCP = "dhcp".equals(connectMode);
				if (networkConfig.IsDHCP)
				{
					DhcpInfo dhcpInfo = (DhcpInfo) _getDhcpInfo.invoke(_ethernetManager);
					networkConfig.Addr = intToIP(dhcpInfo.ipAddress);
					networkConfig.Mask = intToIP(dhcpInfo.netmask);
					networkConfig.Gateway = intToIP(dhcpInfo.gateway);
					networkConfig.Dns1 = intToIP(dhcpInfo.dns1);
					networkConfig.Dns2 = intToIP(dhcpInfo.dns2);
				}
				else
				{
					networkConfig.Addr = (String) _getIpAddress.invoke(_ethernetDevInfo);
					networkConfig.Mask = (String) _getNetMask.invoke(_ethernetDevInfo);
					networkConfig.Gateway = (String) _getRouteAddr.invoke(_ethernetDevInfo);
					networkConfig.Dns1 = (String) _getDnsAddr.invoke(_ethernetDevInfo);
					for (String dns : getDNSAddresses())
					{
						if (networkConfig.Dns1 == null)
							networkConfig.Dns1 = dns;
						else if (networkConfig.Dns2 == null)
							networkConfig.Dns2 = dns;
						else
							break;
					}
				}
			}
			catch (IllegalAccessException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (InvocationTargetException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (SocketException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			return networkConfig;
		}

		private String intToIP(int intIp)
		{
			if (intIp == 0)
				return null;
			byte[] ipBytes = BigInteger.valueOf(intIp).toByteArray();

			if (ipBytes.length != 4)
			{
				ipBytes = new byte[4];
				ipBytes[0] = ipBytes[1] = ipBytes[2] = ipBytes[3] = 0;
			}

			// reverse bytes order
			byte temp;
			temp = ipBytes[0];
			ipBytes[0] = ipBytes[3];
			ipBytes[3] = temp;
			temp = ipBytes[1];
			ipBytes[1] = ipBytes[2];
			ipBytes[2] = temp;

			return TextUtils.implodeBytesArray(ipBytes, "%d", ".");
		}
	}

	private List<String> getDNSAddresses()
	{
		List<String> dnsAddresses = new ArrayList<String>();
		ConnectivityManager mgr = (ConnectivityManager) Environment.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
		try
		{
			Method getLinkPropeties;
			Method getDnses;
			getLinkPropeties = mgr.getClass().getMethod("getLinkProperties", int.class);
			Object linkProperties = getLinkPropeties.invoke(mgr, ConnectivityManager.TYPE_ETHERNET);
			getDnses = linkProperties.getClass().getMethod("getDnses");
			@SuppressWarnings("unchecked")
			Collection<InetAddress> dnses = (Collection<InetAddress>) getDnses.invoke(linkProperties);

			if (dnses != null)
				for (InetAddress dns : dnses)
				{
					dnsAddresses.add(TextUtils.implodeBytesArray(dns.getAddress(), "%d", "."));
				}
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return dnsAddresses;
	}
}
