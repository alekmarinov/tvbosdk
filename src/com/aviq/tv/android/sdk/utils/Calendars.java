/**
 * Copyright (c) 2003-2014, AVIQ Systems AG
 *
 * Project:     AVIQTVSDK
 * Filename:    Calendars.java
 * Author:      alek
 * Description: Text utilities with Calendar
 */

package com.aviq.tv.android.sdk.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.aviq.tv.android.sdk.core.Log;

public class Calendars
{
	private static final String TAG = Calendars.class.getSimpleName();
	public static final String ISO8601DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSZ";
	public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
	public static final String FORMAT_DATE = "yyyy-MM-dd";
	public static final String FORMAT_TIME_HHMM = "HH:mm";
	public static final String FORMAT_TIME_HHMMSS = "HH:mm:ss";

	/**
	 * @return Current time in unix timestamp in seconds
	 */
	public static int timestamp()
	{
		return timestamp(Calendar.getInstance());
	}

	/**
	 * @return Converts Calendar to unix timestamp in seconds
	 */
	public static int timestamp(Calendar calendar)
	{
		return (int) (calendar.getTimeInMillis() / 1000);
	}

	/**
	 * @param cal
	 *            is a Calendar value
	 * @return Convert Calendar to String according to
	 *         Calendars.FORMAT_DATE_TIME
	 */
	public static String makeString(Calendar cal)
	{
		return makeString(cal, FORMAT_DATE_TIME);
	}

	/**
	 * @param timestamp
	 *            is a unix timestamp value
	 * @return Convert Calendar to String according to
	 *         Calendars.FORMAT_DATE_TIME
	 */
	public static String makeString(int timestamp)
	{
		Calendar cal = Calendar.getInstance();
		if (timestamp > 0)
			cal.setTimeInMillis((long) timestamp * 1000);
		return makeString(cal, FORMAT_DATE_TIME);
	}

	/**
	 * @param cal
	 *            is a Calendar value
	 * @param format
	 *            is date string format
	 * @return Converted Calendar to String according to specified format
	 */
	public static String makeString(Calendar cal, String format)
	{
		if (cal == null)
			return "";

		DateFormat df = new SimpleDateFormat(format, Locale.getDefault());
		df.setTimeZone(cal.getTimeZone());
		return df.format(cal.getTime());
	}

	/**
	 * @param cal
	 *            is a Calendar value
	 * @return Converted Calendar to Time String in format HH:MM
	 */
	public static String makeHHMMString(Calendar cal)
	{
		return makeString(cal, FORMAT_TIME_HHMM);
	}

	/**
	 * @param cal
	 *            is a Calendar value
	 * @return Converted Calendar to Time String in format HH:MM:SS
	 */
	public static String makeHHMMSSString(Calendar cal)
	{
		return makeString(cal, FORMAT_TIME_HHMMSS);
	}

	/**
	 * @param datestr
	 *            is a date string formatted according Calendars.FORMAT
	 * @return Calendar corresponding to date string formatted as
	 *         Calendars.FORMAT
	 */
	public static Calendar makeCalendar(String datestr) throws ParseException
	{
		return makeCalendar(datestr, FORMAT_DATE_TIME);
	}

	/**
	 * @param datestr
	 *            is a date string formatted according to the specified format
	 * @param format
	 *            is date string format
	 * @return Calendar corresponding to date string and format
	 */
	public static Calendar makeCalendar(String datestr, String format) throws ParseException
	{
		DateFormat df = new SimpleDateFormat(format);
		Calendar date = Calendar.getInstance();
		date.setTime(df.parse(datestr));
		return date;
	}

	/**
	 * @return the number of days between two Calendars
	 */
	public static int daysBetweenCalendars(Calendar cal1, Calendar cal2)
	{
		GregorianCalendar day1 = new GregorianCalendar(cal1.get(GregorianCalendar.YEAR),
		        cal1.get(GregorianCalendar.MONTH), cal1.get(GregorianCalendar.DAY_OF_MONTH));
		GregorianCalendar day2 = new GregorianCalendar(cal2.get(GregorianCalendar.YEAR),
		        cal2.get(GregorianCalendar.MONTH), cal2.get(GregorianCalendar.DAY_OF_MONTH));

		long hours = (day1.getTimeInMillis() - day2.getTimeInMillis()) / 1000 / 3600;
		return (int) hours / 24;
	}

