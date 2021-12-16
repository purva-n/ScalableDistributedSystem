package daos;

import java.sql.*;
import org.apache.commons.dbcp2.*;
import servlethelpers.LiftRide;
import servlethelpers.LiftRideInput;
import servlethelpers.LiftRideList;

/**
 * Our database access object that handles interacting with the liftrides table of the Upic DB.
 * The liftrides table holds information captured when a skier uses their RFID swipe to
 * take a ski lift
 */
public class LiftRideDAO {
  private static BasicDataSource dataSource;

  public LiftRideDAO() {
    dataSource = DBCPDataSource.getDataSource();
  }

  /**
   * Ensures our connection and our prepared statement have been properly closed
   * @param conn is our connection to the database/datasource
   * @param prepStatement is the SQL query we have used to query our database
   */
  private void closeAll(Connection conn,  PreparedStatement prepStatement) {
    try {
      if (conn != null) {
        conn.close();
      }
      if (prepStatement != null) {
        prepStatement.close();
      }
    } catch (SQLException se) {
      se.printStackTrace();
    }
  }

  /**
   * Takes a skierID and returns a LiftRideList of all lift rides for that skier ID
   * @param skierID an integer ID for the skier
   * @return a LiftRideList containing all lift rides for that skier ID
   */
  public LiftRideList getSkierRides(int skierID) {
    Connection conn = null;
    PreparedStatement prepStatement = null;
    String query = "SELECT * FROM liftrides WHERE skierID = ?";
    // this will be updated to new list, otherwise indicates no data found
    LiftRideList rideList = new LiftRideList();
    try {
      conn = dataSource.getConnection();
      prepStatement = conn.prepareStatement(query);
      prepStatement.setInt(1, skierID);
      ResultSet queryResult = prepStatement.executeQuery();
      if (queryResult.next()) {
        do {
          rideList.addRide(new LiftRide(queryResult.getInt("rideID"),
                  queryResult.getInt("skierID"),
                  queryResult.getInt("resortID"),
                  queryResult.getInt("liftID"),
                  queryResult.getInt("tID")));
        } while (queryResult.next());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      closeAll(conn, prepStatement);
    }
    return rideList;
  }

  /**
   * Gets all lift rides from our database and returns them as a LiftRideList
   * @return a LiftRideList of all lift rides for all skiers
   */
  public LiftRideList getAllLiftRides() {
    Connection conn = null;
    PreparedStatement prepStatement = null;
    String query = "SELECT * FROM liftrides";
    // this will be updated to new list, otherwise indicates no data found
    LiftRideList rideList = new LiftRideList();
    try {
      conn = dataSource.getConnection();
      prepStatement = conn.prepareStatement(query);
      ResultSet queryResult = prepStatement.executeQuery();
      if (queryResult.next()) {
        do {
          rideList.addRide(new LiftRide(queryResult.getInt("rideID"),
                  queryResult.getInt("skierID"),
                  queryResult.getInt("resortID"),
                  queryResult.getInt("liftID"),
                  queryResult.getInt("tID")));
        } while (queryResult.next());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      closeAll(conn, prepStatement);
    }
    return rideList;
  }

  /**
   * Takes a specific integer ride ID and returns a Lift Ride object representing that ride
   * @param rideID is the integer ID of the lift ride to be returned
   * @return a Lift Ride object representing the given rideID or null if not found
   */
  public LiftRide getLiftRide(int rideID) {
    Connection conn = null;
    PreparedStatement prepStatement = null;
    String query = "SELECT skierID, resortID, tID, liftID FROM liftrides " +
            "WHERE rideID = ?";
    // this will be updated to a new LiftRide, otherwise indicates no data found
    LiftRide ride = null;
    try {
      conn = dataSource.getConnection();
      prepStatement = conn.prepareStatement(query);
      prepStatement.setInt(1, rideID);
      ResultSet queryResult = prepStatement.executeQuery();
      if (queryResult.next()) {
        ride = new LiftRide(rideID, queryResult.getInt("skierID"),
                queryResult.getInt("resortID"),
                queryResult.getInt("liftID"),
                queryResult.getInt("tID"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      closeAll(conn, prepStatement);
    }
    return ride;
  }

  /**
   * Takes a set of parameters and returns the matching LiftRideId
   * @param skier is the integer ID of the client
   * @param resort is the integer ID of the resort
   * @param time is the integer ID of the time
   * @param liftID is in the integer ID of the ski lift at that resort
   * @return the integer LiftRideId for the matching client or -1 if no data was found
   */
  public int getLiftRideId(int skier, int resort, int time, int liftID) {
    Connection conn = null;
    PreparedStatement prepStatement = null;
    String query = "SELECT rideID FROM liftrides " +
            "WHERE skierID = ? AND resortID = ? AND tID = ? AND liftID = ? " +
            "ORDER BY rideID DESC";
    // this will be updated to correct ID, otherwise indicates no data found
    int id = -1;
    try {
      conn = dataSource.getConnection();
      prepStatement = conn.prepareStatement(query);
      prepStatement.setInt(1, skier);
      prepStatement.setInt(2, resort);
      prepStatement.setInt(3, time);
      prepStatement.setInt(4, liftID);
      ResultSet queryResult = prepStatement.executeQuery();
      if (queryResult.next()) {
        id = queryResult.getInt("rideID");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      closeAll(conn, prepStatement);
    }
    return id;
  }

  /**
   * Takes a filled LiftRideInput object and adds the values as a new row to our liftrides table
   * @param newLiftRide is the Lift Ride object holding the data to be added
   * @return true if the row was successfully inserted, false otherwise
   */
  public boolean createLiftRide(LiftRideInput newLiftRide) {
    Connection conn = null;
    PreparedStatement prepStatement = null;
    String insert = "INSERT INTO liftrides (skierID, resortID, tID, liftID) " +
            "VALUES (?,?,?,?)";
    // flag to indicate successful insertion
    boolean success = false;
    try {
      conn = dataSource.getConnection();
      prepStatement = conn.prepareStatement(insert);
      prepStatement.setInt(1, newLiftRide.getSkierID());
      prepStatement.setInt(2, newLiftRide.getResortID());
      prepStatement.setInt(3, newLiftRide.getTime());
      prepStatement.setInt(4, newLiftRide.getLiftID());

      // execute insert SQL statement and set our flag
      prepStatement.executeUpdate();
      success = true;
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      closeAll(conn, prepStatement);
    }
    return success;
  }
}
