/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Environment.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Defines application environment
 */

package com.aviq.tv.android.sdk.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.KeyEvent;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.Volley;
import com.aviq.tv.android.sdk.Version;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureManager;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureState;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.feature.IFeature.OnFeatureInitialized;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.core.state.StateManager;
import com.aviq.tv.android.sdk.feature.crashlog.FeatureCrashLog;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;

/**
 * Defines application environment
 */
public class Environment extends Activity
{
	public static final String TAG = Environment.class.getSimpleName();
	public static final int ON_INITIALIZE = EventMessenger.ID("ON_INITIALIZE");
	public static final int ON_LOADING = EventMessenger.ID("ON_LOADING");
	public static final int ON_LOADED = EventMessenger.ID("ON_LOADED");
	public static final int ON_FEATURE_INIT_ERROR = EventMessenger.ID("ON_FEATURE_INIT_ERROR");
	public static final int ON_RESUME = EventMessenger.ID("ON_RESUME");
	public static final int ON_PAUSE = EventMessenger.ID("ON_PAUSE");
	public static final String EXTRA_ERROR_CODE = "EXTRA_ERROR_CODE";
	public static final String EXTRA_FEATURE_NAME = "EXTRA_FEATURE_NAME";
	public static final String EXTRA_KEY = "KEY";
	public static final String EXTRA_KEYCODE = "KEYCODE";
	public static final String EXTRA_KEYCONSUMED = "KEYCONSUMED";
	public static final String SYSTEM_PREFS = "system";
	private static final String AVIQTV_XML_RESOURCE = "aviqtv";
	private static final String ECLIPSE_XML_RESOURCE = "eclipse";
	private static final String RELEASE_XML_RESOURCE = "release";

	public static enum Param
	{
		/**
		 * whether we are in release build
		 */
		RELEASE("devel"),

		/**
		 * Timeout in seconds for feature initialization
		 */
		FEATURE_INITIALIZE_TIMEOUT(FeatureManager.FEATURE_DEFAULT_INIT_TIMEOUT),

		/**
		 * The overlay background color
		 */
		OVERLAY_BACKGROUND_COLOR(0x00000000);

		Param(int value)
		{
			Environment.getInstance().getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getPrefs().put(name(), value);
		}
	}

	private static Environment _instance;
	private StateManager _stateManager;
	private ServiceController _serviceController;
	private FeatureManager _featureManager = new FeatureManager();
	private Prefs _prefs;
	private Prefs _userPrefs;
	private RequestQueue _requestQueue;
	private ImageLoader _imageLoader;
	private final EventMessenger _eventMessenger = new EventMessenger(TAG);
	private final Map<FeatureName.Component, Prefs> _componentPrefs = new HashMap<FeatureName.Component, Prefs>();
	private final Map<FeatureName.Scheduler, Prefs> _schedulerPrefs = new HashMap<FeatureName.Scheduler, Prefs>();
	private final Map<FeatureName.State, Prefs> _statePrefs = new HashMap<FeatureName.State, Prefs>();
	private boolean _isInitialized = false;
	private static boolean _isCreated = false;
	private FeatureRCU _featureRCU;
	private FeatureCrashLog _featureCrashLog;
	private boolean _keyEventsEnabled = true;
	private ExceptKeysList _exceptKeys = new ExceptKeysList();
	private Context _context;
	private boolean _isPause;

