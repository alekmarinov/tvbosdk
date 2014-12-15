package com.aviq.tv.android.sdk.feature.epg;

import java.util.Calendar;
import java.util.List;

import android.graphics.Bitmap;

public interface IEpgDataProvider
{
	public static enum ChannelLogoType
	{
		NORMAL, SELECTED, FAVORITE
	}

	public int getChannelCount();
	public Channel getChannel(int index);
	public Channel getChannel(String channelId);
	public Bitmap getChannelLogoBitmap(int index);
	public Bitmap getChannelLogoBitmap(int index, ChannelLogoType logoType);
	public Program getProgram(String channelId, Calendar when);
	public Program getProgramByOffset(String channelId, Calendar when, int offset);
	public List<Channel> getChannels();
	public List<Program> getProgramList(String channelId, Calendar timeStart, Calendar timeEnd);
	public List<Program> getProgramList(String channelId);
	public Program getProgramById(String channelId, String programId);
	public Calendar getMaxEpgStopTime();
	public Calendar getMinEpgStartTime();
}
