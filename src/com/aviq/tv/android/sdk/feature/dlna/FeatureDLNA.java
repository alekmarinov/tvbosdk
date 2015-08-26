/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureDLNA.java
 * Author:      alek
 * Date:        25 Jun 2015
 * Description: Provides DLNA capabilities
 */

package com.aviq.tv.android.sdk.feature.dlna;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.StateVariable;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.aviq.tv.android.sdk.Name;
import com.aviq.tv.android.sdk.Version;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Provides DLNA capabilities
 */
@SuppressWarnings("rawtypes")
public class FeatureDLNA extends FeatureComponent implements PropertyChangeListener
{
	private static final String TAG = FeatureDLNA.class.getSimpleName();
	private static final String SERVICE_ID = "StateService";

	private UDN _udn;
	private AndroidUpnpService _upnpService;

	/**
	 * @throws FeatureNotFoundException
	 */
	public FeatureDLNA() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// Fix the logging integration between java.util.logging and Android
		// internal logging
		org.seamless.util.logging.LoggingUtil.resetRootHandler(new FixedAndroidLogHandler());

		_udn = new UDN(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.UUID));
		Context context = Environment.getInstance().getApplicationContext();
		context.bindService(new Intent(context, AndroidUpnpServiceImpl.class), serviceConnection,
		        Context.BIND_AUTO_CREATE);

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.DLNA;
	}

	private ServiceConnection serviceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			_upnpService = (AndroidUpnpService) service;

			LocalDevice localDevice = _upnpService.getRegistry().getLocalDevice(_udn, true);
			if (localDevice == null)
			{
				try
				{
					localDevice = createDevice();
					_upnpService.getRegistry().addDevice(localDevice);
				}
				catch (Exception e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}

			LocalService<?> localService = localDevice
			        .findService(new ServiceType(Name.SDK, SERVICE_ID, Version.MAJOR));

			Log.i(TAG, "Found localService.getManager().getPropertyChangeSupport() = "
			        + localService.getManager().getPropertyChangeSupport());
			// Start monitoring the power switch
			((StateServiceManager) localService.getManager()).getPropertyChangeSupport().addPropertyChangeListener(
			        FeatureDLNA.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			_upnpService = null;
		}
	};

	@SuppressWarnings("unchecked")
	private LocalService createService() throws ValidationException
	{
		LocalService service = new LocalService(new ServiceType(Name.SDK, SERVICE_ID, Version.MAJOR), new ServiceId(
		        Name.SDK, SERVICE_ID), new Action[]
		{}, new StateVariable[]
		{});
		return service;
	}

	@SuppressWarnings("unchecked")
	private LocalDevice createDevice() throws ValidationException, LocalServiceBindingException
	{
		String customerName = TextUtils.capitalize(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.CUSTOMER));
		String brandName = TextUtils.capitalize(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BRAND));
		String productTitle = customerName;
		if (!brandName.equals(customerName))
			productTitle += " " + brandName;

		DeviceType type = new UDADeviceType(Name.SDK, 1);
		DeviceDetails details = new DeviceDetails(brandName, new ManufacturerDetails(customerName),
		        new ModelDetails(brandName, productTitle + " Set-Top Box based on " + Name.COMPANY + " " + Name.SDK.toUpperCase()
		                + Version.NAME, _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.VERSION)));
		LocalService stateService = createService();
		stateService.setManager(new StateServiceManager(stateService));

		LocalDevice device = new LocalDevice(new DeviceIdentity(_udn), type, details, DLNAIcon.createDeviceIcon(),
		        stateService);
		return device;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event)
	{
		Log.i(TAG, ".propertyChange: " + event.getPropertyName() + " -> " + event.getNewValue());
	}
}
