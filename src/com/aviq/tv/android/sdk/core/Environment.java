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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.Volley;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureSet;
import com.aviq.tv.android.sdk.core.feature.FeatureState;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.feature.IFeatureFactory;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.state.StateException;
import com.aviq.tv.android.sdk.core.state.StateManager;

/**
 * Defines application environment
 */
public class Environment
{
	public static final String TAG = Environment.class.getSimpleName();
	public static final int ON_LOADING = EventMessenger.ID("ON_LOADING");
	public static final int ON_LOADED = EventMessenger.ID("ON_LOADED");
	public static final int ON_KEY_PRESSED = EventMessenger.ID("ON_KEY_PRESSED");
	public static final int ON_KEY_RELEASED = EventMessenger.ID("ON_KEY_RELEASED");
	public static final String EXTRA_KEY = "KEY";
	public static final String EXTRA_KEYCODE = "KEYCODE";
	public static final String EXTRA_KEYCONSUMED = "KEYCONSUMED";

	public enum Param
	{
		/**
		 * Timeout in seconds for feature initialization
		 */
		FEATURE_INITIALIZE_TIMEOUT(130);

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
	private Activity _activity;
	private StateManager _stateManager;
	private ServiceController _serviceController;
	private Prefs _prefs;
	private Prefs _userPrefs;
	private Properties _brandProperties;
	private RequestQueue _requestQueue;
	private ImageLoader _imageLoader;
	private List<IFeature> _features = new ArrayList<IFeature>();
	private EventMessenger _eventMessenger = new EventMessenger();
	private Map<FeatureName.Component, Prefs> _componentPrefs = new HashMap<FeatureName.Component, Prefs>();
	private Map<FeatureName.Scheduler, Prefs> _schedulerPrefs = new HashMap<FeatureName.Scheduler, Prefs>();
	private Map<FeatureName.State, Prefs> _statePrefs = new HashMap<FeatureName.State, Prefs>();
	private IFeatureFactory _featureFactory;
	// Chain based features initializer
	private FeatureInitializeCallBack _onFeatureInitialized = new FeatureInitializeCallBack();
	private boolean _isInitialized = false;

	/**
	 * Environment constructor method
	 */
	private Environment()
	{
	}

	public static synchronized Environment getInstance()
	{
		if (_instance == null)
			_instance = new Environment();
		return _instance;
	}

	public void setBrandProperties(Properties brandProperties)
	{
		_brandProperties = brandProperties;
	}

	/**
	 * Initialize environment
	 *
	 * @throws FeatureNotFoundException
	 * @throws StateException
	 */
	@SuppressLint("DefaultLocale")
	public void initialize(Activity activity) throws FeatureNotFoundException, StateException
	{
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		Log.i(TAG, "Initializing environment: " + metrics.widthPixels + "x" + metrics.heightPixels + ", density = "
		        + metrics.density + ", densityDpi = " + metrics.densityDpi + ", scaledDensity = "
		        + metrics.scaledDensity + ", xdpi = " + metrics.xdpi + ", ydpi = " + metrics.ydpi);

		// initializes environment context
		_activity = activity;
		_userPrefs = createUserPrefs();
		_prefs = createPrefs("system");
		_serviceController = new ServiceController(_activity);
		_requestQueue = Volley.newRequestQueue(_activity);
		_requestQueue.getCache().clear();

		// Use 1/8th of the available memory for this memory cache.
		int memClass = ((ActivityManager) activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE))
		        .getMemoryClass();
		int cacheSize = 1024 * 1024 * memClass / 8;
		_imageLoader = new ImageLoader(_requestQueue, new BitmapLruCache(cacheSize));

		// initializes features
		Log.i(TAG, "Sorting features topologically based on their declared dependencies");
		_features = topologicalSort(_features);

