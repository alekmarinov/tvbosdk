package com.aviq.tv.android.sdk.feature.epg;

import java.util.Calendar;
import java.util.List;

import android.graphics.Bitmap;

public interface IEpgDataProvider
{
//	public void loadEpgMap(List<Channel> channelList, long timeStart, long timeEnd);
//	public LinkedHashMap<Channel, List<Program>> getEpgMap();
//	public long getMaxEpgTime();
//	public Channel getChannel(String channelGuid);
//	public Program getCurrentProgramForChannel(String channelGuid);
//	public List<Channel> getChannelList();
//	public boolean isUseFavorites();

	public int getChannelCount();
	public Channel getChannel(int index);
	public Bitmap getChannelLogoBitmap(int index);
	public Program getProgram(String channelId, String dateTime);
	public Program getProgram(String channelId, Calendar when);
	public List<Channel> getChannels();
	public List<Program> getProgramList(String channelId, String timeStart, String timeEnd);
	public Calendar getMaxEpgStopTime();
}
