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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.utils.Files;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Ethernet Settings component
 */
public class FeatureEthernet extends FeatureNetwork
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

	private EthernetManagerWrapper _ethernetManagerWrapper;

	public FeatureEthernet()
	{
		_ethernetManagerWrapper = new EthernetManagerWrapper();
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		if (!_ethernetManagerWrapper.isSupported())
		{
			// FIXME: Should this be fatal error?
			// onFeatureInitialized.onInitialized(this, ResultCode.NOT_SUPPORTED);
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
		}
		else
		{
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

	public boolean isEthPlugged()
	{
		int status = -1;
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream("/sys/class/net/" + getNetworkConfig().Iface + "/carrier");
			status = fis.read();
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, e.getMessage());
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
		}
		finally
		{
			Files.closeQuietly(fis, TAG);
		}
		return status == 49; // '1'
	}

	public boolean isEnabled()
	{
		return _ethernetManagerWrapper.isEnabled();
	}

	public void setEnabled(boolean isEnabled)
	{
		if (_feature.Component.WIRELESS.isEnabled() == isEnabled)
			_feature.Component.WIRELESS.setEnabledDirect(!isEnabled);
		setEnabledDirect(isEnabled);
	}

	void setEnabledDirect(boolean isEnabled)
	{
		Log.i(TAG, ".setEnabled: " + isEnabled);
		// _ethernetManagerWrapper.setEnabled(isEnabled);
		Log.w(TAG, ".setEnabled is currently commented out!");
	}

	private class EthernetManagerWrapper
	{
		private boolean _isSupported = false;

		// EthernetManager references
		private Object _ethernetManager;
		private Method _getDeviceNameList;
		private Method _setEthEnabled;
		private Method _getEthState;
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
					_getEthState = _ethernetManager.getClass().getMethod("getEthState");
					_getDeviceNameList = _ethernetManager.getClass().getMethod("getDeviceNameList");
					_updateEthDevInfo = _ethernetManager.getClass().getMethod("updateEthDevInfo", ethernetDevInfoClass);
					_getSavedEthConfig = _ethernetManager.getClass().getMethod("getSavedEthConfig");
					_getDhcpInfo = _ethernetManager.getClass().getMethod("getDhcpInfo");
				}

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

		private void setEnabled(boolean isEnabled)
		{
			if (_setEthEnabled != null)
			{
				try
				{
					_setEthEnabled.invoke(_ethernetManager, isEnabled);
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

		private boolean isEnabled()
		{
			try
			{
				if (_getEthState == null)
					return false;
				Integer result = (Integer) _getEthState.invoke(_ethernetManager);
				return result.intValue() == 2;
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
			return false;
		}

		private void setConfiguration(NetworkConfig networkConfig) throws SecurityException
		{
			try
			{
				setEnabled(false);
				Class<?> ethernetDevInfoClass = Class.forName("android.net.ethernet.EthernetDevInfo");
				_ethernetDevInfo = ethernetDevInfoClass.newInstance();
				_setIfName.invoke(_ethernetDevInfo, networkConfig.Iface);
				_setConnectMode.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? "dhcp" : "manual");
				_setIpAddress.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? null : networkConfig.Addr);
				_setNetMask.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? null : networkConfig.Mask);
				_setRouteAddr.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? null : networkConfig.Gateway);
				_setDnsAddr.invoke(_ethernetDevInfo, networkConfig.IsDHCP ? null : networkConfig.Dns1);
				_updateEthDevInfo.invoke(_ethernetManager, _ethernetDevInfo);
				setEnabled(true);
			}
			catch (IllegalAccessException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (InstantiationException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (ClassNotFoundException e)
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
				if (networkConfig.Iface == null)
					networkConfig.Iface = "eth0";

				NetworkInterface networkInterface = NetworkInterface.getByName(networkConfig.Iface);
				if (networkInterface != null)
					networkConfig.IsUp = networkInterface.isUp();

				if (networkConfig.IsUp)
				{
					String connectMode = (String) _getConnectMode.invoke(_ethernetDevInfo);
					networkConfig.IsDHCP = "dhcp".equals(connectMode);
					if (networkConfig.IsDHCP)
					{
						DhcpInfo dhcpInfo = (DhcpInfo) _getDhcpInfo.invoke(_ethernetManager);
						networkConfig.Addr = NetworkConfig.IntToIP(dhcpInfo.ipAddress);
						networkConfig.Mask = NetworkConfig.IntToIP(dhcpInfo.netmask);
						networkConfig.Gateway = NetworkConfig.IntToIP(dhcpInfo.gateway);
						networkConfig.Dns1 = NetworkConfig.IntToIP(dhcpInfo.dns1);
						networkConfig.Dns2 = NetworkConfig.IntToIP(dhcpInfo.dns2);
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
	}

	private List<String> getDNSAddresses()
	{
		List<String> dnsAddresses = new ArrayList<String>();
		ConnectivityManager mgr = (ConnectivityManager) Environment.getInstance().getSystemService(
		        Context.CONNECTIVITY_SERVICE);
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