		// Initialize brand properties
		if (_brandProperties != null)
		{
			Log.i(TAG, "Apply brand properties");
			Enumeration<Object> keys = _brandProperties.keys();

			while (keys.hasMoreElements())
			{
				String key = (String) keys.nextElement();
				String value = _brandProperties.getProperty(key);
				Log.i(TAG, key + " = `" + value + "'");
				String[] parts = key.split("\\.");
				if (parts.length != 3)
					throw new RuntimeException(
					        "Invalid brand.properties key, expected <featureType>.<featureName>.<featureParam>, got `"
					                + key + "'");
				String featureType = parts[0];
				String featureName = parts[1];
				String featureParam = parts[2];
				Prefs prefs;
				if (featureType.equalsIgnoreCase(IFeature.Type.COMPONENT.name()))
					prefs = getFeaturePrefs(FeatureName.Component.valueOf(featureName));
				else if (featureType.equalsIgnoreCase(IFeature.Type.SCHEDULER.name()))
					prefs = getFeaturePrefs(FeatureName.Scheduler.valueOf(featureName));
				else if (featureType.equalsIgnoreCase(IFeature.Type.STATE.name()))
					prefs = getFeaturePrefs(FeatureName.State.valueOf(featureName));
				else
					throw new RuntimeException(
					        "Invalid feature type in brand.properties key, expected component, scheduler or state, got `"
					                + featureType + "'");

				Log.i(TAG, "Set brand property " + featureType.toLowerCase() + "." + featureName.toUpperCase() + "."
				        + featureParam + "=`" + value + "'");
				prefs.remove(featureParam);
				prefs.put(featureParam, value);
			}
		}

		Log.i(TAG, "Initializing features");
		_onFeatureInitialized.setTimeout(getPrefs().getInt(Param.FEATURE_INITIALIZE_TIMEOUT));
		_onFeatureInitialized.initializeNext();
	}

	/**
	 * Stops the timeout during feature initializations.
	 * This method must be called from IFeature.initialize before
	 * the response of the onInitialized callback.
	 *
	 * @param isEnabled
	 */
	public void stopInitTimeout()
	{
		Log.i(TAG, ".stopInitTimeout");
		_onFeatureInitialized.stopTimeout();
	}

	private class FeatureInitializeCallBack implements Runnable, IFeature.OnFeatureInitialized
	{
		private int _nFeature = -1;
		private int _nInitialized = -1;
		private long _initStartedTime;
		private int _timeout = 0;

		public void setTimeout(int timeout)
		{
			_timeout = timeout;
		}

		public void stopTimeout()
		{
			Log.i(TAG, ".stopTimeout");
			_eventMessenger.removeCallbacks(this);
		}

		public void startTimeout()
		{
			Log.i(TAG, ".startTimeout");
			_eventMessenger.removeCallbacks(this);
			_eventMessenger.postDelayed(this, _timeout * 1000);
		}

		// return true if there are more features to initialize or false
		// otherwise
		public void initializeNext()
		{
			if ((_nFeature + 1) < _features.size())
			{
				_nFeature++;
				_initStartedTime = System.currentTimeMillis();
				final IFeature feature = _features.get(_nFeature);
				Log.i(TAG, ">" + _nFeature + ". Initializing " + feature.getName() + " " + feature.getType() + " ("
				        + feature.getClass().getName() + ") with timeout " + _timeout + " secs");

				startTimeout();
				feature.initialize(this);
			}
			else
			{
				_eventMessenger.trigger(ON_LOADED);
				_isInitialized = true;
			}
		}

		@Override
		public void run()
		{
			// Initialization timed out
			IFeature feature = _features.get(_nFeature);
			Log.e(TAG, _nFeature + ". initialize " + (System.currentTimeMillis() - _initStartedTime) + " ms: "
			        + feature.getName() + " " + feature.getType() + " timeout!");
			throw new RuntimeException("timeout!");
		}

		@Override
		public void onInitialized(IFeature feature, int resultCode)
		{
			stopTimeout();
			onInitializeProgress(feature, 1.0f);
			String featureName = feature.getName() + " " + feature.getType();
			if (_nInitialized == _nFeature)
			{
				throw new RuntimeException("Attempt to initialize feature " + featureName + " more than once");
			}
			_nInitialized = _nFeature;
			Log.i(TAG, "<" + _nFeature + ". " + featureName + " initialized in "
			        + (System.currentTimeMillis() - _initStartedTime) + " ms with result " + resultCode);
			initializeNext();
		}

