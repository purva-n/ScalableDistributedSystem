package servlets;

import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import daos.LiftRideDAO;
import servlethelpers.*;

/**
 * Our LiftRides servlet that will respond to URLs matching the /liftrides or /liftrides/* pattern.
 * It pulls properties (System.getProperty) from the catalina.properties file in the Tomcat conf folder.
 */
@WebServlet(name = "LiftRideServlet", urlPatterns = {"/liftrides/*", "/liftrides"},
            initParams = {@WebInitParam(name = "log4j-properties",
                                        value = "WEB-INF/log4j.properties")})
public class LiftRideServlet extends HttpServlet {
  static Logger log;
  private Gson gson;
  private boolean activeLog;

  /**
   * Initialization method called upon servlet startup that initializes the servlet fields.
   */
  @Override
  public void init() throws ServletException {
    PropertyConfigurator.configure("log4j.properties");
    log = Logger.getLogger(LiftRideServlet.class);
    gson = new Gson();
    activeLog = System.getProperty("SERVER_LOG").equals("TRUE");
  }

  /**
   * Internal helper function to determine if the given URL is of superficial valid form for
   * further processing.
   * @param parts is the various parts of the URL normally separated by "/"
   * @return true if the URL is of a valid form, false otherwise
   */
  private boolean isValid(String[] parts) {
    // this is path pattern: /liftrides/{id}
    if (parts.length == 2) {
      try {
        int testID = Integer.parseInt(parts[1]);
      } catch (NumberFormatException e) {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Helper function for our doGet and doPost that formulates (mostly error) messages to be returned
   * in the response body
   * @param response is the HttpServletResponse whose return code and body we are writing
   * @param code     is the integer response code indicating success/failure (e.g. 2XX, 3XX, 4XX, 5XX)
   * @param msg      is the string we want entered as the response body
   * @throws IOException if there is an issue writing to the response body
   */
  private void writeMessage(HttpServletResponse response, int code, String msg) throws IOException {
    response.setStatus(code);
    Message err = new Message(msg);
    String jsonErr = gson.toJson(err);
    response.getWriter().write(jsonErr);
  }

  /**
   * Helper method for doGet that handles querying the database for a list of lift rides for
   * either a specific skier identified by their ID or all skiers
   * @param skierID is the ID of the skier whose lift rides we are querying for or null if
   *                the query is for all skiers
   * @param response is the HTTP response that the servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  private void querySkierRides(String skierID, HttpServletResponse response) throws IOException {
    LiftRideDAO liftDAO = new LiftRideDAO();
    LiftRideList rides;
    // get all lift rides
    if (skierID == null) {
      rides = liftDAO.getAllLiftRides();
    } else { // get lift rides for a specific skier ID
      int parsedID;
      try {
        parsedID = Integer.parseInt(skierID);
      } catch (NumberFormatException e) {
        writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Skier ID invalid format");
        return;
      }
      rides = liftDAO.getSkierRides(parsedID);
    }
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(gson.toJson(rides));
  }

  /**
   * Helper method for doGet that handles querying the database for the requested rideID
   * and setting up the returned response
   * @param rideID is the integer ID of the lift ride to be queried for
   * @param response is the HTTP response that the servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  private void queryRide(int rideID, HttpServletResponse response) throws IOException {
    LiftRideDAO liftDAO = new LiftRideDAO();
    LiftRide ride = liftDAO.getLiftRide(rideID);
    if (ride == null) {
      writeMessage(response, HttpServletResponse.SC_NOT_FOUND, "LiftRideId not found");
    } else {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(gson.toJson(ride));
    }
  }

  /**
   * This handles the GET requests sent to our servlet. Request responses are either a list
   * of lift rides for a particular resort+skier (via HTTP query) or a particular lift ride.
   * @param request  is the GET request received by the servlet
   * @param response is the HTTP response that this servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String path = request.getPathInfo();

    // corresponds to /liftrides endpoint
    if (path == null || path.isEmpty()) {
      String queriedSkier = request.getParameter("skier");
      querySkierRides(queriedSkier, response);
    } else { // /liftrides/{id} endpoint
      // split our path into parts to validate and process
      String[] parts = path.split("/");
      // first ensure basic validity
      if (!isValid(parts)) {
        writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "URL or ID invalid format");
      } else {
        // return a specific lift ride
        queryRide(Integer.parseInt(parts[1]), response);
      }
    }
    // always log our latency at the end if configured
    if (activeLog) {
      logLatency(startTime, "GET");
    }
  }

  /**
   * Handles parsing an authorization HTTP header from our doPost method to ensure that the user is
   * authenticated prior to any updates being performed
   * Method heavily inspired by:
   * https://stackoverflow.com/questions/16000517/how-to-get-password-from-http-basic-authentication
   * @param authHdr is the HTTP request's full authorization header
   * @return true if the header contains valid authentication, false otherwise
   */
  private boolean authenticate(String authHdr) {
    // Only use basic authentication
    if (authHdr != null && authHdr.toLowerCase().startsWith("basic")) {
      String rawCredentials = authHdr.substring("Basic".length()).trim();
      String decoded = new String(Base64.getDecoder().decode(rawCredentials), StandardCharsets.UTF_8);
      String[] credentials = decoded.split(":", 2);
      return credentials[0].equals(System.getProperty("POST_USR"))
              && credentials[1].equals(System.getProperty("POST_PWD"));
    }
    return false;
  }

  /**
   * Helper method for doPost which handles attempting to insert the new LiftRideInput object
   * into our database
   * @param ride is the LiftRideInput containing the information to insert into the DB
   * @param response is the HttpServletResponse that will be returned at the end of doPost
   * @throws IOException if there was an error while writing to the response
   */
  private void databaseWrite(LiftRideInput ride, HttpServletResponse response) throws IOException {
      LiftRideDAO liftDAO = new LiftRideDAO();
      int skier = ride.getSkierID();
      int resort = ride.getResortID();
      int time = ride.getTime();
      int lift = ride.getLiftID();
      // ensure the write succeeded
      if (liftDAO.createLiftRide(ride)) {
        // rideID is auto-generated, so look it up
        int id = liftDAO.getLiftRideId(skier, resort, time, lift);
        // make sure we found ID
        if (id < 0) {
          writeMessage(response, HttpServletResponse.SC_CONFLICT, "Failed writing to server");
        } else {
          // set up return list
          LiftRideList returnList = new LiftRideList();
          returnList.addRide(new LiftRide(id, skier, resort, time, lift));
          // add return list to response body
          response.getWriter().write(gson.toJson(returnList));
          response.setStatus(HttpServletResponse.SC_CREATED);
        }
      } else {
        writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Failed writing to server");
      }
  }

  /**
   * This handles the POST requests sent to our servlet. The only valid POST request is to write a
   * new lift ride for a client.
   * @param request  is the POST request received by the servlet
   * @param response is the HTTP response that this servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String path = request.getPathInfo();
    // must be /liftrides for POST
    if (path != null) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "URL or ID invalid format");
    } else if (!authenticate(request.getHeader("Authorization"))) { // POSTs must authenticate
      writeMessage(response, HttpServletResponse.SC_UNAUTHORIZED, "Could not authenticate user");
    } else {
      // parse our request body for lift ride info
      LiftRideInput body = gson.fromJson(request.getReader(), LiftRideInput.class);
      // only positive integers allowed
      if (body == null || body.getLiftID() < 1 || body.getTime() < 1 || body.getResortID() < 1
              || body.getSkierID() < 1) {
        writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
      } else { // attempt to update DB
        databaseWrite(body, response);
      }
    }
    // always log our latency at the end if configured
    if (activeLog) {
      logLatency(startTime, "POST");
    }
  }

  /**
   * Updates our log with the total time (in milliseconds) it took for a particular method to
   * finish the request
   * @param startTime is the time (in ms) the method began executing
   * @param operation is the type of operation performed by the servlet (i.e. GET or POST)
   */
  private void logLatency(long startTime, String operation) {
    long endTime = System.currentTimeMillis();
    long responseTime = endTime - startTime;
    log.info(operation + " " + responseTime);
  }
}
