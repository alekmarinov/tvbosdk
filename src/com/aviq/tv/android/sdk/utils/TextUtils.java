/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    TextUtils.java
 * Author:      Nadia
 * Date:        16 Jul 2013
 *
 * Description: Convenient text utilities
 */

package com.aviq.tv.android.sdk.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.util.Log;

public class TextUtils
{
	private static final String TAG = TextUtils.class.getSimpleName();

	/**
	 * Reads an InputStream and returns its contents as a String
	 *
	 * @param inputStream
	 *            The InputStream to read from.
	 * @return The contents of the InputStream as a String.
	 */
	public static String inputSteamToString(InputStream inputStream)
	{
		StringBuilder outputBuilder = new StringBuilder();
		BufferedReader reader = null;
		try
		{
			String string;
			if (inputStream != null)
			{
				reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				while (null != (string = reader.readLine()))
				{
					outputBuilder.append(string).append('\n');
				}
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}

		return outputBuilder.toString().trim();
	}

	/**
	 * Implodes bundle into StringBuffer
	 *
	 * @param buffer the StringBuffer to append into
	 * @param bundle the bundle to be imploded
	 * @param sepKey key from value character separator
	 * @param sepVal key/value pairs separator
	 * @return the same specified buffer
	 */
	public static StringBuffer implodeBundle(StringBuffer buffer, Bundle bundle, char sepKey, char sepVal)
	{
		boolean isSepVal = false;
		if (bundle != null)
			for (String key : bundle.keySet())
			{
				if (isSepVal)
					buffer.append(sepVal);
				buffer.append(key).append(sepKey).append(bundle.get(key));
				isSepVal = true;
			}
		if (isSepVal)
			buffer.append(sepVal);
		return buffer;
	}

	/**
	 * Implodes bundle to string
	 *
	 * @param bundle
	 * @return String with formatted bundle
	 */
	public static String implodeBundle(Bundle bundle, char sepKey, char sepVal)
	{
		StringBuffer buffer = new StringBuffer();
		implodeBundle(buffer, bundle, sepKey, sepVal);
		return buffer.toString();
	}

	/**
	 * Implodes bundle to string formatted as (key1:val1,key2:val2)
	 * @param bundle
	 * @return String with formatted bundle
	 */
	public static String implodeBundle(Bundle bundle)
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append('(');
		implodeBundle(buffer, bundle, ':', ',');
		buffer.append(')');
		return buffer.toString();
	}

	/**
	 * Substitutes all occurrences of ${<i>key</i>} with bundle.get(<i>key</i>)
	 * in the
	 * input string, for all <i>key</i> in bundle's key set
	 *
	 * @param input
	 *            the string in which to find ${<i>key</i>} occurrences
	 * @param bundle
	 *            Bundle with <i>key</i> -> <i>value</i> pairs
	 * @return the result string after all substitutions applied
	 */
	public static String substitute(String input, Bundle bundle)
	{
		for (String key : bundle.keySet())
		{
			input = input.replaceAll("\\$\\{" + key + "\\}", String.valueOf(bundle.get(key)));
		}
		return input;
	}

	/**
	 * <p>
	 * Random object used by random method. This has to be not local to the
	 * random method so as to not return the same value in the same millisecond.
	 * </p>
	 */
	private static final Random RANDOM = new Random();

	/**
	 * Returns the number of occurrences of substring represented as regular
	 * expression inside a given input string
	 *
	 * @param input
	 *            the string in which to find number of occurrences
	 * @param regex
	 *            the substring represented as regular expression
	 * @return number of occurrences of regex matching input
	 */
	public static int occurences(String input, String regex)
	{
		Pattern p = Pattern.compile(regex);

		// get a matcher object
		Matcher m = p.matcher(input);
		int count = 0;
		while (m.find())
		{
			count++;
		}
		return count;
	}

	/**
	 * <p>
	 * Creates a random string whose length is the number of characters
	 * specified.
	 * </p>
	 * <p>
	 * Characters will be chosen from the set of alphabetic characters.
	 * </p>
	 *
	 * @param count
	 *            the length of random string to create
	 * @return the random string
	 */
	public static String randomAlphabetic(int count)
	{
		return random(count, true, false);
	}

