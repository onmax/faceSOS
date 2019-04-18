package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import database.FriendDB;
import resources.UserResource;

public class FriendController extends Controller {

	public FriendController(UriInfo uriInfo) {
		super(uriInfo);
	}
	
	public Response getFriends(String userId, String name, int limitTo, int page) throws SQLException {
      // Check that userId exists
      UserResource user = new UserResource(userId);
      Response userInformationResponse = this.getUserInformationReponse(user);
      if (userInformationResponse.getStatus() != 200) {
          return userInformationResponse;
      }

      // Get data from DB
      FriendDB db = new FriendDB();
      ResultSet rs = db.getFriends(name, this.getElementsPage(limitTo), userId, page - 1);

      if (rs != null) {
          ArrayList<UserResource> friends = new ArrayList<UserResource>();
          while (rs.next())
              friends.add(new UserResource(rs.getInt("user2_id"), rs, this.getBaseUri()));

          // Array with users
          HashMap<String, Object> data = new HashMap<String, Object>();
          data.put("friends", friends);
          data.put("nFriends", friends.size());
          data.put("pagination", this.getPagination(name, page, friends.size() == limitTo));
          return this.getOkResponse(data, "Data loaded succesfully");
      }

      // Error
      return this.getInternalServerErrorResponse("Unable to get friends information");
  }

	public Response addFriend(String friend1Id, String friend2Id) throws SQLException {
		UserResource friend1 = new UserResource(friend1Id);
		UserResource friend2 = new UserResource(friend2Id);
		Response checkUsersResponse = this.checkUsers(friend1, friend2);
		if(checkUsersResponse != null) {
			return checkUsersResponse;
		}
		
		FriendDB db = new FriendDB();
		if (db.addFriend(friend1.getId(), friend2.getId()) > 0) {
			ArrayList<UserResource> friends = new ArrayList<UserResource>();
			friends.add(friend1);
			friends.add(friend2);
			String location = this.getPath() + "/user/" + friend1.getId() + "/friends";
			return this.getCreatedResponse(friends, location, "Friendship added succesfully");
		}
		
		// Error
		return this.getInternalServerErrorResponse("Unable to create friendship");
	}

	public Response removeFriend(String friend1Id, String friend2Id) throws SQLException {		
		UserResource friend1 = new UserResource(friend1Id);
		UserResource friend2 = new UserResource(friend2Id);
		Response checkUsersResponse = this.checkUsers(friend1, friend2);
		if(checkUsersResponse != null) {
			return checkUsersResponse;
		}

		FriendDB db = new FriendDB();
		if (db.removeFriend(friend1, friend2) > 0) {
		    friend1.setFriendsLocation(this.getBaseUri() + "/friends");
			return this.getOkResponse(friend1, "Friendship removed succesfully");
		}

		// Error message should be an object
		return this.getInternalServerErrorResponse("Unable to remove friend");
	}
	
	private Response checkUsers(UserResource friend1, UserResource friend2) throws SQLException {
		// Check that friend1Id and friend2Id exist
		Response friend1InformationResponse = this.getUserInformationReponse(friend1);
		if (friend1InformationResponse.getStatus() != 200) {
			return friend1InformationResponse;
		}

		Response friend2InformationResponse = this.getUserInformationReponse(friend2);
		if (friend2InformationResponse.getStatus() != 200) {
			return friend2InformationResponse;
		}

		if (friend1.getId() == friend2.getId()) {
			return this.getBadRequestResponse("You can only be friend of other user");
		}
		
		return null;
	}
}