		@Override
		public void onInitializeProgress(IFeature feature, float progress)
		{
			float totalProgress = (_nFeature + progress) / _features.size();

			Bundle bundle = new Bundle();
			bundle.putFloat("totalProgress", totalProgress);
			bundle.putFloat("featureProgress", progress);
			bundle.putString("featureName", feature.getType().name() + " " + feature.getName());
			_eventMessenger.trigger(ON_LOADING, bundle);
		}
	}

	/**
	 * @return application resources
	 */
	public Resources getResources()
	{
		return _activity.getResources();
	}

	/**
	 * sets the only application activity
	 *
	 * @param activity
	 *            application activity
	 */
	public void setActivity(Activity activity)
	{
		_activity = activity;
	}

	/**
	 * @return the only application activity
	 */
	public Activity getActivity()
	{
		return _activity;
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
	 * Sets global state manager
	 *
	 * @param StateManager
	 */
	public void setStateManager(StateManager stateManager)
	{
		_stateManager = stateManager;
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
	 * @return
	 * @throws NameNotFoundException
	 */
	public String getBuildVersion()
	{
		try
		{
			return getActivity().getPackageManager().getPackageInfo(getActivity().getApplication().getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param featureName
	 * @return true if the feature component exists
	 */
	public boolean hasFeature(FeatureName.Component featureName)
	{
		try
		{
			// Check if feature exists
			getFeatureComponent(featureName);
			return true;
		}
		catch (FeatureNotFoundException e)
		{
			return false;
		}
	}

	/**
	 * @param featureName
	 * @return true if the feature scheduler exists
	 */
	public boolean hasFeature(FeatureName.Scheduler featureName)
	{
		try
		{
			// Check if feature exists
			getFeatureScheduler(featureName);
			return true;
		}
		catch (FeatureNotFoundException e)
		{
			return false;
		}
	}

	/**
	 * @param featureName
	 * @return true if the feature state exists
	 */
	public boolean hasFeature(FeatureName.State featureName)
	{
		try
		{
			// Check if feature exists
			getFeatureState(featureName);
			return true;
		}
		catch (FeatureNotFoundException e)
		{
			return false;
		}
	}

	/**
	 * Declare special feature referred by its class implementation
	 *
	 * @param featureClass
	 *            The class of the special feature
	 * @return feature instance
	 */
	public IFeature use(Class<?> featureClass) throws FeatureNotFoundException
	{
		try
		{
			// Check if feature is already used
			return getFeature(featureClass);
		}
		catch (FeatureNotFoundException fe)
		{
			IFeature feature = null;
			if (featureClass.isAssignableFrom(IFeature.class))
			{
				throw new FeatureNotFoundException("Feature " + featureClass.getName() + " must implement IFeature");
			}

			try
			{
				feature = (IFeature) featureClass.newInstance();
				useDependencies(feature);
				_features.add(feature);
			}
			catch (InstantiationException e)
			{
				throw new FeatureNotFoundException(e);
			}
			catch (IllegalAccessException e)
			{
				throw new FeatureNotFoundException(e);
			}
			return feature;
		}
	}

	/**
	 * Declare component feature to be used
	 *
	 * @param featureName
	 * @return The used FeatureComponent
	 * @throws FeatureNotFoundException
	 */
	public FeatureComponent use(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		Log.i(TAG, ".use: Component " + featureName);
		try
		{
			// Check if feature is already used
			return getFeatureComponent(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			FeatureComponent feature = _featureFactory.createComponent(featureName);
			useDependencies(feature);
			_features.add(feature);
			return feature;
		}
	}

	/**
	 * Declare scheduler feature to be used
	 *
	 * @param featureName
	 * @return the used FeatureScheduler
	 * @throws FeatureNotFoundException
	 */
	public FeatureScheduler use(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		Log.i(TAG, ".use: Scheduler " + featureName);

		try
		{
			// Check if feature is already used
			return getFeatureScheduler(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			FeatureScheduler feature = _featureFactory.createScheduler(featureName);
			useDependencies(feature);
			_features.add(feature);
			return feature;
		}
	}

	/**
	 * Declare state feature to be used. The first used state feature will be
	 * used as home.
	 *
	 * @param featureName
	 * @return the used FeatureState
	 * @throws FeatureNotFoundException
	 */
	public FeatureState use(FeatureName.State featureName) throws FeatureNotFoundException
	{
		Log.i(TAG, ".use: State " + featureName);
		if (_featureFactory == null)
			throw new RuntimeException("Set IFeatureFactory with setFeatureFactory before declaring feature usages");

		try
		{
			// Check if feature is already used
			return getFeatureState(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			// Use feature
			FeatureState feature = _featureFactory.createState(featureName);
			useDependencies(feature);
			_features.add(feature);
			return feature;
		}
	}

	/**
	 * Get feature by specifying its class implementation
	 *
	 * @param featureClass
	 *            The class of the feature
	 * @return feature instance
	 */
	public IFeature getFeature(Class<?> featureClass) throws FeatureNotFoundException
	{
		for (IFeature feature : _features)
		{
			if (featureClass.isInstance(feature))
				return feature;
		}
		throw new FeatureNotFoundException(featureClass.getName());
	}

	/**
	 * @param featureName
	 * @return FeatureComponent
	 * @throws FeatureNotFoundException
	 */
	public FeatureComponent getFeatureComponent(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		for (IFeature feature : _features)
		{
			if (IFeature.Type.COMPONENT.equals(feature.getType()))
			{
				FeatureComponent component = (FeatureComponent) feature;
				if (featureName.equals(component.getComponentName()))
					return component;
			}
		}
		throw new FeatureNotFoundException(featureName);
	}

	/**
	 * @param featureName
	 * @return FeatureScheduler
	 * @throws FeatureNotFoundException
	 */
	public FeatureScheduler getFeatureScheduler(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		for (IFeature feature : _features)
		{
			if (IFeature.Type.SCHEDULER.equals(feature.getType()))
			{
				FeatureScheduler scheduler = (FeatureScheduler) feature;
				if (featureName.equals(scheduler.getSchedulerName()))
					return scheduler;
			}
		}
		throw new FeatureNotFoundException(featureName);
	}

	/**
	 * @param featureName
	 * @return FeatureState
	 * @throws FeatureNotFoundException
	 */
	public FeatureState getFeatureState(FeatureName.State featureName) throws FeatureNotFoundException
	{
		for (IFeature feature : _features)
		{
			if (IFeature.Type.STATE.equals(feature.getType()))
			{
				FeatureState state = (FeatureState) feature;
				if (featureName.equals(state.getStateName()))
					return state;
			}
		}
		throw new FeatureNotFoundException(featureName);
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

	public void setFeatureFactory(IFeatureFactory featureFactory)
	{
		_featureFactory = featureFactory;
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
		Intent intent = _activity.getPackageManager().getLaunchIntentForPackage(packageName);
		if (intent == null)
		{
			Log.w(getClass().getSimpleName(), "Can't find pacakge `" + packageName + "'");
		}
		else
		{
			_activity.startActivity(intent);
		}
	}

	/**
	 * Inject key press in the environment
	 */
	/* package */boolean onKeyDown(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyDown: key = " + keyEvent);
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_KEY, keyEvent.Code.name());
		bundle.putInt(EXTRA_KEYCODE, keyEvent.Event.getKeyCode());
		boolean consumed = _stateManager.onKeyDown(keyEvent);
		bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
		_eventMessenger.trigger(ON_KEY_PRESSED, bundle);
		return consumed;
	}

	/**
	 * Inject key release in the environment
	 */
	/* package */boolean onKeyUp(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyUp: key = " + keyEvent);
		Bundle bundle = new Bundle();
		bundle.putString(EXTRA_KEY, keyEvent.Code.name());
		bundle.putInt(EXTRA_KEYCODE, keyEvent.Event.getKeyCode());
		boolean consumed = _stateManager.onKeyUp(keyEvent);
		bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
		_eventMessenger.trigger(ON_KEY_RELEASED, bundle);
		return consumed;
	}

	private void useDependencies(IFeature feature) throws FeatureNotFoundException
	{
		FeatureSet deps = feature.dependencies();
		if (deps == null)
			return;
		for (Class<?> featureClass : deps.Specials)
		{
			use(featureClass);
		}
		for (FeatureName.Component featureName : deps.Components)
		{
			use(featureName);
		}
		for (FeatureName.Scheduler featureName : deps.Schedulers)
		{
			use(featureName);
		}
		for (FeatureName.State featureName : deps.States)
		{
			use(featureName);
		}
	}

	private List<IFeature> topologicalSort(List<IFeature> features)
	{
		List<IFeature> sorted = new ArrayList<IFeature>();
		int featureCount = features.size();

		Log.v(TAG, ".topologicalSort: " + featureCount + " features");
		while (sorted.size() < featureCount)
		{
			int prevSortedSize = sorted.size();

			// remove all independent features
			for (IFeature feature : features)
			{
				int resolvedCounter;

				if (feature.dependencies() != null)
				{
					// check special features dependencies
					resolvedCounter = feature.dependencies().Specials.size();
					for (Class<?> special : feature.dependencies().Specials)
					{
						for (IFeature sortedFeature : sorted)
						{
							if (special.isInstance(sortedFeature))
							{
								resolvedCounter--;
								break;
							}
						}
					}
					if (resolvedCounter > 0) // has unresolved dependencies
						continue;

					// check component dependencies
					resolvedCounter = feature.dependencies().Components.size();
					for (FeatureName.Component component : feature.dependencies().Components)
					{
						for (IFeature sortedFeature : sorted)
						{
							if (IFeature.Type.COMPONENT.equals(sortedFeature.getType()))
								if (component.equals(((FeatureComponent) sortedFeature).getComponentName()))
								{
									resolvedCounter--;
									break;
								}
						}
					}
					if (resolvedCounter > 0) // has unresolved dependencies
						continue;

					// check scheduler dependencies
					resolvedCounter = feature.dependencies().Schedulers.size();
					for (FeatureName.Scheduler scheduler : feature.dependencies().Schedulers)
					{
						for (IFeature sortedFeature : sorted)
						{
							if (IFeature.Type.SCHEDULER.equals(sortedFeature.getType()))
								if (scheduler.equals(((FeatureScheduler) sortedFeature).getSchedulerName()))
								{
									resolvedCounter--;
									break;
								}
						}
					}
					if (resolvedCounter > 0) // has unresolved dependencies
						continue;

					// check state dependencies
					resolvedCounter = feature.dependencies().States.size();
					for (FeatureName.State state : feature.dependencies().States)
					{
						for (IFeature sortedFeature : sorted)
						{
							if (IFeature.Type.STATE.equals(sortedFeature.getType()))
								if (state.equals(((FeatureState) sortedFeature).getStateName()))
								{
									resolvedCounter--;
									break;
								}
						}
					}
					if (resolvedCounter > 0) // has unresolved dependencies
						continue;

				}

				if (sorted.indexOf(feature) < 0)
				{
					Log.i(TAG, sorted.size() + ". " + feature);
					sorted.add(feature);
				}
			}
			if (prevSortedSize == sorted.size())
				throw new RuntimeException("Internal error. Unable to sort features!");
		}
		return sorted;
	}

	private Prefs createUserPrefs()
	{
		Log.i(TAG, ".createUserPrefs");
		return new Prefs(_activity.getSharedPreferences("user", Activity.MODE_PRIVATE), true);
	}

	private Prefs createPrefs(String name)
	{
		Log.i(TAG, ".createPrefs: name = " + name);
		return new Prefs(_activity.getSharedPreferences(name, Activity.MODE_PRIVATE), false);
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
			put(url, bitmap);
		}
	}
}
