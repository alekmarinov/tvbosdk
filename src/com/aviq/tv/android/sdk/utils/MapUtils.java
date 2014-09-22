package com.aviq.tv.android.sdk.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapUtils 
{
	/**
	 * Sorting order enum, specifying request result sort behavior.
	 */
	public static enum SortingOrder 
	{
		/**
		 * Resulting sort will be from smaller to biggest.
		 */
		ASCENDING,
		/**
		 * Resulting sort will be from biggest to smallest.
		 */
		DESCENDING
	}
	
	/**
	 * Sort a map by it's keys in ascending order. 
	 *  
	 * @return new instance of {@link LinkedHashMap} contained sorted entries of supplied map.
	 */
	public static <K, V> LinkedHashMap<K, V> sortMapByKey(final Map<K, V> map) 
	{
		return sortMapByKey(map, SortingOrder.ASCENDING);
	}

	/**
	 * Sort a map by it's values in ascending order.
	 *  
	 * @return new instance of {@link LinkedHashMap} contained sorted entries of supplied map.
	 */
	public static <K, V> LinkedHashMap<K, V> sortMapByValue(final Map<K, V> map) 
	{
		return sortMapByValue(map, SortingOrder.ASCENDING);
	}

	/**
	 * Sort a map by it's keys.
	 *  
	 * @param sortingOrder {@link SortingOrder} enum specifying requested sorting order. 
	 * @return new instance of {@link LinkedHashMap} contained sorted entries of supplied map.
	 */
	public static <K, V> LinkedHashMap<K, V> sortMapByKey(final Map<K, V> map, final SortingOrder sortingOrder) 
	{
		Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K,V>>() 
		{
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) 
			{
				return comparableCompare(o1.getKey(), o2.getKey(), sortingOrder);
			}
		};

		return sortMap(map, comparator);
	}
	
	/**
	 * Sort a map by it's values.
	 *  
	 * @param sortingOrder {@link SortingOrder} enum specifying requested sorting order. 
	 * @return new instance of {@link LinkedHashMap} contained sorted entries of supplied map.
	 */
	public static <K, V> LinkedHashMap<K, V> sortMapByValue(final Map<K, V> map, final SortingOrder sortingOrder) 
	{
		Comparator<Map.Entry<K, V>> comparator = new Comparator<Map.Entry<K,V>>() 
		{
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) 
			{
				return comparableCompare(o1.getValue(), o2.getValue(), sortingOrder);
			}
		};

		return sortMap(map, comparator);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> int comparableCompare(T o1, T o2, SortingOrder sortingOrder) 
	{
		int compare = ((Comparable<T>)o1).compareTo(o2);

		switch (sortingOrder) 
		{
			case ASCENDING:
				return compare;
			case DESCENDING:
				return (-1) * compare;
		}

		return 0;
	}
	
	public static <K, V> LinkedHashMap<K, V> sortMap(final Map<K, V> map, final Comparator<Map.Entry<K, V>> comparator) 
	{
		// Convert the map into a list of key/value pairs.
		List<Map.Entry<K, V>> mapEntries = new LinkedList<Map.Entry<K, V>>(map.entrySet());

		// Sort the converted list according to supplied comparator.
		Collections.sort(mapEntries, comparator);

		// Build a new ordered map, containing the same entries as the old map.  
		LinkedHashMap<K, V> result = new LinkedHashMap<K, V>(map.size() + (map.size() / 20));
		for (Map.Entry<K, V> entry : mapEntries) 
		{
			// We iterate on the mapEntries list which is sorted by the 
			// comparator putting new entries into the targeted result 
			// which is a sorted map. 
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
