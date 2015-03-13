/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVODBulsat.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: VOD feature scheduler for Bulsatcom provider
 */

package com.aviq.tv.android.sdk.feature.vod.bulsat;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD;
import com.aviq.tv.android.sdk.feature.vod.VODGroup;
import com.aviq.tv.android.sdk.feature.vod.VODItem;

/**
 * VOD feature scheduler for Bulsatcom provider
 */
public class FeatureVODBulsat extends FeatureVOD
{
	
	public FeatureVODBulsat() throws FeatureNotFoundException
	{
	}
	
	@Override
	protected VODGroup createVodGroup(String id, String title, VODGroup parent)
	{
		return new VodGroupBulsat(id, title, parent);
	}
	
	@Override
	protected VODItem createVodItem(String id, String title, VODGroup parent)
	{
		return new VodItemBulsat(id, title, parent);
	}
	
	@Override
	protected String getVodItemsUrl()
	{
		String url = super.getVodItemsUrl();
		return url
		        + "?attr=poster_small,poster_medium,poster_large,short_description,release,country,imdb_rating,duration,youtube_trailer_url,cast.director.1.name";
	}
	
	@Override
	protected VODItem.MetaData createVodItemMetaData()
	{
		return new VodItemBulsat.MetaData();
	}
	
	@Override
	protected VODGroup.MetaData createVodGroupMetaData()
	{
		return new VodGroupBulsat.MetaData();
	}
	
	@Override
	protected void indexVodItemMetaData(VODItem.MetaData metaData, String[] meta)
	{
		VodItemBulsat.MetaData bulsatMetaData = (VodItemBulsat.MetaData) metaData;
		super.indexVodItemMetaData(metaData, meta);
		
		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			
			if ("poster_small".equals(key))
				bulsatMetaData.metaPosterSmall = j;
			else if ("poster_medium".equals(key))
				bulsatMetaData.metaPosterMedium = j;
			else if ("poster_large".equals(key))
				bulsatMetaData.metaPosterLarge = j;
			else if ("short_description".equals(key))
				bulsatMetaData.metaShortDescription = j;
			else if ("release".equals(key))
				bulsatMetaData.metaRelease = j;
			else if ("country".equals(key))
				bulsatMetaData.metaCountry = j;
			else if ("imdb_rating".equals(key))
				bulsatMetaData.metaRatingImdb = j;
			else if ("duration".equals(key))
				bulsatMetaData.metaDuration = j;
			else if ("youtube_trailer_url".equals(key))
				bulsatMetaData.metaYouTubeTrailerUrl = j;
			else if ("cast.director.1.name".equals(key))
				bulsatMetaData.metaDirector = j;
			
		}
	}
	
	/**
	 * Returns long representation of vod item id
	 * 
	 * @param vodId
	 * @return vod item id
	 */
	protected long convertVodIdToLong(String vodId)
	{
		vodId = vodId.replace("ivd_", "");
		return Long.parseLong(vodId);
	}
	
	/**
	 * Returns long representation of box id
	 * 
	 * @param boxId
	 * @return box item id
	 */
	protected long convertBoxIdToLong(String boxId)
	{	
		long id = 0;
		if (boxId.length() != 12)
		{
			Log.e(TAG, "Specified MAC Address must contain 12 hex digits ");
		}
		else
		{			
			for (int i = 0; i < 12; i += 2)
			{
				String element = boxId.substring(i,i+2);
				byte octet = (byte) Integer.parseInt(element, 16);
				long t = (octet & 0xffL) << ((5 - i) * 8);
				id |= t;
			}			
		}		
		return id;
	}
}
