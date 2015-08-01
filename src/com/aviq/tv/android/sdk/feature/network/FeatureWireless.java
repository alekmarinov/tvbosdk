/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureWifi.java
 * Author:      alek
 * Date:        13 Apr 2014
 * Description:
 */

package com.aviq.tv.android.sdk.feature.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.system.SystemProperties;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Wireless Settings component
 */
@Author("alek")
public class FeatureWireless extends FeatureComponent
{
	public static final String TAG = FeatureWireless.class.getSimpleName();
	public static final int RESCAN_INTERVAL = 10000;
	public static final int MAX_SCAN_RETRIES = 3;
	public static final int ON_ACCESS_POINTS_UPDATE = EventMessenger.ID("ON_ACCESS_POINTS_UPDATE");
	public static final int ON_ACCESS_POINT_CONNECTED = EventMessenger.ID("ON_ACCESS_POINT_CONNECTED");
	public static final String EXTRA_WIFI_STATE = "EXTRA_WIFI_STATE";
	public static final int ON_SCAN_ERROR = EventMessenger.ID("ON_SCAN_ERROR");
	public static final int ON_REQUEST_PASSWORD = EventMessenger.ID("ON_REQUEST_PASSWORD");

	// hidden by Android
	private static final String CONFIGURED_NETWORKS_CHANGED_ACTION = "CONFIGURED_NETWORKS_CHANGE";
	private static final String LINK_CONFIGURATION_CHANGED_ACTION = "LINK_CONFIGURATION_CHANGED";

	private WifiManager _wifiManager;
	private List<AccessPoint> _accessPoints = new ArrayList<AccessPoint>();

	// FIXME: get rid of this state variable
	private AccessPoint _currentAccessPoint;
	private String _lastPassword;

	public enum OnRequestPasswordExtras
	{
		SSID, LAST_PASSWORD, WRONG_PASSWORD
	}

	public enum OnAccessPointConnectedExtras
	{
		SSID
	}

	public enum WifiState
	{
		DISABLED, DISABLING, ENABLED, ENABLING, UNKNOWN
	}