	private OnResultReceived _onFeaturesReceived = new OnResultReceived()
	{
		@Override
		public void onReceiveResult(int resultCode, Bundle resultData)
		{
			try
			{
				// Log target device parameters
				DisplayMetrics metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);
				int memClass = ((ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE))
				        .getMemoryClass();
				Log.i(TAG, "Initializing environment: version (app = " + getBuildVersion() + ", sdk = " + Version.NAME
				        + "), " + metrics.widthPixels + "x" + metrics.heightPixels + ", density = " + metrics.density
				        + ", densityDpi = " + metrics.densityDpi + ", scaledDensity = " + metrics.scaledDensity
				        + ", xdpi = " + metrics.xdpi + ", ydpi = " + metrics.ydpi + ", memClass = " + memClass + " mb");

				// initializes environment context
				_stateManager = new StateManager(Environment.this);
				_stateManager.setMessageState(_featureManager.use(FeatureName.State.MESSAGE_BOX));
				_stateManager.setOverlayBackgroundColor(getPrefs().getInt(Param.OVERLAY_BACKGROUND_COLOR));
				_featureRCU = (FeatureRCU) _featureManager.use(FeatureName.Component.RCU);
				_featureCrashLog = (FeatureCrashLog) _featureManager.use(FeatureName.Component.CRASHLOG);

				_serviceController = new ServiceController(Environment.this);
				_requestQueue = Volley.newRequestQueue(Environment.this);
				_requestQueue.getCache().clear();

				// Use 1/8th of the available memory for this memory cache.
				int cacheSize = 1024 * 1024 * memClass / 8;
				_imageLoader = new ImageLoader(_requestQueue, new BitmapLruCache(cacheSize));

				// initializes features
				getEventMessenger().trigger(ON_INITIALIZE);
				_featureManager.setInitTimeout(getPrefs().getInt(Param.FEATURE_INITIALIZE_TIMEOUT));
				_featureManager.initialize(new OnFeatureInitialized()
				{
					@Override
					public void onInitialized(IFeature feature, int resultCode)
					{
						if (resultCode != ResultCode.OK)
						{
							Bundle bundle = new Bundle();
							bundle.putInt(EXTRA_ERROR_CODE, resultCode);
							bundle.putString(EXTRA_FEATURE_NAME, feature.getName());
							_eventMessenger.trigger(ON_FEATURE_INIT_ERROR, bundle);
							_featureCrashLog.fatal(feature.getName(), "feature init failed with code " + resultCode, null);
						}
						else
						{
							_eventMessenger.trigger(ON_LOADED);
							_isInitialized = true;
						}
					}

					@Override
					public void onInitializeProgress(IFeature feature, float progress)
					{
						Bundle bundle = new Bundle();
						bundle.putFloat("progress", progress);
						bundle.putString("featureName", feature.getType().name() + " " + feature.getName());

						Log.i(TAG, "ON_LOADING: progress = " + progress);
						_eventMessenger.triggerDirect(ON_LOADING, bundle);
					}
				});
			}
			catch (FeatureNotFoundException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	};

	@SuppressWarnings("serial")
	public class ExceptKeysList extends ArrayList<Key>
	{
		public ExceptKeysList except(Key key)
		{
			add(key);
			return this;
		}
	}

	/**
	 * Environment constructor method
	 */
	public Environment()
	{
		_context = this;
		_instance = this;
	}

	/**
	 * Environment constructor method with external Context
	 */
	public Environment(Context context)
	{
		_context = context;
		_instance = this;
	}

	public static synchronized Environment getInstance()
	{
		return _instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (_isCreated)
		{
			finish();
			throw new RuntimeException("This process has already started!");
		}
		_isCreated = true;

		Log.i(TAG, ".onCreate");

		try
		{
			// initialize preferences
			_userPrefs = createUserPrefs();
			_prefs = createPrefs(SYSTEM_PREFS);
			int appDebugXmlId = getResources().getIdentifier(ECLIPSE_XML_RESOURCE, "raw", getPackageName());
			if (appDebugXmlId != 0)
			{
				String infoMsg = "Using debug xml definition - " + ECLIPSE_XML_RESOURCE;
				Log.i(TAG, String.format(String.format("%%0%dd", infoMsg.length()), 0).replace("0", "-"));
				Log.i(TAG, infoMsg);
				Log.i(TAG, String.format(String.format("%%0%dd", infoMsg.length()), 0).replace("0", "-"));

				// initialize environment by debug app's raw/eclipse.xml
				Log.i(TAG, "Parsing " + ECLIPSE_XML_RESOURCE + " xml definition");
				InputStream inputStream = getResources().openRawResource(appDebugXmlId);
				_featureManager.addFeaturesFromXml(inputStream, _onFeaturesReceived);
			}
			else
			{
				// initialize environment by default app's raw/aviqtv.xml
				Log.i(TAG, "Parsing " + AVIQTV_XML_RESOURCE + " xml definition");
				int appXmlId = getResources().getIdentifier(AVIQTV_XML_RESOURCE, "raw", getPackageName());
				InputStream inputStream = getResources().openRawResource(appXmlId);
				_featureManager.addFeaturesFromXml(inputStream, new OnResultReceived()
				{
					@Override
					public void onReceiveResult(int resultCode, Bundle resultData)
					{
						// initialize environment by app's raw/release.xml
						Log.i(TAG, "Parsing " + RELEASE_XML_RESOURCE + " xml definition");
						int appXmlId = getResources().getIdentifier(RELEASE_XML_RESOURCE, "raw", getPackageName());
						InputStream inputStream = getResources().openRawResource(appXmlId);
						try
						{
							_featureManager.addFeaturesFromXml(inputStream, _onFeaturesReceived);
						}
						catch (SAXException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						catch (IOException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
					}
				});
			}
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (SAXException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Log.i(TAG, ".onResume");
		_isPause = false;
		getEventMessenger().trigger(ON_RESUME);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Log.i(TAG, ".onPause");
		_isPause = true;
		getEventMessenger().trigger(ON_PAUSE);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG, ".onDestroy");
	}

	/**
	 * @return true if this activity is on pause
	 */
	public boolean isPause()
	{
		return _isPause;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Key key = _featureRCU.getKey(keyCode);
		return onKeyDown(new AVKeyEvent(event, key));
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		Key key = _featureRCU.getKey(keyCode);
		return onKeyUp(new AVKeyEvent(event, key));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Log.i(TAG, ".onConfigurationChanged: new language = " + newConfig.locale.getLanguage());
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Returns global preferences manager
	 *
	 * @return Prefs
	 */
	public Prefs getPrefs()
	{
		return _prefs;
	}

	/**
	 * Reset all feature preferences to their initial constant values
	 * The application must be rebooted after calling this method in order to
	 * reinitialize Prefs objects with the initial values
	 */
	public void resetPreferences()
	{
		Log.i(TAG, ".resetPreferences");

		// clear components prefs
		Iterator<Entry<FeatureName.Component, Prefs>> itc = _componentPrefs.entrySet().iterator();
		while (itc.hasNext())
		{
			Map.Entry<FeatureName.Component, Prefs> pairs = itc.next();
			Prefs prefs = pairs.getValue();
			prefs.clear();
		}

		// clear scheduler prefs
		Iterator<Entry<FeatureName.Scheduler, Prefs>> ith = _schedulerPrefs.entrySet().iterator();
		while (ith.hasNext())
		{
			Map.Entry<FeatureName.Scheduler, Prefs> pairs = ith.next();
			Prefs prefs = pairs.getValue();
			prefs.clear();
		}

		// clear state prefs
		Iterator<Entry<FeatureName.State, Prefs>> its = _statePrefs.entrySet().iterator();
		while (its.hasNext())
		{
			Map.Entry<FeatureName.State, Prefs> pairs = its.next();
			Prefs prefs = pairs.getValue();
			prefs.clear();
		}

		// clear system prefs
		getPrefs().clear();
	}

	/**
	 * Returns global services controller
	 *
	 * @return ServiceController
	 */
	public ServiceController getServiceController()
	{
		return _serviceController;
	}

	/**
	 * Returns global state manager
	 *
	 * @return StateManager
	 */
	public StateManager getStateManager()
	{
		return _stateManager;
	}

	/**
	 * Returns global feature manager
	 *
	 * @return FeatureManager
	 */
	public FeatureManager getFeatureManager()
	{
		return _featureManager;
	}

	/**
	 * Returns global Volley requests queue
	 *
	 * @return RequestQueue
	 */
	public RequestQueue getRequestQueue()
	{
		return _requestQueue;
	}

	/**
	 * Returns global Volley image loader
	 *
	 * @return ImageLoader
	 */
	public ImageLoader getImageLoader()
	{
		return _imageLoader;
	}

	/**
	 * Returns event messenger
	 *
	 * @return EventMessenger
	 */
	public EventMessenger getEventMessenger()
	{
		return _eventMessenger;
	}

	/**
	 * Parses the version string from the manifest.
	 *
	 * @return application version
	 */
	public String getBuildVersion()
	{
		try
		{
			return getPackageManager().getPackageInfo(getApplication().getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Get feature by specifying its class implementation
	 *
	 * @param featureClass
	 *            The class of the feature
	 * @return feature instance or null if not found
	 */
	public IFeature getFeature(Class<?> featureClass)
	{
		try
		{
			return _featureManager.getFeature(featureClass);
		}
		catch (FeatureNotFoundException e)
		{
			return null;
		}
	}

	/**
	 * @param featureName
	 * @return FeatureComponent or null
	 */
	public FeatureComponent getFeatureComponent(FeatureName.Component featureName)
	{
		try
		{
			return _featureManager.getFeatureComponent(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			return null;
		}
	}

	/**
	 * @param featureName
	 * @return FeatureScheduler or null
	 */
	public FeatureScheduler getFeatureScheduler(FeatureName.Scheduler featureName)
	{
		try
		{
			return _featureManager.getFeatureScheduler(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			return null;
		}
	}

	/**
	 * @param featureName
	 * @return FeatureState or null
	 */
	public FeatureState getFeatureState(FeatureName.State featureName)
	{
		try
		{
			return _featureManager.getFeatureState(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			return null;
		}
	}

	public Prefs getFeaturePrefs(FeatureName.Component featureName)
	{
		Prefs prefsFile = _componentPrefs.get(featureName);
		if (prefsFile == null)
		{
			prefsFile = createPrefs(featureName.name());
			_componentPrefs.put(featureName, prefsFile);
		}
		return prefsFile;
	}

	public Prefs getFeaturePrefs(FeatureName.Scheduler featureName)
	{
		Prefs prefsFile = _schedulerPrefs.get(featureName);
		if (prefsFile == null)
		{
			prefsFile = createPrefs(featureName.name());
			_schedulerPrefs.put(featureName, prefsFile);
		}
		return prefsFile;
	}

	public Prefs getFeaturePrefs(FeatureName.State featureName)
	{
		Prefs prefsFile = _statePrefs.get(featureName);
		if (prefsFile == null)
		{
			prefsFile = createPrefs(featureName.name());
			_statePrefs.put(featureName, prefsFile);
		}
		return prefsFile;
	}

	public Prefs getUserPrefs()
	{
		return _userPrefs;
	}

	/**
	 * @return true if all features has been initialized
	 */
	public boolean isInitialized()
	{
		return _isInitialized;
	}

	/**
	 * starts android app by package name
	 */
	public void startAppPackage(String packageName)
	{
		Log.i(TAG, "Starting " + packageName);
		Intent intent = _context.getPackageManager().getLaunchIntentForPackage(packageName);
		if (intent == null)
		{
			Log.w(getClass().getSimpleName(), "Can't find pacakge `" + packageName + "'");
		}
		else
		{
			_context.startActivity(intent);
		}
	}

	public boolean isDevel()
	{
		return "devel".equalsIgnoreCase(getPrefs().getString(Param.RELEASE));
	}

	public Key translateKeyCode(int keyCode)
	{
		return _featureRCU.getKey(keyCode);
	}

	/**
	 * Enable broadcasts of key events throughout the application.
	 */
	public void setKeyEventsEnabled()
	{
		_keyEventsEnabled = true;
	}

	/**
	 * Disable broadcasts of key events throughout the application.
	 *
	 * @param enabled
	 *            true to enable key events, false to disable key events
	 */
	public ExceptKeysList setKeyEventsDisabled()
	{
		_keyEventsEnabled = false;
		return _exceptKeys;
	}

	/**
	 * Inject key press in the environment
	 */
	/* package */boolean onKeyDown(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyDown: key = " + keyEvent + ", keyEventsEnabled = " + _keyEventsEnabled);

		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_KEY, keyEvent.Code.name());
		bundle.putInt(EXTRA_KEYCODE, keyEvent.Event.getKeyCode());
		boolean consumed = _stateManager.onKeyDown(keyEvent);

		if (_keyEventsEnabled || _exceptKeys.contains(keyEvent.Code))
		{
			bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
			_featureRCU.getEventMessenger().trigger(FeatureRCU.ON_KEY_PRESSED, bundle);
		}

		return consumed;
	}

	/**
	 * Inject key release in the environment
	 */
	/* package */boolean onKeyUp(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyUp: key = " + keyEvent + ", keyEventsEnabled = " + _keyEventsEnabled);
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_KEY, keyEvent.Code.name());
		bundle.putInt(EXTRA_KEYCODE, keyEvent.Event.getKeyCode());
		boolean consumed = _stateManager.onKeyUp(keyEvent);

		if (_keyEventsEnabled || _exceptKeys.contains(keyEvent.Code))
		{
			bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
			_featureRCU.getEventMessenger().trigger(FeatureRCU.ON_KEY_RELEASED, bundle);
		}

		return consumed;
	}

	private Prefs createUserPrefs()
	{
		Log.i(TAG, ".createUserPrefs");
		return new Prefs("user", _context.getSharedPreferences("user", Activity.MODE_PRIVATE), true);
	}

	private Prefs createPrefs(String name)
	{
		Log.i(TAG, ".createPrefs: name = " + name);
		return new Prefs(name, _context.getSharedPreferences(name, Activity.MODE_PRIVATE), false);
	}

	private class BitmapLruCache extends LruCache<String, Bitmap> implements ImageCache
	{
		public BitmapLruCache(int maxSize)
		{
			super(maxSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap value)
		{
			return value.getRowBytes() * value.getHeight();
		}

		@Override
		public Bitmap getBitmap(String url)
		{
			return get(url);
		}

		@Override
		public void putBitmap(String url, Bitmap bitmap)
		{
			// put(url, bitmap);
		}
	}
}
