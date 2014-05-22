/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureManager.java
 * Author:      alek
 * Date:        2 May 2014
 * Description: Features management utility class
 */

package com.aviq.tv.android.sdk.core.feature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.IFeature.OnFeatureInitialized;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Features management utility class
 */
public class FeatureManager
{
	public static final String TAG = FeatureManager.class.getSimpleName();
	public static final int FEATURE_DEFAULT_INIT_TIMEOUT = 120;
	private List<IFeature> _features = new ArrayList<IFeature>();
	private final FeatureInitializer _featureInitializer = new FeatureInitializer();
	private FeatureFactoryCustom _featureFactory = new FeatureFactoryCustom();

	/*
	 * Public methods
	 * *********************
	 */

	/**
	 *
	 */
	public FeatureManager()
	{
	}

	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, "Sorting features topologically based on their declared dependencies");
		topologicalSort();

		Log.i(TAG, "Initializing features");
		_featureInitializer.setOnFeatureInitialized(onFeatureInitialized);
		_featureInitializer.initialize();
	}

	/**
	 * Stops the timeout during feature initializations.
	 * This method must be called from IFeature.initialize before
	 * the response of the onInitialized callback.
	 */
	public void stopInitTimeout()
	{
		Log.i(TAG, ".stopInitTimeout");
		_featureInitializer.stopTimeout();
	}

	/**
	 * Sets timeout for feature initialization.
	 *
	 * @param initTimeout
	 */
	public void setInitTimeout(int initTimeout)
	{
		Log.i(TAG, ".setInitTimeout: initTimeout = " + initTimeout);
		_featureInitializer.setTimeout(initTimeout);
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

	/**
	 * @throws IOException
	 *             Initializes the environment by AVIQTV xml
	 * @param inputSource
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addFeaturesFromXml(InputStream inputStream, final OnResultReceived resultReceived) throws SAXException,
	        IOException
	{
		Log.i(TAG, ".addFeaturesFromXml");
		InputSource inputSource = new InputSource();
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser;
		try
		{
			parser = parserFactory.newSAXParser();
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
		}
		XMLReader reader = parser.getXMLReader();
		final AVIQTVXmlHandler aviqtvXMLHandler = new AVIQTVXmlHandler();
		reader.setContentHandler(aviqtvXMLHandler);
		inputSource.setByteStream(inputStream);
		reader.parse(inputSource);

		new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (aviqtvXMLHandler.getIncludeUrls().size() > 0)
				{
					final URL url = aviqtvXMLHandler.getIncludeUrls().remove(0);
					final OnResultReceived _this = this;
					Log.i(TAG, "Parsing " + url);
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								final InputStream inputStream = url.openConnection().getInputStream();
								final String xml = TextUtils.inputStreamToString(inputStream);
								inputStream.close();
								Environment.getInstance().runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										try
										{
											InputStream stream = new ByteArrayInputStream(xml.getBytes());
											addFeaturesFromXml(stream, _this);
										}
										catch (SAXException e)
										{
											Log.e(TAG, e.getMessage(), e);
											resultReceived.onReceiveResult(ResultCode.PROTOCOL_ERROR, null);
										}
										catch (IOException e)
										{
											Log.e(TAG, e.getMessage(), e);
											resultReceived.onReceiveResult(ResultCode.GENERAL_FAILURE, null);
										}
									}
								});
							}
							catch (IOException e)
							{
								Log.e(TAG, e.getMessage(), e);
								onResult(ResultCode.GENERAL_FAILURE);
							}
						}

						private void onResult(final int resultCode)
						{
							Environment.getInstance().runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									resultReceived.onReceiveResult(resultCode, null);
								}
							});
						}

					}).start();
				}
				else
				{
					resultReceived.onReceiveResult(ResultCode.OK, null);
				}
			}
		}.onReceiveResult(ResultCode.OK, null);
	}

	/**
	 * @throws IOException
	 *             Initializes the environment by AVIQTV xml
	 * @param inputSource
	 * @throws SAXException
	 * @throws IOException
	 */
	public void addFeaturesFromUrl(final URL url, final OnResultReceived resultReceived) throws SAXException,
	        IOException
	{
		new Thread(new Runnable()
		{
			private void onResult(final int resultCode)
			{
				Environment.getInstance().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						resultReceived.onReceiveResult(resultCode, null);
					}
				});
			}

			@Override
			public void run()
			{
				try
				{
					addFeaturesFromXml(url.openConnection().getInputStream(), resultReceived);
					onResult(ResultCode.OK);
				}
				catch (SAXException e)
				{
					onResult(ResultCode.PROTOCOL_ERROR);
				}
				catch (IOException e)
				{
					onResult(ResultCode.GENERAL_FAILURE);
				}
			}
		}).start();
	}

	/*
	 * Private methods
	 * *********************
	 */

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

	private void topologicalSort()
	{
		List<IFeature> sorted = new ArrayList<IFeature>();
		int featureCount = _features.size();

		Log.v(TAG, ".topologicalSort: " + featureCount + " features");
		while (sorted.size() < featureCount)
		{
			int prevSortedSize = sorted.size();

			// remove all independent features
			for (IFeature feature : _features)
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

		_features = sorted;
	}

	private class FeatureInitializer implements Runnable, OnFeatureInitialized
	{
		// The current feature number being initialized
		private int _featureNumber;

		// Last feature number being initialized used to detect if a feature has
		// been initialized twice
		private int _lastFeatureNumber;

		// used to compute feature initialization time
		private long _initStartedTime;

		// the allowed time for feature initialization
		private int _timeout = 0;

		private OnFeatureInitialized _onFeatureInitialized;
		private Handler _handler = new Handler();

		public void setTimeout(int timeout)
		{
			_timeout = timeout;
		}

		public void setOnFeatureInitialized(OnFeatureInitialized onFeatureInitialized)
		{
			_onFeatureInitialized = onFeatureInitialized;
		}

		public void stopTimeout()
		{
			Log.i(TAG, ".stopTimeout");
			_handler.removeCallbacks(this);
		}

		public void startTimeout()
		{
			Log.i(TAG, ".startTimeout");
			_handler.removeCallbacks(this);
			_handler.postDelayed(this, _timeout * 1000);
		}

		public void initialize()
		{
			_featureNumber = _lastFeatureNumber = -1;
			initializeNext();
		}

		// return true if there are more features to initialize or false
		// otherwise
		private void initializeNext()
		{
			if ((_featureNumber + 1) < _features.size())
			{
				_featureNumber++;
				_initStartedTime = System.currentTimeMillis();
				final IFeature feature = _features.get(_featureNumber);
				Log.i(TAG, ">" + _featureNumber + ". Initializing " + feature + " (" + feature.getClass().getName()
				        + ") with timeout " + _timeout + " secs");

				startTimeout();

				// initializing next feature
				_handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							feature.setDependencyFeatures(new Features(feature.dependencies()));
							feature.initialize(FeatureInitializer.this);
						}
						catch (FeatureNotFoundException e)
						{
							_onFeatureInitialized.onInitialized(feature, ResultCode.FEATURE_NOT_FOUND);
						}
					}
				});
			}
			else
			{
				_onFeatureInitialized.onInitialized(null, ResultCode.OK);
			}
		}

		@Override
		public void run()
		{
			// Initialization timed out
			IFeature feature = _features.get(_featureNumber);
			Log.e(TAG, _featureNumber + ". initialize " + (System.currentTimeMillis() - _initStartedTime) + " ms: "
			        + feature + " timeout!");
			_onFeatureInitialized.onInitialized(feature, ResultCode.TIMEOUT);
		}

		@Override
		public void onInitialized(IFeature feature, int resultCode)
		{
			stopTimeout();
			onInitializeProgress(feature, 1.0f);
			if (_lastFeatureNumber == _featureNumber)
			{
				throw new RuntimeException("Internal Error: Attempt to initialize feature " + feature
				        + " more than once");
			}
			_lastFeatureNumber = _featureNumber;
			Log.i(TAG, "<" + _featureNumber + ". " + feature + " initialized in "
			        + (System.currentTimeMillis() - _initStartedTime) + " ms with result " + resultCode);

			// Stop all features initialization when one fails to initialize
			if (resultCode != ResultCode.OK)
			{
				_onFeatureInitialized.onInitialized(feature, resultCode);
			}
			else
			{
				initializeNext();
			}
		}

		@Override
		public void onInitializeProgress(IFeature feature, float progress)
		{
			float totalProgress = (_featureNumber + progress) / _features.size();
			Log.i(TAG, "load progress -> " + totalProgress);
			_onFeatureInitialized.onInitializeProgress(feature, totalProgress);
		}
	}

	private class AVIQTVXmlHandler extends DefaultHandler
	{
		private static final String TAG_INCLUDE = "include";
		private static final String ATTR_SRC = "src";
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
		private List<URL> _includeUrls = new ArrayList<URL>();

		public List<URL> getIncludeUrls()
		{
			return _includeUrls;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{
			if (TAG_INCLUDE.equalsIgnoreCase(localName))
			{
				try
				{
					URL url = new URL(attributes.getValue(ATTR_SRC));
					_includeUrls.add(url);
				}
				catch (MalformedURLException e)
				{
					throw new SAXException(e);
				}
			}
			else if (TAG_FEATURES.equalsIgnoreCase(localName))
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
					prefs = Environment.getInstance().getPrefs();
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
					prefs = Environment.getInstance().getPrefs();
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
					prefs = Environment.getInstance().getPrefs();
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