package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		Song song = songToAdd;
		db.save(songToAdd);
		DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		status.setData(songToAdd);
		return status;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		Song song_found = db.findById(songId, Song.class);
		//null check
		if (song_found ==null) {
			DbQueryStatus status = new DbQueryStatus("song not in database",DbQueryExecResult.QUERY_ERROR_GENERIC);
			status.setData(song_found);
			return status;
		}
		String res  = song_found.getSongName();
		DbQueryStatus status = new DbQueryStatus(res,DbQueryExecResult.QUERY_OK);
		status.setData(song_found);
		return status;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		Song song_found = db.findById(songId, Song.class);
		//null check
		if (song_found ==null) {
			DbQueryStatus status = new DbQueryStatus("song not in database",DbQueryExecResult.QUERY_ERROR_GENERIC);
			status.setData(song_found);
			return status;
		}
		String res  = song_found.getSongName();
		DbQueryStatus status = new DbQueryStatus(res,DbQueryExecResult.QUERY_OK);
		status.setData(song_found);
		return status;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO fave list stuff
	    Song song_found = db.findById(songId, Song.class);
	    
	    if (song_found == null) {
	      DbQueryStatus status = new DbQueryStatus("song not in database", DbQueryExecResult.QUERY_ERROR_GENERIC);
	      status.setData(song_found);
	      return status;
	    }
	    try {
	      db.remove(song_found);
	      DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	      status.setData(song_found);
	      return status;
	    } catch(Exception e) {
	      System.out.println("Something went wrong");
	      return null; // not sure what to put here for time being
	    }
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
	    Song song_found = db.findById(songId, Song.class);
	    
	    if (song_found == null) {
          DbQueryStatus status = new DbQueryStatus("song not in database", DbQueryExecResult.QUERY_ERROR_GENERIC);
          status.setData(song_found);
          return status;
        }
	    
	    try {
	      long faveCount = song_found.getSongAmountFavourites();
	      if (shouldDecrement) {
	    	  if (song_found.getSongAmountFavourites() <= 0) {
	              DbQueryStatus status = new DbQueryStatus("favourites cannot be less than 0", DbQueryExecResult.QUERY_ERROR_GENERIC);
	              status.setData(song_found);
	              return status;
	    	  }
	      song_found.setSongAmountFavourites(faveCount - 1);
	      } else if (!shouldDecrement) {
	        song_found.setSongAmountFavourites(faveCount + 1);
	      }
	      db.save(song_found);
	      DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	      status.setData(song_found);
	      return status;
	      
	    } catch(Exception e) {
	      System.out.println("Something went wrong");
          return null; // not sure what to put here for time being
	    }
	   
	}
}