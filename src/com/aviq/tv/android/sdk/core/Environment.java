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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.Volley;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureFactoryCustom;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureSet;
import com.aviq.tv.android.sdk.core.feature.FeatureState;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.state.StateManager;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.system.FeatureSystem;

/**
 * Defines application environment
 */
public class Environment extends Activity
{
	public static final String TAG = Environment.class.getSimpleName();
	public static final int ON_INITIALIZE = EventMessenger.ID("ON_INITIALIZE");
	public static final int ON_LOADING = EventMessenger.ID("ON_LOADING");
	public static final int ON_LOADED = EventMessenger.ID("ON_LOADED");
	public static final int ON_KEY_PRESSED = EventMessenger.ID("ON_KEY_PRESSED");
	public static final int ON_KEY_RELEASED = EventMessenger.ID("ON_KEY_RELEASED");
	public static final int ON_FEATURE_INIT_ERROR = EventMessenger.ID("ON_FEATURE_INIT_ERROR");
	public static final String EXTRA_ERROR_CODE = "EXTRA_ERROR_CODE";
	public static final String EXTRA_KEY = "KEY";
	public static final String EXTRA_KEYCODE = "KEYCODE";
	public static final String EXTRA_KEYCONSUMED = "KEYCONSUMED";
	private static final String SYSTEM_PREFS = "system";
	private static final String AVIQTV_XML_RESOURCE = "aviqtv";
	private static final String ECLIPSE_XML_RESOURCE = "eclipse";
	private static final String RELEASE_XML_RESOURCE = "release";

	public enum Param
	{
		/**
		 * whether we are in release build
		 */
		RELEASE("devel"),

		/**
		 * Timeout in seconds for feature initialization
		 */
		FEATURE_INITIALIZE_TIMEOUT(120),

		/**
		 * The overlay background color
		 */
		OVERLAY_BACKGROUND_COLOR(0x00000000), ;

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
	private Prefs _prefs;
	private Prefs _userPrefs;
	private final List<Properties> _brandPropertyLists = new ArrayList<Properties>();
	private RequestQueue _requestQueue;
	private ImageLoader _imageLoader;
	private List<IFeature> _features = new ArrayList<IFeature>();
	private final EventMessenger _eventMessenger = new EventMessenger();
	private final Map<FeatureName.Component, Prefs> _componentPrefs = new HashMap<FeatureName.Component, Prefs>();
	private final Map<FeatureName.Scheduler, Prefs> _schedulerPrefs = new HashMap<FeatureName.Scheduler, Prefs>();
	private final Map<FeatureName.State, Prefs> _statePrefs = new HashMap<FeatureName.State, Prefs>();
	private FeatureFactoryCustom _featureFactory = new FeatureFactoryCustom();
	// Chain based features initializer
	private final FeatureInitializeCallBack _onFeatureInitialized = new FeatureInitializeCallBack();
	private boolean _isInitialized = false;
	private static boolean _isCreated = false;
	private FeatureRCU _featureRCU;
	private FeatureSystem _featureSystem;

	/**
	 * Environment constructor method
	 */
	public Environment()
	{
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
				String warnMsg = "Using debug xml definition - " + ECLIPSE_XML_RESOURCE;
				Log.w(TAG, String.format(String.format("%%0%dd", warnMsg.length()), 0).replace("0", "-"));
				Log.w(TAG, warnMsg);
				Log.w(TAG, String.format(String.format("%%0%dd", warnMsg.length()), 0).replace("0", "-"));

				// initialize environment by debug app's raw/eclipse.xml
				Log.i(TAG, "Parsing " + ECLIPSE_XML_RESOURCE + " xml definition");
				InputStream inputStream = getResources().openRawResource(appDebugXmlId);
				addXmlDefinition(inputStream);
			}
			else
			{
				// initialize environment by default app's raw/aviqtv.xml
				Log.i(TAG, "Parsing " + AVIQTV_XML_RESOURCE + " xml definition");
				int appXmlId = getResources().getIdentifier(AVIQTV_XML_RESOURCE, "raw", getPackageName());
				InputStream inputStream = getResources().openRawResource(appXmlId);
				addXmlDefinition(inputStream);

				// initialize environment by app's raw/release.xml
				Log.i(TAG, "Parsing " + RELEASE_XML_RESOURCE + " xml definition");
				appXmlId = getResources().getIdentifier(RELEASE_XML_RESOURCE, "raw", getPackageName());
				inputStream = getResources().openRawResource(appXmlId);
				addXmlDefinition(inputStream);
			}

