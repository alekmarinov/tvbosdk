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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.KeyEvent;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.aviq.tv.android.sdk.Version;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
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
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

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
	public static final int ON_STATE_CHANGED = EventMessenger.ID("ON_STATE_CHANGED");
	public static final int ON_WAIT_START = EventMessenger.ID("ON_WAIT_START");
	public static final int ON_WAIT_STOP = EventMessenger.ID("ON_WAIT_STOP");
	public static final int ON_WAIT_CANCELED = EventMessenger.ID("ON_WAIT_CANCELED");

	// FIXME: Convert EXTRA_KEY* to enum ExtraKey
	public static final String EXTRA_KEY = "KEY";
	public static final String EXTRA_KEYCODE = "KEYCODE";
	public static final String EXTRA_KEYREPEAT = "KEYREPEAT";
	public static final String EXTRA_KEYCONSUMED = "KEYCONSUMED";
	public static final String SYSTEM_PREFS = "system";
	private static final String AVIQTV_XML_RESOURCE = "aviqtv";
	private static final String ECLIPSE_XML_RESOURCE = "eclipse";
	private static final String RELEASE_XML_RESOURCE = "release";

	public enum OnWaitStartExtras
	{
		MESSAGE
	}

	public enum ExtraInitError
	{
		ERROR_CODE, ERROR_DATA, FEATURE_NAME, FEATURE_CLASS
	}

	public enum ExtraStateChanged
	{
		STATE_NAME
	}

	public static enum Param
	{
		/**
		 * whether we are in release build
		 */
		RELEASE("devel"),

		/**
		 * whether we are in debug mode
		 */
		DEBUG(false),

		/**
		 * Timeout in seconds for feature initialization
		 */
		FEATURE_INITIALIZE_TIMEOUT(180),

		/**
		 * The overlay background color
		 */
		OVERLAY_BACKGROUND_COLOR(0x00000000),

		/**
		 * the main background color
		 */
		MAIN_BACKGROUND_COLOR(0x00000000),

		/**
		 * Disk cache for images (1G)
		 */
		IMAGE_DISK_CACHE_SIZE(1024 * 1024 * 1024),

		/**
		 * Memory cache for images (50 MB)
		 */
		IMAGE_MEM_CACHE_SIZE(50 * 1024 * 1024),

		/**
		 * Maximum keys to keep in queue, remaining will be discarded
		 */
		MAX_KEYS_IN_QUEUE(3);

		Param(int value)
		{
			Environment.getInstance().getPrefs().put(name(), value);
		}

		Param(boolean value)
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
	private boolean _keyEventsEnabled = true;
	private boolean _keyRepetitionEnabled = true;
	private ExceptKeysList _exceptKeysDisabled = new ExceptKeysList();
	private ExceptKeysList _exceptKeysRepetition = new ExceptKeysList();
	private ConcurrentLinkedQueue<AVKeyEvent> _keysQueue = new ConcurrentLinkedQueue<AVKeyEvent>();
	private Context _context;
	private boolean _isPause;
	private Cache _volleyCache;
	private Network _volleyNetwork;
	private int _maxKeysInQueue;
	private boolean _isKeyQueueEnabled = false;

	private OnResultReceived _onFeaturesReceived = new OnResultReceived()
	{
		@Override
		public void onReceiveResult(FeatureError error, Object object)
		{
			try
			{
				// Log target device parameters
				DisplayMetrics metrics = getDisplayMetrics();
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
				_stateManager.setMainBackgroundColor(getPrefs().getInt(Param.MAIN_BACKGROUND_COLOR));
				_featureRCU = (FeatureRCU) _featureManager.use(FeatureName.Component.RCU);

				_serviceController = new ServiceController(Environment.this);

				// Setup Volley

				// create network
				_volleyNetwork = new BasicNetwork(new HttpClientStack(AndroidHttpClient.newInstance("tvbosdk/volley")));

				// Use 1/2th of the available memory for caching the global
				// request queue
				int cacheSize = 1024 * 1024 * memClass / 2;
				_volleyCache = new DiskBasedCache(getCacheDir(), cacheSize);

				// create request queue
				_requestQueue = newRequestQueue();

				BitmapMemLruCache memCache = new BitmapMemLruCache(getPrefs().getInt(Param.IMAGE_MEM_CACHE_SIZE));

				// BitmapDiskLruCache diskCache = new
				// BitmapDiskLruCache(getPrefs().getInt(
				// Param.IMAGE_DISK_CACHE_SIZE));

				ImageCache noCache = new ImageCache()
				{
					@Override
					public void putBitmap(String url, Bitmap bitmap)
					{
					}

					@Override
					public Bitmap getBitmap(String url)
					{
						return null;
					}
				};

				// _imageLoader = new ImageLoader(_requestQueue, memCache);
				// _imageLoader = new ImageLoader(_requestQueue, diskCache);
				// _imageLoader = new ImageLoader(_requestQueue, new
				// BitmapMemDiskLruCache(memCache, diskCache));
				_imageLoader = new ImageLoader(_requestQueue, noCache);

				// initializes features
				getEventMessenger().trigger(ON_INITIALIZE);
				_featureManager.setInitTimeout(getPrefs().getInt(Param.FEATURE_INITIALIZE_TIMEOUT));
				_featureManager.initialize(new OnFeatureInitialized()
				{
					@Override
					public void onInitialized(FeatureError error)
					{
						if (error.isError())
						{
							Log.e(TAG, error.getMessage(), error);

							Bundle bundle = new Bundle();
							bundle.putInt(ExtraInitError.ERROR_CODE.name(), error.getCode());
							bundle.putBundle(ExtraInitError.ERROR_DATA.name(), error.getBundle());
							bundle.putString(ExtraInitError.FEATURE_NAME.name(), error.getFeature().getName());
							bundle.putString(ExtraInitError.FEATURE_CLASS.name(), error.getFeature().getClass()
							        .getName());
							_eventMessenger.trigger(ON_FEATURE_INIT_ERROR, bundle);
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

			// copy debug param from system to user prefs if not already set
			if (!_userPrefs.has(Param.DEBUG))
			{
				_userPrefs.put(Param.DEBUG, _prefs.getBool(Param.DEBUG));
			}

			Log.enableRingBuffer(_userPrefs.getBool(Param.DEBUG));

			_maxKeysInQueue = _prefs.getInt(Param.MAX_KEYS_IN_QUEUE);

			// enter strict mode in non release builds
			if (isDevel())
			{
				// StrictMode.setThreadPolicy(new
				// StrictMode.ThreadPolicy.Builder().detectAll().build());
				// StrictMode.setVmPolicy(new
				// StrictMode.VmPolicy.Builder().detectAll().build());
			}

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
					public void onReceiveResult(FeatureError error, Object object)
					{
						// initialize environment by app's raw/release.xml
						Log.i(TAG, "Parsing " + RELEASE_XML_RESOURCE + " xml definition");
						int appXmlId = getResources().getIdentifier(RELEASE_XML_RESOURCE, "raw", getPackageName());
						if (appXmlId != 0)
						{
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
						else
						{
							Log.d(TAG, "No " + RELEASE_XML_RESOURCE + ".xml to parse");
							_onFeaturesReceived.onReceiveResult(error, null);
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
		if (!_keysProcessorThread.isAlive())
			_keysProcessorThread.start();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Log.i(TAG, ".onPause");
		_isPause = true;
		getEventMessenger().trigger(ON_PAUSE);
		_keysProcessorThread.interrupt();
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
		if (_featureRCU != null)
		{
			Key key = _featureRCU.getKey(keyCode);

			if (_isKeyQueueEnabled)
			{
				// limit the number of keys in the queue discarding oldest keys
				// FIXME: alek: consider replacing while to if
				while (_keysQueue.size() > _maxKeysInQueue)
				{
					_keysQueue.remove();
				}

				_keysQueue.add(new AVKeyEvent(event, key));
			}
			else
				return onKeyDown(new AVKeyEvent(event, key));
			return true;
		}
		Log.w(TAG, ".onKeyDown: two early call before feature RCU is ready.");
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (_featureRCU != null)
		{
			Key key = _featureRCU.getKey(keyCode);
			// return onKeyUp(new AVKeyEvent(event, key));
			_keysQueue.add(new AVKeyEvent(event, key));
			return true;
		}
		Log.w(TAG, ".onKeyUp: two early call before feature RCU is ready.");
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Log.i(TAG, ".onConfigurationChanged: new language = " + newConfig.locale.getLanguage());
		super.onConfigurationChanged(newConfig);
	}

	public DisplayMetrics getDisplayMetrics()
	{
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics;
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
	 * Creates new RequestQueue
	 *
	 * @return RequestQueue
	 */
	public RequestQueue newRequestQueue()
	{
		Log.d(TAG, ".newRequestQueue");
		RequestQueue requestQueue = new RequestQueue(_volleyCache, _volleyNetwork);
		requestQueue.start();
		return requestQueue;
	}

	//
	// public void setRequestQueue(RequestQueue requestQueue)
	// {
	// _requestQueue = requestQueue;
	// }

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
	 * Returns application version code
	 *
	 * @return version code
	 */
	public int getVersionCode()
	{
		try
		{
			return getPackageManager().getPackageInfo(getApplication().getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return 0;
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
		Log.i(TAG, ".setKeyEventsEnabled");
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
		Log.i(TAG, ".setKeyEventsDisabled");
		_keyEventsEnabled = false;
		return _exceptKeysDisabled;
	}

	/**
	 * Enable key repetition
	 */
	public void setKeyRepetitionEnabled()
	{
		Log.i(TAG, ".setKeyRepetitionEnabled");
		_keyRepetitionEnabled = true;
	}

	/**
	 * Disable key repetition
	 *
	 * @param enabled
	 *            true to enable key events, false to disable key events
	 */
	public ExceptKeysList setKeyRepetitionDisabled()
	{
		Log.i(TAG, ".setKeyRepetitionDisabled");
		_keyRepetitionEnabled = false;
		return _exceptKeysRepetition;
	}

	/**
	 * Enable key handling mode in which all keys are pushed in a limited queue
	 * and a background thread is sending the keys to the app sequentially
	 *
	 * @param isKeyQueueEnabled
	 */
	public void setKeyQueueEnabled(boolean isKeyQueueEnabled)
	{
		_isKeyQueueEnabled = isKeyQueueEnabled;
	}

	/**
	 * @return true if the keys queue mode is enabled
	 */
	public boolean isKeyQueueEnabled()
	{
		return _isKeyQueueEnabled;
	}

	/**
	 * Inject key press in the environment
	 */
	/* package */boolean onKeyDown(AVKeyEvent keyEvent)
	{
		keyEvent.Event.startTracking();
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_KEY, keyEvent.Code.name());
		bundle.putInt(EXTRA_KEYCODE, keyEvent.Event.getKeyCode());
		bundle.putInt(EXTRA_KEYREPEAT, keyEvent.Event.getRepeatCount());

		boolean consumed = _stateManager.onKeyDown(keyEvent);
		Log.i(TAG, ".onKeyDown: key = " + keyEvent + ", repeat = " + keyEvent.Event.getRepeatCount()
		        + ", keyEventsEnabled = " + _keyEventsEnabled + ", _keyRepetitionEnabled = " + _keyRepetitionEnabled
		        + " -> consumed = " + consumed);

		if (!_keyRepetitionEnabled && keyEvent.Event.getRepeatCount() > 0
		        && !_exceptKeysRepetition.contains(keyEvent.Code))
		{
			Log.i(TAG, ".onKeyDown: key = " + keyEvent + " disabled by repetition rule");
			return true;
		}

		if (_keyEventsEnabled || _exceptKeysDisabled.contains(keyEvent.Code))
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

		if (_keyEventsEnabled || _exceptKeysDisabled.contains(keyEvent.Code))
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

	private class BitmapMemLruCache extends LruCache<String, Bitmap> implements ImageCache
	{
		public BitmapMemLruCache(int cacheSize)
		{
			super(cacheSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap value)
		{
			return value.getRowBytes() * value.getHeight() / 1024;
		}

		@Override
		public Bitmap getBitmap(String key)
		{
			Bitmap value = get(key);
			Log.i(TAG, ".getBitmap: key = " + key + ", value = " + value);
			return value;
		}

		@Override
		public void putBitmap(String key, Bitmap bitmap)
		{
			Log.i(TAG, ".putBitmap: key = " + key + ", bitmap = " + bitmap);
			put(key, bitmap);
		}
	}

	private class BitmapDiskLruCache implements ImageCache
	{
		private DiskLruCache _lruCache;

		public BitmapDiskLruCache(int cacheSize)
		{
			try
			{
				/**
				 * FIXME: causes silent exception as described bellow:
				 * A resource was acquired at attached stack trace but never
				 * released. See java.io.Closeable for information on avoiding
				 * resource leaks.
				 * java.lang.Throwable: Explicit termination method 'close' not
				 * called
				 */
				_lruCache = DiskLruCache.open(new File(getCacheDir(), "images"), getVersionCode(), 1, cacheSize);
			}
			catch (IOException e)
			{
				// will not use cache
				_lruCache = null;
				Log.e(TAG, e.getMessage(), e);
			}
		}

		@Override
		public Bitmap getBitmap(String url)
		{
			try
			{
				Snapshot snappshot = _lruCache.get(urlToKey(url));
				if (snappshot != null)
				{
					InputStream is = snappshot.getInputStream(0);
					Bitmap bmp = BitmapFactory.decodeStream(is);
					is.close();
					return bmp;
				}
			}
			catch (IOException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
			catch (NoSuchAlgorithmException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
			return null;
		}

		@Override
		public void putBitmap(String url, Bitmap bitmap)
		{
			try
			{
				DiskLruCache.Editor creator = _lruCache.edit(urlToKey(url));
				OutputStream os = creator.newOutputStream(0);
				bitmap.compress(CompressFormat.JPEG, 100, os);
				os.close();
				creator.commit();
			}
			catch (IOException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
			catch (NoSuchAlgorithmException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
		}

		private String urlToKey(String url) throws NoSuchAlgorithmException
		{
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(url.getBytes());
			StringBuffer sb = new StringBuffer();
			byte[] bytes = digest.digest();
			for (int i = 0; i < bytes.length; i++)
				sb.append(Integer.toString((bytes[i] & 0xFF) + 0x100, 16).substring(1));
			return sb.toString();
		}
	}

	private class BitmapMemDiskLruCache implements ImageCache
	{
		private BitmapMemLruCache _memCache;
		private BitmapDiskLruCache _diskCache;
		private ExecutorService _executorService;

		public BitmapMemDiskLruCache(BitmapMemLruCache memCache, BitmapDiskLruCache diskCache)
		{
			_memCache = memCache;
			_diskCache = diskCache;
			_executorService = Executors.newFixedThreadPool(10);
		}

		@Override
		public Bitmap getBitmap(String key)
		{
			Bitmap value = _memCache.get(key);
			if (value == null)
				value = _diskCache.getBitmap(key);
			return value;
		}

		@Override
		public void putBitmap(final String key, final Bitmap bitmap)
		{
			_memCache.put(key, bitmap);

			_executorService.submit(new Runnable()
			{
				@Override
				public void run()
				{
					_diskCache.putBitmap(key, bitmap);
				}
			});
		}
	}

	private class KeysProcessor implements Runnable
	{
		AVKeyEvent _keyEvent;

		@Override
		public void run()
		{
			if (_keyEvent.Event.getAction() == KeyEvent.ACTION_DOWN)
				onKeyDown(_keyEvent);
			else if (_keyEvent.Event.getAction() == KeyEvent.ACTION_UP)
				onKeyUp(_keyEvent);
		}
	}

	private KeysProcessor _keysProcessor = new KeysProcessor();
	private Thread _keysProcessorThread = new Thread(new KeysProcessorThread());

	private class KeysProcessorThread implements Runnable
	{
		@Override
		public void run()
		{
			while (!Thread.currentThread().isInterrupted())
			{
				AVKeyEvent keyEvent = _keysQueue.poll();
				if (keyEvent != null)
				{
					_keysProcessor._keyEvent = keyEvent;
					getEventMessenger().post(_keysProcessor);
				}
				else
				{
					try
					{
						Thread.sleep(50);
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}
	}
}