	/**
	 * Returns the Calendar set in the beginning of the current day shifted by
	 * dayOffset
	 *
	 * @param dayOffset
	 *            is the day offset (0 for today)
	 */
	public static Calendar getDateByDayOffset(int dayOffset)
	{
		return getDateByDayOffsetFrom(Calendar.getInstance(), dayOffset);
	}

	/**
	 * @param dayFrom
	 *            the starting day
	 * @param dayOffset
	 *            the offset to add
	 * @return a Calendar set in the beginning of the day shifted by dayOffset
	 *         number of days
	 */
	public static Calendar getDateByDayOffsetFrom(Calendar dayFrom, int dayOffset)
	{
		GregorianCalendar dateRes = new GregorianCalendar(dayFrom.get(GregorianCalendar.YEAR),
		        dayFrom.get(GregorianCalendar.MONTH), dayFrom.get(GregorianCalendar.DAY_OF_MONTH));

		dateRes.add(GregorianCalendar.DAY_OF_YEAR, dayOffset);

		return dateRes;
	}

	/**
	 * Returns the Calendar set in the beginning of the current day shifted by
	 * hourOffset
	 *
	 * @param hourOffset
	 *            is the hour offset (0 for now)
	 */
	public static Calendar getDateByHourOffset(Calendar dayFrom, int hourOffset)
	{
		GregorianCalendar dateRes = new GregorianCalendar(dayFrom.get(GregorianCalendar.YEAR),
		        dayFrom.get(GregorianCalendar.MONTH), dayFrom.get(GregorianCalendar.DAY_OF_MONTH),
		        dayFrom.get(GregorianCalendar.HOUR_OF_DAY), dayFrom.get(GregorianCalendar.MINUTE),
		        dayFrom.get(GregorianCalendar.SECOND));
		dateRes.add(GregorianCalendar.HOUR_OF_DAY, hourOffset);

		return dateRes;
	}

	/**
	 * Returns the Calendar set in the beginning of the current day shifted by
	 * minuteOffset
	 *
	 * @param minuteOffset
	 *            is the minute offset (0 for now)
	 */
	public static Calendar getDateByMinuteOffset(Calendar dayFrom, int minuteOffset)
	{
		GregorianCalendar dateRes = new GregorianCalendar(dayFrom.get(GregorianCalendar.YEAR),
		        dayFrom.get(GregorianCalendar.MONTH), dayFrom.get(GregorianCalendar.DAY_OF_MONTH),
		        dayFrom.get(GregorianCalendar.HOUR_OF_DAY), dayFrom.get(GregorianCalendar.MINUTE),
		        dayFrom.get(GregorianCalendar.SECOND));
		dateRes.add(GregorianCalendar.MINUTE, minuteOffset);

		return dateRes;
	}

	/**
	 * @param date
	 *            to calculate day offset from
	 * @return offset from the specified date as 0 for Today, -1 - Yesterday, +1
	 *         - Tomorrow, etc
	 */
	public static int getDayOffsetByDate(Calendar date)
	{
		date = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
		Calendar now = Calendar.getInstance(date.getTimeZone());
		Calendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
		        now.get(Calendar.DAY_OF_MONTH));
		return (int) (date.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);
	}

	/**
	 * @param timestamp
	 *            to calculate day offset from
	 * @return offset from the specified date as 0 for Today, -1 - Yesterday, +1
	 *         - Tomorrow, etc
	 */
	public static int getDayOffsetByDate(int timestamp)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(1000 * (long) timestamp);
		return getDayOffsetByDate(cal);
	}

	/**
	 * Generate a Calendar from ISO 8601 date
	 *
	 * @param date
	 *            a ISO 8601 Date string
	 * @return a Calendar object
	 */
	public static Calendar getCalendarFromISO(String datestring)
	{
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		SimpleDateFormat dateformat = new SimpleDateFormat(ISO8601DATEFORMAT, Locale.getDefault());
		try
		{
			Date date = dateformat.parse(datestring);
			date.setHours(date.getHours() - 1);
			calendar.setTime(date);
		}
		catch (ParseException e)
		{
			Log.e(TAG, e.getMessage(), e);
			return null;
		}

		return calendar;
	}
}