			// Log target device parameters
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			Log.i(TAG, "Initializing environment: " + metrics.widthPixels + "x" + metrics.heightPixels + ", density = "
			        + metrics.density + ", densityDpi = " + metrics.densityDpi + ", scaledDensity = "
			        + metrics.scaledDensity + ", xdpi = " + metrics.xdpi + ", ydpi = " + metrics.ydpi);

			// initializes environment context
			_stateManager = new StateManager(this);
			_stateManager.setMessageState(use(FeatureName.State.MESSAGE_BOX));
			_stateManager.setOverlayBackgroundColor(getPrefs().getInt(Param.OVERLAY_BACKGROUND_COLOR));
			_featureRCU = (FeatureRCU) use(FeatureName.Component.RCU);
			_featureSystem = (FeatureSystem) use(FeatureName.Component.SYSTEM);

			_serviceController = new ServiceController(this);
			_requestQueue = Volley.newRequestQueue(this);
			_requestQueue.getCache().clear();

			// Use 1/8th of the available memory for this memory cache.
			int memClass = ((ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE))
			        .getMemoryClass();
			int cacheSize = 1024 * 1024 * memClass / 8;
			_imageLoader = new ImageLoader(_requestQueue, new BitmapLruCache(cacheSize));

			// initializes features
			Log.i(TAG, "Sorting features topologically based on their declared dependencies");
			_features = topologicalSort(_features);

			Log.i(TAG, "Initializing features");
			_onFeatureInitialized.setTimeout(getPrefs().getInt(Param.FEATURE_INITIALIZE_TIMEOUT));

			initialize();
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
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
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Log.i(TAG, ".onPause");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG, ".onDestroy");

