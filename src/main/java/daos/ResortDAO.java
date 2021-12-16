package daos;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import servlethelpers.Resort;
import servlethelpers.ResortList;

/**
 * Our database access object that handles interacting with the resorts table of the Upic DB.
 * The resorts table holds information on our current active resorts.
 */
public class ResortDAO {
  private static BasicDataSource dataSource;

  public ResortDAO() {
    dataSource = DBCPDataSource.getDataSource();
  }

  /**
   * Queries our database and returns a ResortList object filled with the
   * current active resorts we have
   */
  public ResortList getResortList() {
    Connection conn = null;
    Statement statement = null;
    ResortList resorts = new ResortList();
    String query = "SELECT resortID, name FROM resorts";
    try {
      conn = dataSource.getConnection();
      statement = conn.createStatement();
      ResultSet queryResult = statement.executeQuery(query);
      // construct a new Resort object from the data
      // add the filled Resort to our ResortList
      while (queryResult.next()) {
        resorts.addResort(new Resort(queryResult.getString("name"),
                queryResult.getInt("resortID")));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (statement != null) {
          statement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return resorts;
  }
}