	/**
	 * <p>
	 * Creates a random string whose length is the number of characters
	 * specified.
	 * </p>
	 * <p>
	 * Characters will be chosen from the set of alpha-numeric characters as
	 * indicated by the arguments.
	 * </p>
	 *
	 * @param count
	 *            the length of random string to create
	 * @param letters
	 *            if <code>true</code>, generated string will include
	 *            alphabetic characters
	 * @param numbers
	 *            if <code>true</code>, generated string will include
	 *            numeric characters
	 * @return the random string
	 */
	public static String random(int count, boolean letters, boolean numbers)
	{
		return random(count, 0, 0, letters, numbers);
	}

	/**
	 * <p>
	 * Creates a random string whose length is the number of characters
	 * specified.
	 * </p>
	 * <p>
	 * Characters will be chosen from the set of alpha-numeric characters as
	 * indicated by the arguments.
	 * </p>
	 *
	 * @param count
	 *            the length of random string to create
	 * @param start
	 *            the position in set of chars to start at
	 * @param end
	 *            the position in set of chars to end before
	 * @param letters
	 *            if <code>true</code>, generated string will include
	 *            alphabetic characters
	 * @param numbers
	 *            if <code>true</code>, generated string will include
	 *            numeric characters
	 * @return the random string
	 */
	public static String random(int count, int start, int end, boolean letters, boolean numbers)
	{
		return random(count, start, end, letters, numbers, null, RANDOM);
	}

	/**
	 * <p>
	 * Creates a random string based on a variety of options, using supplied
	 * source of randomness.
	 * </p>
	 * <p>
	 * If start and end are both <code>0</code>, start and end are set to
	 * <code>' '</code> and <code>'z'</code>, the ASCII printable characters,
	 * will be used, unless letters and numbers are both <code>false</code>, in
	 * which case, start and end are set to <code>0</code> and
	 * <code>Integer.MAX_VALUE</code>.
	 * <p>
	 * If set is not <code>null</code>, characters between start and end are
	 * chosen.
	 * </p>
	 * <p>
	 * This method accepts a user-supplied {@link Random} instance to use as a
	 * source of randomness. By seeding a single {@link Random} instance with a
	 * fixed seed and using it for each call, the same random sequence of
	 * strings can be generated repeatedly and predictably.
	 * </p>
	 *
	 * @param count
	 *            the length of random string to create
	 * @param start
	 *            the position in set of chars to start at
	 * @param end
	 *            the position in set of chars to end before
	 * @param letters
	 *            only allow letters?
	 * @param numbers
	 *            only allow numbers?
	 * @param chars
	 *            the set of chars to choose randoms from.
	 *            If <code>null</code>, then it will use the set of all chars.
	 * @param random
	 *            a source of randomness.
	 * @return the random string
	 * @throws ArrayIndexOutOfBoundsException
	 *             if there are not <code>(end - start) + 1</code> characters in
	 *             the set array.
	 * @throws IllegalArgumentException
	 *             if <code>count</code> &lt; 0.
	 * @since 2.0
	 */
	public static String random(int count, int start, int end, boolean letters, boolean numbers, char[] chars,
	        Random random)
	{
		if (count == 0)
		{
			return "";
		}
		else if (count < 0)
		{
			throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
		}
		if ((start == 0) && (end == 0))
		{
			end = 'z' + 1;
			start = ' ';
			if (!letters && !numbers)
			{
				start = 0;
				end = Integer.MAX_VALUE;
			}
		}

		StringBuffer buffer = new StringBuffer();
		int gap = end - start;

		while (count-- != 0)
		{
			char ch;
			if (chars == null)
			{
				ch = (char) (random.nextInt(gap) + start);
			}
			else
			{
				ch = chars[random.nextInt(gap) + start];
			}
			if ((letters && numbers && Character.isLetterOrDigit(ch)) || (letters && Character.isLetter(ch))
			        || (numbers && Character.isDigit(ch)) || (!letters && !numbers))
			{
				buffer.append(ch);
			}
			else
			{
				count++;
			}
		}
		return buffer.toString();
	}
}