		_featureSystem.command("killall " + getPackageName());
	}

	public void initialize()
	{
		if (_isInitialized)
			throw new RuntimeException("Environment is already initialized!");
		Log.i(TAG, ".initialize");
		_onFeatureInitialized.initialize();
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

	/**
	 * @throws IOException
	 *             Initializes the environment by AVIQTV xml
	 * @param inputSource
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addXmlDefinition(InputStream inputStream) throws SAXException, IOException
	{
		try
		{
			InputSource inputSource = new InputSource();
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			AVIQTVXmlHandler aviqtvXMLHandler = new AVIQTVXmlHandler();
			reader.setContentHandler(aviqtvXMLHandler);
			inputSource.setByteStream(inputStream);
			reader.parse(inputSource);
		}
		catch (ParserConfigurationException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
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
		private int _nFeature;
		private int _nInitialized;
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

		public void initialize()
		{
			_nFeature = _nInitialized = -1;

			getEventMessenger().trigger(ON_INITIALIZE);
			initializeNext();
		}

		// return true if there are more features to initialize or false
		// otherwise
		private void initializeNext()
		{
			if ((_nFeature + 1) < _features.size())
			{
				_nFeature++;
				_initStartedTime = System.currentTimeMillis();
				final IFeature feature = _features.get(_nFeature);
				Log.i(TAG, ">" + _nFeature + ". Initializing " + feature.getName() + " " + feature.getType() + " ("
				        + feature.getClass().getName() + ") with timeout " + _timeout + " secs");

				startTimeout();

				// initializing next feature
				getEventMessenger().post(new Runnable()
				{
					@Override
					public void run()
					{
						feature.initialize(FeatureInitializeCallBack.this);
					}
				});
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

			// Stop all features initialization when any one fails to initialize
			if (resultCode != ResultCode.OK)
			{
				Bundle bundle = new Bundle();
				bundle.putInt(EXTRA_ERROR_CODE, resultCode);
				_eventMessenger.trigger(ON_FEATURE_INIT_ERROR, bundle);
				return;
			}

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
			return getPackageManager().getPackageInfo(getApplication().getPackageName(), 0).versionName;
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
		if (_featureFactory == null)
			throw new RuntimeException("Set IFeatureFactory with setFeatureFactory before using features");

		try
		{
			// Check if feature is already used
			return getFeature(featureClass);
		}
		catch (FeatureNotFoundException fe)
		{
			Log.i(TAG, ".use: " + featureClass.getName());
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
		if (_featureFactory == null)
			throw new RuntimeException("Set IFeatureFactory with setFeatureFactory before using features");

		try
		{
			// Check if feature is already used
			return getFeatureComponent(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			Log.i(TAG, ".use: Component " + featureName);
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
		if (_featureFactory == null)
			throw new RuntimeException("Set IFeatureFactory with setFeatureFactory before using features");

		try
		{
			// Check if feature is already used
			return getFeatureScheduler(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			Log.i(TAG, ".use: Scheduler " + featureName);
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
		if (_featureFactory == null)
			throw new RuntimeException("Set IFeatureFactory with setFeatureFactory before using features");

		try
		{
			// Check if feature is already used
			return getFeatureState(featureName);
		}
		catch (FeatureNotFoundException e)
		{
			Log.i(TAG, ".use: State " + featureName);

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
		Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
		if (intent == null)
		{
			Log.w(getClass().getSimpleName(), "Can't find pacakge `" + packageName + "'");
		}
		else
		{
			startActivity(intent);
		}
	}

	public boolean isDevel()
	{
		return "devel".equalsIgnoreCase(getPrefs().getString(Param.RELEASE));
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

		if (_isInitialized)
		{
			bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
			_eventMessenger.trigger(ON_KEY_PRESSED, bundle);
		}

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
		if (_isInitialized)
		{
			bundle.putBoolean(EXTRA_KEYCONSUMED, consumed);
			_eventMessenger.trigger(ON_KEY_RELEASED, bundle);
		}
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
				int resolvedPosition = -1;

				if (feature.dependencies() != null)
				{
					// check special features dependencies
					resolvedCounter = feature.dependencies().Specials.size();
					for (Class<?> special : feature.dependencies().Specials)
					{
						for (int i = 0; i < sorted.size(); i++)
						{
							IFeature sortedFeature = sorted.get(i);
							if (special.isInstance(sortedFeature))
							{
								resolvedCounter--;
								if (i > resolvedPosition)
									resolvedPosition = i;
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
						for (int i = 0; i < sorted.size(); i++)
						{
							IFeature sortedFeature = sorted.get(i);
							if (IFeature.Type.COMPONENT.equals(sortedFeature.getType()))
								if (component.equals(((FeatureComponent) sortedFeature).getComponentName()))
								{
									resolvedCounter--;
									if (i > resolvedPosition)
										resolvedPosition = i;
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
						for (int i = 0; i < sorted.size(); i++)
						{
							IFeature sortedFeature = sorted.get(i);
							if (IFeature.Type.SCHEDULER.equals(sortedFeature.getType()))
								if (scheduler.equals(((FeatureScheduler) sortedFeature).getSchedulerName()))
								{
									resolvedCounter--;
									if (i > resolvedPosition)
										resolvedPosition = i;
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
						for (int i = 0; i < sorted.size(); i++)
						{
							IFeature sortedFeature = sorted.get(i);
							if (IFeature.Type.STATE.equals(sortedFeature.getType()))
								if (state.equals(((FeatureState) sortedFeature).getStateName()))
								{
									resolvedCounter--;
									if (i > resolvedPosition)
										resolvedPosition = i;
									break;
								}
						}
					}
					if (resolvedCounter > 0) // has unresolved dependencies
						continue;
				}

				if (sorted.indexOf(feature) < 0)
				{
					if (feature.getClass().getAnnotation(PriorityFeature.class) != null)
					{
						resolvedPosition++;
						if (resolvedPosition < sorted.size())
							sorted.add(resolvedPosition, feature);
						else
							sorted.add(feature);

						Log.i(TAG, "Feature " + feature + " is prioritized and shifted to position " + resolvedPosition);
					}
					else
					{
						sorted.add(feature);
					}
				}
			}
			if (prevSortedSize == sorted.size())
				throw new RuntimeException("Internal error. Unable to sort features!");
		}

		for (int i = 0; i < sorted.size(); i++)
		{
			IFeature feature = sorted.get(i);
			Log.i(TAG, i + ". " + feature);
		}

		return sorted;
	}

	private Prefs createUserPrefs()
	{
		Log.i(TAG, ".createUserPrefs");
		return new Prefs("user", getSharedPreferences("user", Activity.MODE_PRIVATE), true);
	}

	private Prefs createPrefs(String name)
	{
		Log.i(TAG, ".createPrefs: name = " + name);
		return new Prefs(name, getSharedPreferences(name, Activity.MODE_PRIVATE), false);
	}

	public Key translateKeyCode(int keyCode)
	{
		return _featureRCU.getKey(keyCode);
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

	private class AVIQTVXmlHandler extends DefaultHandler
	{
		private static final String TAG_AVIQTV = "aviqtv";
		private static final String TAG_FEATURES = "features";
		private static final String TAG_FEATURE = "feature";
		private static final String TAG_COMPNENT = "component";
		private static final String TAG_SCHEDULER = "scheduler";
		private static final String TAG_STATE = "state";
		private static final String TAG_USE = "use";
		private static final String TAG_STRING = "string";
		private static final String TAG_INT = "int";
		private static final String TAG_BOOLEAN = "boolean";
		private static final String ATTR_NAME = "name";
		private static final String ATTR_CLASS = "class";
		private static final String ATTR_VALUE = "value";
		private IFeature _feature;
		private boolean _inFactory;
		private boolean _inUse;
		private boolean _inString;
		private StringBuffer _stringValue = new StringBuffer();
		private String _paramName;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{
			if (TAG_FEATURES.equalsIgnoreCase(localName))
			{
				if (_inFactory)
					throw new SAXException("Nested tags " + TAG_FEATURE + " is not allowed");
				_inFactory = true;
			}
			else if (TAG_FEATURE.equalsIgnoreCase(localName))
			{
				if (!_inFactory)
					throw new SAXException("Tag " + TAG_FEATURE + " must be inside tag " + TAG_FEATURES);

				String className = attributes.getValue(ATTR_CLASS);
				try
				{
					_feature = (IFeature) Class.forName(className).newInstance();
				}
				catch (InstantiationException e)
				{
					throw new SAXException(e);
				}
				catch (IllegalAccessException e)
				{
					throw new SAXException(e);
				}
				catch (ClassNotFoundException e)
				{
					throw new SAXException(e);
				}

				_featureFactory.registerFeature(_feature);
			}
			else if (TAG_STRING.equalsIgnoreCase(localName))
			{
				if (_feature == null && !_inUse)
					throw new SAXException("Tag '" + TAG_STRING + "' must be inside tag '" + TAG_FEATURE + "'");
				_paramName = attributes.getValue(ATTR_NAME);
				_stringValue.setLength(0);
				_inString = true;
			}
			else if (TAG_INT.equalsIgnoreCase(localName))
			{
				if (_feature == null && !_inUse)
					throw new SAXException("Tag " + TAG_INT + " must be inside tag " + TAG_FEATURE);
				_paramName = attributes.getValue(ATTR_NAME);

				String featureName;
				Prefs prefs;
				if (_feature != null)
				{
					prefs = _feature.getPrefs();
					featureName = _feature.getName();
				}
				else
				{
					prefs = getPrefs();
					featureName = getClass().getSimpleName();
				}
				String sValue = attributes.getValue(ATTR_VALUE);
				long value = 0;

				try
				{
					value = Long.parseLong(sValue);
				}
				catch (NumberFormatException e)
				{
					throw new SAXException(e);
				}

				if (prefs.has(_paramName))
				{
					int prevValue = prefs.getInt(_paramName);
					if (prevValue != value)
					{
						Log.i(TAG, "Overwriting param " + featureName + "." + _paramName + ": " + prevValue + " -> "
						        + value);
						prefs.remove(_paramName);
						prefs.put(_paramName, (int) value);
					}
					else
					{
						Log.w(TAG, "No change to param " + featureName + "." + _paramName + " with value " + value);
					}
				}
				else
				{
					Log.i(TAG, "Add new param " + featureName + "." + _paramName + " = " + value);
					prefs.put(_paramName, (int) value);
				}
			}
			else if (TAG_BOOLEAN.equalsIgnoreCase(localName))
			{
				if (_feature == null && !_inUse)
					throw new SAXException("Tag " + TAG_BOOLEAN + " must be inside tag " + TAG_FEATURE);
				_paramName = attributes.getValue(ATTR_NAME);

				String featureName;
				Prefs prefs;
				if (_feature != null)
				{
					prefs = _feature.getPrefs();
					featureName = _feature.getName();
				}
				else
				{
					prefs = getPrefs();
					featureName = getClass().getSimpleName();
				}

				String sValue = attributes.getValue(ATTR_VALUE);
				boolean value = Boolean.parseBoolean(sValue);

				if (prefs.has(_paramName))
				{
					boolean prevValue = prefs.getBool(_paramName);
					if (prevValue != value)
					{
						Log.i(TAG, "Overwriting param " + featureName + "." + _paramName + ": " + prevValue + " -> "
						        + value);
						prefs.remove(_paramName);
						prefs.put(_paramName, value);
					}
					else
					{
						Log.w(TAG, "No change to param " + featureName + "." + _paramName + " with value " + value);
					}
				}
				else
				{
					Log.i(TAG, "Add new param " + featureName + "." + _paramName + " = " + value);
					prefs.put(_paramName, value);
				}
			}
			else if (TAG_USE.equalsIgnoreCase(localName))
			{
				_inUse = true;
			}
			else if (TAG_COMPNENT.equalsIgnoreCase(localName))
			{
				if (!_inUse)
					throw new SAXException("Tag " + TAG_COMPNENT + " must be inside tag " + TAG_USE);

				String name = attributes.getValue(ATTR_NAME);
				String className = attributes.getValue(ATTR_CLASS);

				try
				{
					if (name != null)
					{
						FeatureName.Component componentName = FeatureName.Component.valueOf(name);
						_feature = use(componentName);
					}
					else
					{
						_feature = use(Class.forName(className));
					}
				}
				catch (FeatureNotFoundException e)
				{
					throw new SAXException(e);
				}
				catch (ClassNotFoundException e)
				{
					throw new SAXException(e);
				}
			}
			else if (TAG_SCHEDULER.equalsIgnoreCase(localName))
			{
				if (!_inUse)
					throw new SAXException("Tag " + TAG_SCHEDULER + " must be inside tag " + TAG_USE);

				String name = attributes.getValue(ATTR_NAME);
				String className = attributes.getValue(ATTR_CLASS);
				try
				{
					if (name != null)
					{
						FeatureName.Scheduler schedulerName = FeatureName.Scheduler.valueOf(name);
						_feature = use(schedulerName);
					}
					else
					{
						_feature = use(Class.forName(className));
					}
				}
				catch (FeatureNotFoundException e)
				{
					throw new SAXException(e);
				}
				catch (ClassNotFoundException e)
				{
					throw new SAXException(e);
				}
			}
			else if (TAG_STATE.equalsIgnoreCase(localName))
			{
				if (!_inUse)
					throw new SAXException("Tag " + TAG_STATE + " must be inside tag " + TAG_USE);

				String name = attributes.getValue(ATTR_NAME);
				String className = attributes.getValue(ATTR_CLASS);
				try
				{
					if (name != null)
					{
						FeatureName.State stateName = FeatureName.State.valueOf(name);
						_feature = use(stateName);
					}
					else
					{
						_feature = use(Class.forName(className));
					}
				}
				catch (FeatureNotFoundException e)
				{
					throw new SAXException(e);
				}
				catch (ClassNotFoundException e)
				{
					throw new SAXException(e);
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			if (TAG_FEATURES.equalsIgnoreCase(localName))
			{
				_inFactory = false;
			}
			else if (TAG_FEATURE.equalsIgnoreCase(localName) || TAG_COMPNENT.equalsIgnoreCase(localName)
			        || TAG_SCHEDULER.equalsIgnoreCase(localName) || TAG_STATE.equalsIgnoreCase(localName))
			{
				_feature = null;
			}
			else if (TAG_STRING.equalsIgnoreCase(localName))
			{
				_inString = false;
				String featureName;
				Prefs prefs;
				if (_feature != null)
				{
					prefs = _feature.getPrefs();
					featureName = _feature.getName();
				}
				else
				{
					prefs = getPrefs();
					featureName = getClass().getSimpleName();
				}

				String value = _stringValue.toString();
				if (prefs.has(_paramName))
				{
					String prevValue = prefs.getString(_paramName);
					if (prevValue == null || !prevValue.equals(value))
					{
						Log.i(TAG, "Overwriting param " + featureName + "." + _paramName + ": " + prevValue + " -> "
						        + value);
						prefs.remove(_paramName);
						prefs.put(_paramName, value);
					}
					else
					{
						Log.w(TAG, "No change to param " + featureName + "." + _paramName + " with value " + value);
					}
				}
				else
				{
					Log.i(TAG, "Add new param " + featureName + "." + _paramName + " = " + value);
					prefs.put(_paramName, value);
				}
			}
			else if (TAG_USE.equalsIgnoreCase(localName))
			{
				_inUse = false;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			if (_inString)
				_stringValue.append(ch, start, length);
		}
	}
}