	public FeatureWireless() throws FeatureNotFoundException
	{
		_wifiManager = (WifiManager) Environment.getInstance().getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		filter.addAction(CONFIGURED_NETWORKS_CHANGED_ACTION);
		filter.addAction(LINK_CONFIGURATION_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

		BroadcastReceiver receiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();
				Log.i(TAG, "BroadcastReceiver.onReceive: action = " + action);
				if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
				{
					int wifiStateInt = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
					WifiState wifiState = wrapWifiState(wifiStateInt);
					if (WifiState.ENABLED.equals(wifiState))
					{
						startScan();
					}
				}
				else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)
				        || CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action)
				        || LINK_CONFIGURATION_CHANGED_ACTION.equals(action))
				{
					updateAccessPoints();
				}
				else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action))
				{
					SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
					onNetworkStateChanged(WifiInfo.getDetailedStateOf(state));
				}
				else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action))
				{
					NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					onNetworkStateChanged(networkInfo.getDetailedState());
				}
				else if (WifiManager.RSSI_CHANGED_ACTION.equals(action))
				{
				}
			}
		};
		Environment.getInstance().registerReceiver(receiver, filter);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.WIRELESS;
	}

	public void setEnabled(boolean isEnabled)
	{
		FeatureEthernet featureEthernet = (FeatureEthernet) Environment.getInstance().getFeatureComponent(
		        FeatureName.Component.ETHERNET);
		if (featureEthernet != null)
			featureEthernet.setEnabledDirect(!isEnabled);
		setEnabledDirect(isEnabled);
	}

	void setEnabledDirect(boolean isEnabled)
	{
		Log.i(TAG, ".setEnabled: " + isEnabled);
		_wifiManager.setWifiEnabled(isEnabled);
	}

	public boolean isEnabled()
	{
		return _wifiManager.isWifiEnabled();
	}

	public void startScan()
	{
		Log.i(TAG, ".startScan");
		setEnabled(true);
		getEventMessenger().post(new Runnable()
		{
			private int _retry = 0;

			@Override
			public void run()
			{
				Log.i(TAG, "scanning: _retry = " + (1 + _retry) + " of " + MAX_SCAN_RETRIES);
				if (!_wifiManager.startScan())
				{
					if (_retry < MAX_SCAN_RETRIES)
					{
						_retry++;
						getEventMessenger().postDelayed(this, RESCAN_INTERVAL);
					}
					else
					{
						getEventMessenger().trigger(ON_SCAN_ERROR);
					}
				}
				else
				{
					Log.i(TAG, "Wireless scan started successfully");
				}
			}
		});
	}

	public List<AccessPoint> getAccessPoints()
	{
		return _accessPoints;
	}

	public boolean connectToNetwork(AccessPoint accessPoint)
	{
		return connectToNetwork(accessPoint, null);
	}

	public boolean connectToNetwork(AccessPoint accessPoint, String password)
	{
		Log.i(TAG, ".connectToNetwork: ssid = " + accessPoint.getSsid() + ", networkID = " + accessPoint.getNetworkId()
		        + ", security = " + accessPoint.getSecurity() + ", password = " + password);

		// remember the connecting access point and password
		_currentAccessPoint = accessPoint;
		_lastPassword = password;

		if (password == null)
		{
			boolean status = _wifiManager.enableNetwork(accessPoint.getNetworkId(), true);
			Log.i(TAG, "connectToNetwork returns with " + status);
			return status;
		}

		WifiConfiguration config = new WifiConfiguration();
		config.SSID = "\"" + accessPoint.getSsid() + "\"";

		switch (accessPoint.getSecurity())
		{
			case NONE:
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			break;

			case WEP:
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				config.wepKeys[0] = '"' + password + '"';
			// Only 1 WEP key is supported
			break;
			case PSK:
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				config.preSharedKey = '"' + password + '"';
			break;
			case EAP:
				Log.e(TAG, "Security type EAP is not supported yet!");
			break;
		}
		// Remove the initial wifi configuration before adding another one
		if (accessPoint.getConfig() != null)
		{
			_wifiManager.removeNetwork(accessPoint.getNetworkId());
			if (!_wifiManager.saveConfiguration())
			{
				Log.w(TAG, "WifiManager.saveConfiguration results failure after removed configured network");
			}
		}

		int networkId = _wifiManager.addNetwork(config);
		if (networkId == AccessPoint.INVALID_NETWORK_ID)
		{
			Log.w(TAG, "WifiManager.addNetwork results invalid network id " + AccessPoint.INVALID_NETWORK_ID);

			Bundle bundle = new Bundle();
			bundle.putString(OnRequestPasswordExtras.SSID.name(), accessPoint.getSsid());
			bundle.putBoolean(OnRequestPasswordExtras.WRONG_PASSWORD.name(), true);
			getEventMessenger().trigger(ON_REQUEST_PASSWORD, bundle);

			return false;
		}
		else if (!_wifiManager.saveConfiguration())
		{
			Log.w(TAG, "WifiManager.saveConfiguration results failure status");
			return false;
		}
		else
		{
			boolean status = _wifiManager.enableNetwork(networkId, true);
			Log.i(TAG, "connectToNetwork returns with " + status);
			return status;
		}
	}

	public WifiState getWifiState()
	{
		return wrapWifiState(_wifiManager.getWifiState());
	}

	public NetworkConfig getNetworkConfig()
	{
		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		if (wifiInfo == null)
			return null;

		NetworkConfig config = new NetworkConfig();
		config.Iface = SystemProperties.get("wifi.interface");
		config.IsUp = _wifiManager.isWifiEnabled();
		DhcpInfo dhcpInfo = _wifiManager.getDhcpInfo();
		config.IsDHCP = dhcpInfo != null;
		if (dhcpInfo != null)
		{
			config.Addr = NetworkConfig.IntToIP(dhcpInfo.ipAddress);
			config.Mask = NetworkConfig.IntToIP(dhcpInfo.netmask);
			config.Gateway = NetworkConfig.IntToIP(dhcpInfo.gateway);
			config.Dns1 = NetworkConfig.IntToIP(dhcpInfo.dns1);
			config.Dns2 = NetworkConfig.IntToIP(dhcpInfo.dns2);
		}
		return config;
	}

	public String getCurrentSsid()
	{
		String ssid = null;
		ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance().getSystemService(
		        Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo.isConnected())
		{
			WifiInfo connectionInfo = _wifiManager.getConnectionInfo();
			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID()))
			{
				ssid = connectionInfo.getSSID();
			}
		}
		return ssid;
	}

	private WifiState wrapWifiState(int wifiStateInt)
	{
		WifiState wifiState = WifiState.UNKNOWN;
		switch (wifiStateInt)
		{
			case WifiManager.WIFI_STATE_DISABLED:
				wifiState = WifiState.DISABLED;
			break;
			case WifiManager.WIFI_STATE_DISABLING:
				wifiState = WifiState.DISABLING;
			break;
			case WifiManager.WIFI_STATE_ENABLED:
				wifiState = WifiState.ENABLED;
			break;
			case WifiManager.WIFI_STATE_ENABLING:
				wifiState = WifiState.ENABLING;
			break;
		}
		return wifiState;
	}

	private void updateAccessPoints()
	{
		WifiState wifiState = getWifiState();
		Log.i(TAG, ".updateAccessPoints: wifi state = " + wifiState);

		_accessPoints.clear();
		if (WifiState.ENABLED.equals(wifiState))
		{
			collectAccessPoints(_accessPoints);
		}

		StringBuffer accessPointsBuffer = new StringBuffer();
		for (AccessPoint ap : _accessPoints)
		{
			if (accessPointsBuffer.length() > 0)
				accessPointsBuffer.append(", ");
			accessPointsBuffer.append(ap.getSsid());
		}
		Log.i(TAG, "ON_ACCESS_POINTS_UPDATE [" + accessPointsBuffer + "]");

		// access points list changed, notify the client
		getEventMessenger().trigger(ON_ACCESS_POINTS_UPDATE);
	}

	private void onNetworkStateChanged(DetailedState detailedState)
	{
		boolean isWifiEnabled = _wifiManager.isWifiEnabled();
		Log.i(TAG, ".onNetworkStateChanged: detailedState = " + detailedState + ", isWifiEnabled = " + isWifiEnabled);
		if (isWifiEnabled)
		{
			WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
			for (AccessPoint ap : _accessPoints)
			{
				if (wifiInfo.getNetworkId() == ap.getNetworkId())
				{
					ap.updateWifiInfo(wifiInfo, detailedState);
					if (DetailedState.CONNECTED.equals(ap.getState()))
					{
						Bundle bundle = new Bundle();
						bundle.putString(OnAccessPointConnectedExtras.SSID.name(), ap.getSsid());
						getEventMessenger().trigger(ON_ACCESS_POINT_CONNECTED, bundle);
						break;
					}
				}

				// find authentication failed reason
				if (_currentAccessPoint != null && _currentAccessPoint.getSsid().equals(ap.getSsid())
				        && ap.isAuthError())
				{
					Bundle bundle = new Bundle();
					bundle.putString(OnRequestPasswordExtras.SSID.name(), ap.getSsid());
					bundle.putString(OnRequestPasswordExtras.LAST_PASSWORD.name(), _lastPassword);
					bundle.putBoolean(OnRequestPasswordExtras.WRONG_PASSWORD.name(), true);
					getEventMessenger().trigger(ON_REQUEST_PASSWORD, bundle);
					return;
				}
			}
		}
		updateAccessPoints();
	}

	/** A restricted multimap for use in collectAccessPoints */
	private class Multimap<K, V>
	{
		private final HashMap<K, List<V>> store = new HashMap<K, List<V>>();

		/** retrieve a non-null list of values with key K */
		List<V> getAll(K key)
		{
			List<V> values = store.get(key);
			return values != null ? values : Collections.<V> emptyList();
		}

		void put(K key, V val)
		{
			List<V> curVals = store.get(key);
			if (curVals == null)
			{
				curVals = new ArrayList<V>(3);
				store.put(key, curVals);
			}
			curVals.add(val);
		}
	}

	/** Returns sorted list of access points */
	private void collectAccessPoints(List<AccessPoint> accessPoints)
	{
		Log.i(TAG, ".collectAccessPoints");
		/**
		 * Lookup table to more quickly update AccessPoints by only considering
		 * objects with the
		 * correct SSID. Maps SSID -> List of AccessPoints with the given SSID.
		 */
		Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

		final List<WifiConfiguration> configs = _wifiManager.getConfiguredNetworks();
		if (configs != null)
		{
			for (WifiConfiguration config : configs)
			{
				AccessPoint accessPoint = new AccessPoint(config);
				// accessPoint.update(mLastInfo, mLastState);
				accessPoints.add(accessPoint);
				apMap.put(accessPoint.getSsid(), accessPoint);
			}
		}

		final List<ScanResult> scanResults = _wifiManager.getScanResults();
		if (scanResults != null)
		{
			for (ScanResult scanResult : scanResults)
			{
				// Ignore hidden and ad-hoc networks.
				if (scanResult.SSID == null || scanResult.SSID.length() == 0
				        || scanResult.capabilities.contains("[IBSS]"))
				{
					continue;
				}

				boolean found = false;
				for (AccessPoint accessPoint : apMap.getAll(scanResult.SSID))
				{
					if (accessPoint.updateScanResult(scanResult))
						found = true;
				}
				if (!found)
				{
					AccessPoint accessPoint = new AccessPoint(scanResult);
					accessPoints.add(accessPoint);
					apMap.put(accessPoint.getSsid(), accessPoint);
				}
			}
		}

		WifiInfo wifiInfo = _wifiManager.getConnectionInfo();
		if (wifiInfo != null)
		{
			Log.i(TAG, ".collectAccessPoints: _wifiManager.getConnectionInfo() -> " + wifiInfo.getSSID() + ", "
			        + wifiInfo.getSupplicantState() + ", " + WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()));
			for (AccessPoint ap : accessPoints)
			{
				ap.updateWifiInfo(wifiInfo, WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()));
			}
		}

		// Pre-sort accessPoints to speed preference insertion
		Collections.sort(accessPoints);
	}
}
