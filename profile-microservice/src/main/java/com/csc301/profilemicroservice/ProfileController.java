package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		try {
			DbQueryStatus status = profileDriver.createUserProfile(params.get("userName"), params.get("fullName"),
					params.get("password"));
			response.put("status", status.getMessage());
		} catch (Exception e) {
			response.put("status", "ERROR NOT FOUND");
			return response;
		}
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		try {
			DbQueryStatus status = profileDriver.followFriend(userName, friendUserName);
			response.put("status", status.getMessage());
		} catch (Exception e) {
			response.put("status", "ERROR NOT FOUND");
			return response;
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus songFriendsLikeList = profileDriver.getAllSongFriendsLike(userName);

		if (songFriendsLikeList.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_ERROR_NOT_FOUND)) {
			response.put("status", "USER SONGLIST NOT FOUND");
			return response;
		}

		Map<String, ArrayList<String>> data = (Map<String, ArrayList<String>>) songFriendsLikeList.getData();

		for (String name : data.keySet()) {
			ArrayList<String> songName = new ArrayList<String>();
			for (String songId : data.get(name)) {

				Request req = new Request.Builder().url("http://localhost:3001/getSongTitleById/" + songId).build();
				try (Response resp = this.client.newCall(req).execute()) {
					String body = resp.body().string();
					JSONObject responseData = new JSONObject(body);
					songName.add((String) responseData.get("data"));
				} catch (Exception e) {
					response.put("status", "ERROR NOT FOUND");
					return response;
				}
			}
			data.replace(name, songName);
		}
		Utils.setResponseStatus(response, songFriendsLikeList.getdbQueryExecResult(), data);

		return response;
	}

	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		try {
			DbQueryStatus status = profileDriver.unfollowFriend(userName, friendUserName);
			response.put("status", status.getMessage());
		} catch (Exception e) {
			response.put("status", "ERROR NOT FOUND");
			return response;
		}
		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		// String path = String.format("GET http://localhost:3001/getSongById/"+songId);
		try {
			if (!songId.isEmpty() && !userName.isEmpty()&& !songId.equals(" ") && !userName.equals(" ")) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001/getSongById/" + songId).newBuilder();
				HttpUrl.Builder likeBuilder = HttpUrl
						.parse("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=false")
						.newBuilder();
				String likeUrl = likeBuilder.build().toString();
				String url = urlBuilder.build().toString();

				RequestBody body = RequestBody.create(null, new byte[0]);

				Request requestOne = new Request.Builder().url(url).build();

				Request requestLike = new Request.Builder().url(likeUrl).method("PUT", body).build();

				Call call = client.newCall(requestOne);
				Call likeCall = client.newCall(requestLike);
				Response responseFromAddMs = null;
				Response responseFromLike = null;

				String addServiceBody;
				String likeServiceBody;

				try {
					responseFromAddMs = call.execute();
					addServiceBody = responseFromAddMs.body().string();
					if (addServiceBody.contains("song not in database")) {
						response.put("status", "SONG NOT FOUND");
						return response;
					} else {
						try {
							DbQueryStatus status = playlistDriver.likeSong(userName, songId);
							responseFromLike = likeCall.execute();
							likeServiceBody = responseFromLike.body().string();
							response.put("status", status.getMessage());
							return response;
						} catch (Exception e) {
							response.put("status", "ERROR NOT FOUND");
							return response;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (NumberFormatException nfe) {

		}

		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		response.put("status", "empty string");
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		// String path = String.format("GET http://localhost:3001/getSongById/"+songId);
		try {
			if (!songId.isEmpty() && !userName.isEmpty() && !songId.equals(" ") && !userName.equals(" ")) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001/getSongById/" + songId).newBuilder();
				HttpUrl.Builder likeBuilder = HttpUrl
						.parse("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=true")
						.newBuilder();
				String likeUrl = likeBuilder.build().toString();
				String url = urlBuilder.build().toString();

				RequestBody body = RequestBody.create(null, new byte[0]);

				Request requestOne = new Request.Builder().url(url).build();

				Request requestLike = new Request.Builder().url(likeUrl).method("PUT", body).build();

				Call call = client.newCall(requestOne);
				Call likeCall = client.newCall(requestLike);
				Response responseFromAddMs = null;
				Response responseFromLike = null;

				String addServiceBody;
				String likeServiceBody;

				try {
					responseFromAddMs = call.execute();
					addServiceBody = responseFromAddMs.body().string();
					if (addServiceBody.contains("song not in database")) {
						response.put("status", "SONG NOT FOUND");
						return response;
					} else {
						try {
							DbQueryStatus status = playlistDriver.unlikeSong(userName, songId);
							responseFromLike = likeCall.execute();
							likeServiceBody = responseFromLike.body().string();
							response.put("status", status.getMessage());
							return response;
						} catch (Exception e) {
							response.put("status", "ERROR NOT FOUND");
							return response;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (NumberFormatException nfe) {

		}

		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		response.put("status", "ERROR NOT FOUND");
		return response;
	}

	  @RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	  public @ResponseBody Map<String, Object> deleteAllSongsFromDb(
	      @PathVariable("songId") String songId, HttpServletRequest request) {

	    Map<String, Object> response = new HashMap<String, Object>();
	    try {
	      DbQueryStatus status = playlistDriver.deleteSongFromDb(songId);
	      response.put("status", status.getMessage());
	    } catch (Exception e) {
	      response.put("status", "ERROR NOT FOUND");
	      return response;
	    }
	    return response;
	  }
	}
