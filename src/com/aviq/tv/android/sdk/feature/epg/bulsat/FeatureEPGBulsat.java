/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGBulsat.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Bulsat specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.feature.channels.FeatureChannels.UserParam;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGBulsat extends FeatureEPG
{
	public static final String TAG = FeatureEPGBulsat.class.getSimpleName();

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
//		userPrefs.put(FeatureChannels.UserParam.CHANNELS, "animal-planet,axn,bnt-1,btv,btv-action,btv-cinema,btv-comedy,diema,diema-family,discovery-channel,discovery-hd,film-plus,hbo,kinonova,mtv-hits,ngc,nick-jr,nova-sport,nova,planeta-tv,ring-bg,the-voice,tv7,tv-plus,viasat-explorer,hobby-hd");
		userPrefs.put(UserParam.LAST_CHANNEL_ID, "24-kitchen");
		super.initialize(onFeatureInitialized);
	}

	/**
	 * @return Bulsat EPG provider name
	 */
	@Override
    protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.bulsat;
	}

	@Override
	protected String getProgramsUrl(String channelId)
	{
		String url = super.getProgramsUrl(channelId);
		return url + "?attr=description";
	}

	@Override
    protected Channel createChannel(int index)
    {
	    return new ChannelBulsat(index);
    }

	@Override
	protected Program createProgram(Channel channel)
    {
	    return new ProgramBulsat(channel);
    }

	@Override
    public String getChannelStreamId(int channelIndex)
    {
		// FIXME: Refactore to return stream id here, but provide the real url from new Bulsat streamer
		if (true)
			return "http://185.4.83.195:1935/tv/test/playlist.m3u8";
		switch (channelIndex)
		{
			case 0:
				return "http://185.4.83.195:1935/dvr4/amoreyohd.stream/playlist.m3u8";
			case 1:
				return "http://185.4.83.195:1935/dvr4/amoreyosd.stream/playlist.m3u8";
			case 2:
				return "http://185.4.83.195:1935/dvr4/animal_planet.stream/playlist.m3u8";
			case 3:
				return "http://185.4.83.195:1935/dvr4/axn.stream/playlist.m3u8";
			case 4:
				return "http://185.4.83.195:1935/dvr4/cubayohd.stream/playlist.m3u8";
			case 5:
				return "http://185.4.83.195:1935/dvr4/cubayosd.stream/playlist.m3u8";
			case 6:
				return "http://185.4.83.195:1935/dvr4/bnt1.stream/playlist.m3u8";
			case 7:
				return "http://185.4.83.195:1935/dvr4/partyshd.stream/playlist.m3u8";
			case 8:
				return "http://185.4.83.195:1935/dvr4/partyssd.stream/playlist.m3u8";
			case 9:
				return "http://185.4.83.195:1935/dvr4/btv.stream/playlist.m3u8";
			case 10:
				return "http://185.4.83.195:1935/dvr4/btv_action.stream/playlist.m3u8";
			case 11:
				return "http://185.4.83.195:1935/dvr4/btv_cinema.stream/playlist.m3u8";
			case 12:
				return "http://185.4.83.195:1935/dvr4/btv_comedy.stream/playlist.m3u8";
			case 22:
				return "http://185.4.83.195:1935/dvr4/diema.stream/playlist.m3u8";
			case 23:
				return "http://185.4.83.195:1935/dvr4/diema_family.stream/playlist.m3u8";
			case 24:
				return "http://185.4.83.195:1935/dvr4/discovery.stream/playlist.m3u8";
			case 25:
				return "http://185.4.83.195:1935/dvr4/discovery_world.stream/playlist.m3u8";
			case 34:
				return "http://185.4.83.195:1935/dvr4/filmplus.stream/playlist.m3u8";
			case 42:
				return "http://185.4.83.195:1935/dvr4/hbo.stream/playlist.m3u8";
			case 47:
				return "http://185.4.83.195:1935/dvr4/hobbytv.stream/playlist.m3u8";
			case 52:
				return "http://185.4.83.195:1935/dvr4/kinonova.stream/playlist.m3u8";
			case 55:
				return "http://185.4.83.195:1935/dvr4/mtv_rock.stream/playlist.m3u8";
			case 56:
				return "http://185.4.83.195:1935/dvr4/nat_geo.stream/playlist.m3u8";
			case 60:
				return "http://185.4.83.195:1935/dvr4/nickelodeon.stream/playlist.m3u8";
			case 62:
				return "http://185.4.83.195:1935/dvr4/novasport.stream/playlist.m3u8";
			case 61:
				return "http://185.4.83.195:1935/dvr4/novatv.stream/playlist.m3u8";
			case 67:
				return "http://185.4.83.195:1935/dvr4/planetatv.stream/playlist.m3u8";
			case 70:
				return "http://185.4.83.195:1935/dvr4/ringbg.stream/playlist.m3u8";
			case 74:
				return "http://185.4.83.195:1935/dvr4/the_voice.stream/playlist.m3u8";
			case 77:
				return "http://185.4.83.195:1935/dvr4/tv7.stream/playlist.m3u8";
			case 79:
				return "http://185.4.83.195:1935/dvr4/tvplus.stream/playlist.m3u8";
			case 80:
				return "http://185.4.83.195:1935/dvr4/viasat_history.stream/playlist.m3u8";
		}
		return "http://185.4.83.195:1935/dvr/sportaltv.stream/playlist.m3u8";
    }

	@Override
    protected Program.MetaData createProgramMetaData()
	{
		return new ProgramBulsat.MetaData();
	}

	@Override
    protected void indexProgramMetaData(Program.MetaData metaData, String[] meta)
	{
		ProgramBulsat.MetaData bulsatMetaData = (ProgramBulsat.MetaData)metaData;
		super.indexProgramMetaData(metaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("description".equals(key))
				bulsatMetaData.metaDescription = j;
		}
	}
}
