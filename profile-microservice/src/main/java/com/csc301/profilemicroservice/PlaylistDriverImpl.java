package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run(
							"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
							parameters("user", userName));
					if ((result.single().toString()).contains("TRUE")) {
						result = tx.run(
								"MATCH (pl:playlist)-[r:includes]->(s:song) WHERE pl.plName = {user} +'-favorites playlist' AND s.songId = {song} RETURN count(r) > 0 as r",
								parameters("user", userName, "song", songId));
						if ((result.single().toString()).contains("FALSE")) {
							result = tx.run("MATCH (s:song) WHERE s.songId = {song} RETURN count(s) > 0 as s",
									parameters("user", userName, "song", songId));
							String temp = result.single().toString();
							if (temp.contains("TRUE")) {
								tx.run("MATCH (pl:playlist), (s:song) WHERE pl.plName = {user} +'-favorites playlist' AND s.songId = {song} CREATE (pl)-[:includes]->(s)",
										parameters("user", userName, "song", songId));
							} else if (temp.contains("FALSE")) {
								tx.run("MATCH (pl:playlist) WHERE pl.plName = {yourUser} +'-favorites playlist' CREATE (pl)-[r:includes]->(s:song {songId: {song}})",
										parameters("yourUser", userName, "song", songId));
							}
						}
						tx.success();
						DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
						return status;
					}
				} catch (Exception e) {
					DbQueryStatus status = new DbQueryStatus("transaction has not started - song not liked",
							DbQueryExecResult.QUERY_ERROR_GENERIC);
					return status;
				}
			} catch (Exception e) {
				DbQueryStatus status = new DbQueryStatus("session has not started - song not liked",
						DbQueryExecResult.QUERY_ERROR_GENERIC);
				return status;
			}

		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("error not found - song not liked",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		DbQueryStatus status = new DbQueryStatus("error not found - song not liked",
				DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		return status;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run(
							"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
							parameters("user", userName));
					if ((result.single().toString()).contains("TRUE")) {
						result = tx.run("MATCH (pl:playlist)-[r:includes]->(s:song)  WHERE pl.plName = {yourUser} +'-favorites playlist' AND s.songId = {song} RETURN count(r) > 0 as r",
								parameters("yourUser", userName, "song", songId));
						String temp = result.single().toString();
						if ((temp.contains("TRUE"))) {
							tx.run("MATCH (pl:playlist)-[r:includes]->(s:song)  WHERE pl.plName = {yourUser} +'-favorites playlist' AND s.songId = {song} DELETE r",
									parameters("yourUser", userName, "song", songId));
							tx.success();
							DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
							return status;
						}
						else if ((temp.contains("FALSE"))) {
							DbQueryStatus status = new DbQueryStatus("CANT UNLIKE A SONG THAT YOU HAVE NOT LIKED", DbQueryExecResult.QUERY_OK);
							return status;
						}
						tx.run("MATCH (pl:playlist)-[r:includes]->(s:song)  WHERE pl.plName = {yourUser} +'-favorites playlist' AND s.songId = {song} DELETE r",
								parameters("yourUser", userName, "song", songId));
						tx.success();
						DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
						return status;
					}
				} catch (Exception e) {
					DbQueryStatus status = new DbQueryStatus("transaction has not started - song not unliked",
							DbQueryExecResult.QUERY_ERROR_GENERIC);
					return status;
				}
			} catch (Exception e) {
				DbQueryStatus status = new DbQueryStatus("session has not started - song not unliked",
						DbQueryExecResult.QUERY_ERROR_GENERIC);
				return status;
			}

		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("error not found -song not unliked",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		DbQueryStatus status = new DbQueryStatus("user not found - song not unliked",
				DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		return status;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		if (songId.equals(" ") || songId == null) {
			DbQueryStatus status = new DbQueryStatus("empty field", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run("MATCH (s:song)  WHERE s.songId = {song} DETACH DELETE s",
							parameters("song", songId));
					tx.success();
					DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
					return status;
				} catch (Exception e) {
					DbQueryStatus status = new DbQueryStatus("transaction has not started - profile not created",
							DbQueryExecResult.QUERY_ERROR_GENERIC);
					return status;
				}
			} catch (Exception e) {
				DbQueryStatus status = new DbQueryStatus("session has not started - profile not created",
						DbQueryExecResult.QUERY_ERROR_GENERIC);
				return status;
			}

		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("error not found - profile not created - profile not created",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
	}
}
