package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

import static org.neo4j.driver.v1.Values.parameters;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		if (userName.equals("") || userName == null || fullName.equals("") || fullName == null || password.equals("")
				|| password == null) {
			DbQueryStatus status = new DbQueryStatus("empty field", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}

		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					// unique check
					StatementResult result = tx.run(
							"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
							parameters("user", userName));

					if ((result.single().toString()).contains("FALSE")) {
						tx.run("CREATE (p:profile {userName: {user}, fullName: {name}, password: {password}}) - [:created] -> "
								+ "(pl:playlist {plName: {user} + '-favorites playlist'})",
								parameters("user", userName, "name", fullName, "password", password));
						// added succesffuly
						tx.success();
						DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
						return status;
					}
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
			DbQueryStatus status = new DbQueryStatus("error not found - profile not created",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		DbQueryStatus status = new DbQueryStatus("user name already exists in database- profile not created",
				DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		return status;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		if (userName.equals(frndUserName)) {
			DbQueryStatus status = new DbQueryStatus("cannot follow yourself", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run(
							"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
							parameters("user", userName));
					if ((result.single().toString()).contains("TRUE")) {
						StatementResult result2 = tx.run(
								"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
								parameters("user", frndUserName));
						if ((result2.single().toString()).contains("TRUE")) {
							StatementResult result3 = tx.run(
									"MATCH (p:profile)-[r:follows]->(p1:profile) WHERE p.userName = {user} AND p1.userName = {user2} RETURN count(r) > 0 as r",
									parameters("user", userName, "user2", frndUserName));
							if ((result3.single().toString()).contains("FALSE")) {
								tx.run("MATCH (p:profile),(f:profile) WHERE p.userName = {yourUser} AND f.userName = {friendUser} CREATE (p)-[r:follows]->(f)",
										parameters("yourUser", userName, "friendUser", frndUserName));
								// friend followed succesfuly
								tx.success();
								DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
								return status;
							}
						}
					}
				} catch (Exception e) {
					DbQueryStatus status = new DbQueryStatus("transaction has not started - profile not followed",
							DbQueryExecResult.QUERY_ERROR_GENERIC);
					return status;
				}
			} catch (Exception e) {
				DbQueryStatus status = new DbQueryStatus("session has not started - profile not followed",
						DbQueryExecResult.QUERY_ERROR_GENERIC);
				return status;
			}

		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("error - profile not followed",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		DbQueryStatus status = new DbQueryStatus("error - profile not followed",
				DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		return status;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		if (userName.equals("") || frndUserName.equals("") || userName == null || frndUserName == null) {
			DbQueryStatus status = new DbQueryStatus("empty field", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		try {
			try (Session session = driver.session()) {
				try (Transaction tx = session.beginTransaction()) {
					StatementResult result = tx.run(
							"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
							parameters("user", userName));
					if ((result.single().toString()).contains("TRUE")) {
						StatementResult result2 = tx.run(
								"MATCH (p:profile) WHERE p.userName = {user} RETURN count(p) > 0 as p",
								parameters("user", frndUserName));
						if ((result2.single().toString()).contains("TRUE")) {
							StatementResult result3 = tx.run(
									"MATCH (p:profile)-[r:follows]->(p1:profile) WHERE p.userName = {user} AND p1.userName = {user2} RETURN count(r) > 0 as r",
									parameters("user", userName, "user2", frndUserName));
							if ((result3.single().toString()).contains("TRUE")) {
					tx.run("MATCH (p { userName: {user} })-[r:follows]->(f { userName: {friendUser} }) DELETE r",
							parameters("user", userName, "friendUser", frndUserName));
					// friend unfollowed succesfully
					tx.success();
					DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
					return status;
				}}}} catch (Exception e) {
					DbQueryStatus status = new DbQueryStatus("transaction has not started - user not unfollowed",
							DbQueryExecResult.QUERY_ERROR_GENERIC);
					return status;
				}
			} catch (Exception e) {
				DbQueryStatus status = new DbQueryStatus("session has not started  - user not unfollowed",
						DbQueryExecResult.QUERY_ERROR_GENERIC);
				return status;
			}

		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("user does not exist - user not unfollowed",
					DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
		DbQueryStatus status = new DbQueryStatus("user not unfollowed",
				DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		return status;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();

		if (userName.equals(" ") || userName == null) {
			DbQueryStatus status = new DbQueryStatus("empty field", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}

		try (Session session = driver.session()) {
			StatementResult result = session.run("MATCH (noProfile: profile{ userName: {user} }) RETURN noProfile",
					Values.parameters("user", userName));
			if (!result.hasNext()) {
				DbQueryStatus songList = new DbQueryStatus("not valid profile",
						DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return songList;
			}
		}

		try (Session session = driver.session()) {
			StatementResult result = session.run(
					// get the friend
					"MATCH (selfUser: profile{userName:{user}}) -[:follows]-> (frnd: profile) RETURN frnd",
					Values.parameters("user", userName));
			while (result.hasNext()) {
				Record frnd = result.next();
				ArrayList<String> songNames = new ArrayList<String>();
				String frndUser = (String) frnd.fields().get(0).value().asMap().get("userName");
				// get the songs
				StatementResult songIdList = session.run(
						"MATCH (selfUser: profile{userName:{friend}}) -[:created]-> (: playlist) -[:includes]->( songs : song) RETURN songs",
						Values.parameters("friend", frndUser));

				while (songIdList.hasNext()) {
					Record songs = songIdList.next();
					songNames.add((String) songs.fields().get(0).value().asMap().get("songId"));
				}
				data.put((String) frnd.fields().get(0).value().asMap().get("userName"), songNames);
			}
			DbQueryStatus songList = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			songList.setData(data);
			return songList;
		}
	}
}