package com.aviq.tv.android.sdk.feature.epg;

import java.util.Calendar;
import java.util.List;

import android.graphics.Bitmap;

public interface IEpgDataProvider
{
	public int getChannelCount();
	public Channel getChannel(int index);
	public Channel getChannel(String channelId);
	public Bitmap getChannelLogoBitmap(int index);
	public Program getProgram(String channelId, Calendar when);
	public List<Channel> getChannels();
	public List<Program> getProgramList(String channelId, Calendar timeStart, Calendar timeEnd);
	public Program getProgramById(String channelId, String programId);
	public Calendar getMaxEpgStopTime();
	public Calendar getMinEpgStartTime();
}
