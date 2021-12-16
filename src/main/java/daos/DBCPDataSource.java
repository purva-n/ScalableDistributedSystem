package daos;

import org.apache.commons.dbcp2.*;

/**
 * Our database connection pool which handles our DAO connections to the Upic RDS database.
 * This code is heavily based on our Lab 6 from class: https://piazza.com/class/ktdhhr8hkmg2x6?cid=90
 */
public class DBCPDataSource {
  private static BasicDataSource dataSource;
  private static final String HOST_NAME = System.getProperty("MySQL_IP_ADDRESS");
  private static final String PORT = System.getProperty("MySQL_PORT");
  private static final String DATABASE = "upic";
  private static final String USERNAME = System.getProperty("DB_USERNAME");
  private static final String PASSWORD = System.getProperty("DB_PASSWORD");

  static {
    // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
    dataSource = new BasicDataSource();
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.out.println("Failure finding JDBC driver");
    }
    String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
    // set up basic connection pool login
    dataSource.setUrl(url);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
    // set up initial and max number of allowed DB connections
    dataSource.setInitialSize(10);
    dataSource.setMaxTotal(60);
    // set up connection pool to find and recover unclosed DB connections
    dataSource.setTimeBetweenEvictionRunsMillis(300);
    dataSource.setRemoveAbandonedOnBorrow(true);
    dataSource.setRemoveAbandonedTimeout(15);
    dataSource.setRemoveAbandonedOnMaintenance(true);
  }

  /**
   * Simple getter to fetch our underlying data source for our DAOs to use
   * @return the data source connection to our database
   */
  public static BasicDataSource getDataSource() {
    return dataSource;
  }
}
