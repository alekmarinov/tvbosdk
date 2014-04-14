/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aviq.tv.android.sdk.feature.network;

import java.lang.reflect.Field;

import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.aviq.tv.android.sdk.core.Log;

public class AccessPoint implements Comparable<AccessPoint>
{
	private static final String TAG = AccessPoint.class.getSimpleName();
	private static final int DISABLED_AUTH_FAILURE = 3;

	public static final int INVALID_NETWORK_ID = -1;

	/**
	 * Security type enumeration
	 */
	public static enum SecurityType
	{
		NONE, WEP, PSK, EAP
	}

	/**
	 * Password type
	 */
	public static enum PskType
	{
		UNKNOWN, WPA, WPA2, WPA_WPA2
	}

	private String _ssid;
	private String _bssid;
	private SecurityType _security;
	private int _networkId;
	private PskType _pskType = PskType.UNKNOWN;
	private WifiConfiguration _config;
	private ScanResult _scanResult;
	private int _rssi;
	private WifiInfo _wifiInfo;
	private DetailedState _state;

	private static SecurityType getSecurity(WifiConfiguration config)
	{
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK))
		{
			return SecurityType.PSK;
		}
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X))
		{
			return SecurityType.EAP;
		}
		return (config.wepKeys[0] != null) ? SecurityType.WEP : SecurityType.NONE;
	}

	private static SecurityType getSecurity(ScanResult result)
	{
		if (result.capabilities.contains("WEP"))
		{
			return SecurityType.WEP;
		}
		else if (result.capabilities.contains("PSK"))
		{
			return SecurityType.PSK;
		}
		else if (result.capabilities.contains("EAP"))
		{
			return SecurityType.EAP;
		}
		return SecurityType.NONE;
	}

	private static PskType getPskType(ScanResult result)
	{
		boolean wpa = result.capabilities.contains("WPA-PSK");
		boolean wpa2 = result.capabilities.contains("WPA2-PSK");
		if (wpa2 && wpa)
		{
			return PskType.WPA_WPA2;
		}
		else if (wpa2)
		{
			return PskType.WPA2;
		}
		else if (wpa)
		{
			return PskType.WPA;
		}
		else
		{
			return PskType.UNKNOWN;
		}
	}

	private static String removeDoubleQuotes(String string)
	{
		int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"'))
		{
			return string.substring(1, length - 1);
		}
		return string;
	}

	private static String convertToQuotedString(String string)
	{
		return "\"" + string + "\"";
	}

	AccessPoint(WifiConfiguration config)
	{
		_ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
		_bssid = config.BSSID;
		_security = getSecurity(config);
		_networkId = config.networkId;
		_rssi = Integer.MAX_VALUE;
		_config = config;
		Log.v(TAG, "AccessPoint:WifiConfiguration: ssid = " + _ssid + ", bssid = " + _bssid + ", security = "
		        + _security + ", networkId = " + _networkId + ", rssi = " + _rssi);
	}

	AccessPoint(ScanResult result)
	{
		_ssid = result.SSID;
		_bssid = result.BSSID;
		_security = getSecurity(result);
		if (_security == SecurityType.PSK)
		{
			_pskType = getPskType(result);
		}
		_networkId = -1;
		_rssi = result.level;
		_scanResult = result;
		Log.v(TAG, "AccessPoint:ScanResult: ssid = " + _ssid + ", bssid = " + _bssid + ", security = " + _security
		        + ", pskType = " + _pskType + ", rssi = " + _rssi + ", capabilities = " + result.capabilities);
	}

	@Override
	public int compareTo(AccessPoint other)
	{
		// Active one goes first.
		if (_wifiInfo != other._wifiInfo)
		{
			return (_wifiInfo != null) ? -1 : 1;
		}
		// Reachable one goes before unreachable one.
		if ((_rssi ^ other._rssi) < 0)
		{
			return (_rssi != Integer.MAX_VALUE) ? -1 : 1;
		}
		// Configured one goes before unconfigured one.
		if ((_networkId ^ other._networkId) < 0)
		{
			return (_networkId != -1) ? -1 : 1;
		}
		// Sort by signal strength.
		int difference = WifiManager.compareSignalLevel(other._rssi, _rssi);
		if (difference != 0)
		{
			return difference;
		}
		// Sort by ssid.
		return _ssid.compareToIgnoreCase(other._ssid);
	}

	public boolean isAuthError()
	{
		if (getNetworkId() == -1)
			return false; // not configured

		return getConfig().status == WifiConfiguration.Status.DISABLED &&  getDisableReason() == DISABLED_AUTH_FAILURE;
	}

	public String getSsid()
	{
		return _ssid;
	}

	public String getBssid()
	{
		return _bssid;
	}

	public SecurityType getSecurity()
	{
		return _security;
	}

	public int getNetworkId()
	{
		return _networkId;
	}

	public PskType getPskType()
	{
		return _pskType;
	}

	public WifiConfiguration getConfig()
	{
		return _config;
	}

	public ScanResult getScanResult()
	{
		return _scanResult;
	}

	public WifiInfo getWifiInfo()
	{
		return _wifiInfo;
	}

	public DetailedState getState()
	{
		return _state;
	}

	public int getLevel()
	{
		if (_rssi == Integer.MAX_VALUE)
		{
			return -1;
		}
		return WifiManager.calculateSignalLevel(_rssi, 6);
	}

	int getDisableReason()
	{
		try
		{
			WifiConfiguration config = getConfig();
			Field fieldDisableReason = config.getClass().getField("disableReason");
			return fieldDisableReason.getInt(config);
		}
		catch (NoSuchFieldException e)
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
		return -1;
	}

	boolean setScanResult(ScanResult result)
	{
		if (_ssid.equals(result.SSID) && _security == getSecurity(result))
		{
			if (WifiManager.compareSignalLevel(result.level, _rssi) > 0)
			{
				int oldLevel = getLevel();
				_rssi = result.level;
				if (getLevel() != oldLevel)
				{
					notifyChanged();
				}
			}
			// This flag only comes from scans, is not easily saved in config
			if (SecurityType.PSK.equals(_security))
			{
				_pskType = getPskType(result);
			}
			return true;
		}
		return false;
	}

	void setWifiInfo(WifiInfo info, DetailedState state)
	{
		boolean reorder = false;
		if (info != null && _networkId != INVALID_NETWORK_ID && _networkId == info.getNetworkId())
		{
			reorder = (_wifiInfo == null);
			_rssi = info.getRssi();
			_wifiInfo = info;
			_state = state;
		}
		else if (_wifiInfo != null)
		{
			reorder = true;
			_wifiInfo = null;
			_state = null;
		}
		if (reorder)
		{
			notifyHierarchyChanged();
		}
	}

	private void notifyChanged()
	{
		Log.i(TAG, ".notifyChanged");
	}

	private void notifyHierarchyChanged()
	{
		Log.i(TAG, ".notifyHierarchyChanged");
	}
}
