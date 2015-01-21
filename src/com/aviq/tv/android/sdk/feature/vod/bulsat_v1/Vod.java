package com.aviq.tv.android.sdk.feature.vod.bulsat_v1;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;


public class Vod implements Parcelable
{
	private String _id;
	private String _title;
	private String _titleOrg;
	private String _shortName;
	private String _poster;
	private String _shortDescription;
	private String _description;
	private String _subitems;
	private String _validFrom;
	private String _release;
	private String _duration;
	private String _imdbId;
	private String _rating;
	private String _audioLang;
	private String _subtitles;
	private String _videoType;
	private String _audioType;
	private String _trailerLink;
	private String _country;
	private String _countryId;
	private String _pgId;
	private String _pg;
	private String _genreId;
	private String _genre;
	private String _genresAll;
	private String _genreNameAll;
	private List<Source> _sourcesList = new ArrayList<Source>();
	
	public Vod()
	{}
	
	public String getId()
	{
		return _id;
	}
	
	public void setId(String id)
	{
		_id = id;
	}
	
	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getPoster()
	{
		return _poster;
	}
	
	public void setPoster(String poster)
	{
		_poster = poster;
	}
	
	public void addSource(Source source)
	{
		_sourcesList.add(source);
	}
	
	public List<Source> getSourcesList()
	{
		return _sourcesList;
	}
	
	public void setSourcesList(List<Source> sourcesList)
	{
		_sourcesList = sourcesList;
	}
	
	public String getTitleOrg() 
	{
		return _titleOrg;
	}

	public void setTitleOrg(String titleOrg) 
	{
		_titleOrg = titleOrg;
	}

	public String getShortName() 
	{
		return _shortName;
	}

	public void setShortName(String shortName) 
	{
		_shortName = shortName;
	}

	public String getShortDescription() 
	{
		return _shortDescription;
	}

	public void setShortDescription(String shortDescription) 
	{
		_shortDescription = shortDescription;
	}

	public String getDescription() 
	{
		return _description;
	}

	public void setDescription(String description) 
	{
		_description = description;
	}

	public String getSubitems() 
	{
		return _subitems;
	}

	public void setSubitems(String subitems) 
	{
		_subitems = subitems;
	}

	public String getValidFrom() 
	{
		return _validFrom;
	}

	public void setValidFrom(String validFrom) 
	{
		_validFrom = validFrom;
	}

	public String getRelease() 
	{
		return _release;
	}

	public void setRelease(String release) 
	{
		_release = release;
	}

	public String getDuration() 
	{
		return _duration;
	}

	public void setDuration(String duration) 
	{
		_duration = duration;
	}

	public String getImdbId() 
	{
		return _imdbId;
	}

	public void setImdbId(String imdbId) 
	{
		_imdbId = imdbId;
	}

	public String getRating() 
	{
		return _rating;
	}

	public void setRating(String rating) 
	{
		_rating = rating;
	}

	public String getAudioLang() 
	{
		return _audioLang;
	}

	public void setAudioLang(String audioLang) 
	{
		_audioLang = audioLang;
	}

	public String getSubtitles() 
	{
		return _subtitles;
	}

	public void setSubtitles(String subtitles) 
	{
		_subtitles = subtitles;
	}

	public String getVideoType() 
	{
		return _videoType;
	}

	public void setVideoType(String videoType) 
	{
		_videoType = videoType;
	}

	public String getAudioType() 
	{
		return _audioType;
	}

	public void setAudioType(String audioType) 
	{
		_audioType = audioType;
	}

	public String getTrailerLink() 
	{
		return _trailerLink;
	}

	public void setTrailerLink(String trailerLink) 
	{
		_trailerLink = trailerLink;
	}

	public String getCountry() 
	{
		return _country;
	}

	public void setCountry(String country) 
	{
		_country = country;
	}

	public String getCountryId() 
	{
		return _countryId;
	}

	public void setCountryId(String countryId) 
	{
		_countryId = countryId;
	}

	public String getPgId() 
	{
		return _pgId;
	}

	public void setPgId(String pgId) 
	{
		_pgId = pgId;
	}

	public String getPg() 
	{
		return _pg;
	}

	public void setPg(String pg) 
	{
		_pg = pg;
	}

	public String getGenreId() 
	{
		return _genreId;
	}

	public void setGenreId(String genreId) 
	{
		_genreId = genreId;
	}

	public String getGenre() 
	{
		return _genre;
	}

	public void setGenre(String genre) 
	{
		_genre = genre;
	}

	public String getGenresAll() 
	{
		return _genresAll;
	}

	public void setGenresAll(String genresAll) 
	{
		_genresAll = genresAll;
	}

	public String getGenreNameAll() 
	{
		return _genreNameAll;
	}

	public void setGenreNameAll(String genreNameAll) 
	{
		_genreNameAll = genreNameAll;
	}

	@Override
	public String toString() 
	{
		return "Vod [id=" + _id + ", title=" + _title + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		Vod other = (Vod) obj;
		
		if (_id == null) 
		{
			if (other._id != null)
				return false;
		} 
		else if (!_id.equals(other._id))
			return false;
		
		return true;
	}
	
	// Parcelable contract
	
	public Vod(Parcel in)
	{
		_id = in.readString();
		_title = in.readString();
		_titleOrg = in.readString();
		_shortName = in.readString();
		_poster = in.readString();
		_shortDescription = in.readString();
		_description = in.readString();
		_subitems = in.readString();
		_validFrom = in.readString();
		_release = in.readString();
		_duration = in.readString();
		_imdbId = in.readString();
		_rating = in.readString();
		_audioLang = in.readString();
		_subtitles = in.readString();
		_videoType = in.readString();
		_audioType = in.readString();
		_trailerLink = in.readString();
		_country = in.readString();
		_countryId = in.readString();
		_pgId = in.readString();
		_pg = in.readString();
		_genreId = in.readString();
		_genre = in.readString();
		_genresAll = in.readString();
		_genreNameAll = in.readString();
		_sourcesList = new ArrayList<Source>();
		in.readList(_sourcesList, _sourcesList.getClass().getClassLoader());
	}
	
	@Override
    public int describeContents()
	{
        return 0;
    }
	
	@Override
    public void writeToParcel(Parcel dest, int flags) 
    {
		dest.writeString(_id);
		dest.writeString(_title);
		dest.writeString(_titleOrg);
		dest.writeString(_shortName);
		dest.writeString(_poster);
		dest.writeString(_shortDescription);
		dest.writeString(_description);
		dest.writeString(_subitems);
		dest.writeString(_validFrom);
		dest.writeString(_release);
		dest.writeString(_duration);
		dest.writeString(_imdbId);
		dest.writeString(_rating);
		dest.writeString(_audioLang);
		dest.writeString(_subtitles);
		dest.writeString(_videoType);
		dest.writeString(_audioType);
		dest.writeString(_trailerLink);
		dest.writeString(_country);
		dest.writeString(_countryId);
		dest.writeString(_pgId);
		dest.writeString(_pg);
		dest.writeString(_genreId);
		dest.writeString(_genre);
		dest.writeString(_genresAll);
		dest.writeString(_genreNameAll);
		dest.writeList(_sourcesList);
    }
	
    public static final Parcelable.Creator<Vod> CREATOR = new Parcelable.Creator<Vod>() 
    {
        public Vod createFromParcel(Parcel in) 
        {
            return new Vod(in); 
        }

        public Vod[] newArray(int size) 
        {
            return new Vod[size];
        }
    };
}
