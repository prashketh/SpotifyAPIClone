package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		DbQueryStatus status = songDal.findSongById(songId);
		// null check
		if (status.getData() == null) {
			response.put("status", status.getMessage());
			return response;
		} else {
			response.put("status", "OK");
			response.put("data", status.getData());
			return response;
		}
	}

	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		DbQueryStatus status = songDal.getSongTitleById(songId);
		Song song = (Song) status.getData();
		// null check
		if (status.getData() == null) {
			response.put("status", status.getMessage());
			return response;
		} else {
			response.put("data", song.getSongName());
			response.put("status", "OK");
			return response;
		}
	}

	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		DbQueryStatus status = songDal.deleteSongById(songId);
		Song song = (Song) status.getData();
		try {
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3002/deleteAllSongsFromDb/" + songId)
					.newBuilder();
			String url = urlBuilder.build().toString();
			RequestBody body = RequestBody.create(null, new byte[0]);
			Request requestOne = new Request.Builder().url(url).method("PUT", body).build();
			Call call = client.newCall(requestOne);
			Response responseFromAddMs = null;
			try {
				responseFromAddMs = call.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (NumberFormatException nfe) {

		}
		if (status.getData() == null) {
			response.put("status", status.getMessage());
			return response;
		} else {
			response.put("status", status.getMessage());
			return response;
		}

	}

	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		Song addedSong = new Song(params.get("songName"), params.get("songArtistFullName"), params.get("songAlbum"));
		// null check
		if (params.get("songName") == null || params.get("songArtistFullName") == null
				|| params.get("songAlbum") == null || params.get("songName") == "" || params.get("songAlbum") == ""
				|| params.get("songArtistFullName") == "") {
			response.put("status", "required fields are missing");
			return response;
		}
		DbQueryStatus status = songDal.addSong(addedSong);
		Song song = (Song) status.getData();
		response.put("data", song.getJsonRepresentation());
		response.put("status", status.getMessage());
		return response;
	}

	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		boolean moreOrLess = false;
		if (shouldDecrement.equals("true"))
			moreOrLess = true;
		else if (shouldDecrement.equals("false"))
			moreOrLess = false;
		else {
			response.put("status", "invalid shouldDecrement value");
			return response;
		}
		DbQueryStatus status = songDal.updateSongFavouritesCount(songId, moreOrLess);
		Song song = (Song) status.getData();
		if (status.getData() == null) {
			response.put("status", status.getMessage());
			return response;
		} else {
			response.put("status", status.getMessage());
			return response;
		}
		// response.put("data", String.format("PUT %s", Utils.getUrl(request)));
	}
}
