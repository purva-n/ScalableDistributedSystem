package client;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;

import servlethelpers.LiftRideInput;
import servlethelpers.LiftRideList;

/**
 * The thread class that our multi-threaded client uses to send POST and GET requests to our
 * servlets/database.
 */
public class SkierClientThread extends Thread {

  private final String hostname;
  private final int port;
  private final int firstSkierID;
  private final int lastSkierID;
  private final int liftStartTime;
  private final int liftEndTime;
  private final int numLifts;
  private final HttpClient client;
  private final String app_path = "/A3_war";
  private final Gson gson = new Gson();
  private static int FAILURE_COUNT = 0;
  private static int SUCCESS_COUNT = 0;
  private static int TOTAL_GET = 0;
  private static int TOTAL_POST = 0;
  private static final int[] POST_HISTOGRAM = new int[500];
  private static final int[] GET_HISTOGRAM = new int[500];
  private static int POST_OVERFLOW = 0;
  private static int GET_OVERFLOW = 0;
  private static final Logger log = Logger.getLogger(SkierClientThread.class);

  /**
   * Our constructor which takes the hostname and port of our servlet, a range of skier IDs,
   * a range of possible lift times, and the number of available lifts.
   * @param hostname is the String hostname of the servlet send requests to
   * @param port is the integer port of the servlet to send requests to
   * @param firstSkierID is the integer lower bound for our possible skier IDs
   * @param lastSkierID is the integer upper bound for our possible skier IDs
   * @param liftStartTime is the integer lower bound for our lift ride time
   * @param liftEndTime is the integer upper bound for our lift ride time
   * @param numLifts is the total number of lifts available to choose from
   */
  public SkierClientThread(String hostname, int port, int firstSkierID, int lastSkierID,
                           int liftStartTime, int liftEndTime, int numLifts) {
    this.hostname = hostname;
    this.port = port;
    this.firstSkierID = firstSkierID;
    this.lastSkierID = lastSkierID;
    this.liftStartTime = liftStartTime;
    this.liftEndTime = liftEndTime;
    this.numLifts = numLifts;
    this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
  }

  /**
   * This is the method executed when our threads run which will send POST requests and GET requests
   * to our servlet in a loop until the main thread loop has indicated the timer has completed.
   */
  public void run() {
    // our threads run until main thread switches the done flag
    while (!SkierClientMultiThreaded.isDone()) {
      int resortID = 1;
      int skierID = getRandomNum(firstSkierID, lastSkierID);
      int time = getRandomNum(liftStartTime, liftEndTime);
      int liftId = getRandomNum(1, numLifts);

      // json formatted data
      String requestBody = gson.toJson(
              new LiftRideInput(skierID, resortID, liftId, time));
      int rideID = sendPostRequest(requestBody);
      if (!SkierClientMultiThreaded.isDone()) { // check before running GET
        sendGetRequest(rideID);
      }
      }
  }

  /**
   * Helper method that handles issuing the POST request to the servlet and logging the response.
   * @param jsonPostBody is the JSON formatted LiftRideInput that will be created in the database
   * @return the integer ID of the Lift Ride that was created in our database or -1 if it failed
   */
  private int sendPostRequest(String jsonPostBody) {
    long startTime = System.currentTimeMillis();
    String uriBuilt = "http://" + hostname + app_path + "/liftrides";
    URI uri = URI.create(uriBuilt);
    int rideID = -1;

    try {
      HttpRequest request = HttpRequest.newBuilder()
              .POST(HttpRequest.BodyPublishers.ofString(jsonPostBody))
              .uri(uri)
              .setHeader("Content-Type", "application/json")
              .setHeader("Authorization", basicAuth("admin", "admin"))
              .build();
      HttpResponse<String> response = client.send(request,
              HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 201) {
        incSuccess();
        // pull out the lift ride ID that was returned
        rideID = gson.fromJson(response.body(), LiftRideList.class).getLastRideId();
      } else {
        incFail();
        log.debug("POST: " + response.body());
      }
    } catch (Exception ex) {
      incFail();
      log.debug("POST: " + ex.getMessage());
    }
    updateHistogram("POST", (int) (System.currentTimeMillis() - startTime));
    incPost();
    return rideID;
  }

  /**
   * Helper method that handles issuing the GET request to the servlet and logging the response.
   * @param rideID is the integer ID of the lift ride we are querying
   */
  private void sendGetRequest(int rideID) {
    long startTime = System.currentTimeMillis();
    String uriBuilt = "http://" + hostname + ":" + port + app_path + "/liftrides/" + rideID;
    URI uri = URI.create(uriBuilt);
    if (rideID < 1) {
      incFail();
      log.debug("GET: ride ID less than 1");
      return;
    } else {
      try {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .setHeader("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          incSuccess();
        } else {
          incFail();
          log.debug("GET: " + response.body());
        }
      } catch (Exception ex) {
        incFail();
        log.debug("GET: " + ex.getMessage());
      }
    }
    updateHistogram("GET", (int) (System.currentTimeMillis() - startTime));
    incGet();
  }

  /**
   * Simple formatter for our POST method that handles correctly encoding the HTTP basic
   * authorization for use in the Authorization header
   * @param username is the String username to be encoded
   * @param password is the String password to be encoded
   * @return the Base64 encoded String of the username and password
   */
  private String basicAuth(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
  }

  /**
   * Updates our GET and POST histograms by incrementing the bucket that the given time sits in.
   * Assumes that the histogram holds 10ms buckets.
   * @param operation is the String representing the type of operation (GET or POST)
   * @param time is the integer time it took the request to process
   */
  synchronized private void updateHistogram(String operation, int time) {
    if (operation.equals("POST")) {
      if (time < 5000) {
        POST_HISTOGRAM[time/10]++;
      } else {
        POST_OVERFLOW++;
      }
    } else {
      if (time < 5000) {
        GET_HISTOGRAM[time/10]++;
      } else {
        GET_OVERFLOW++;
      }
    }
  }

  /**
   * Simply selects a random integer value between a given lower and upper bound
   * @param lower is the integer lower bound
   * @param upper is the integer upper bound
   * @return an integer value between the given lower and upper bounds
   */
  private int getRandomNum(int lower, int upper) {
    return new Random().nextInt(upper - lower) + upper;
  }

  /**
   * Increments our global failure counter indicating the number of failed requests
   * (GET & POST combined)
   */
  synchronized private void incFail() {
    FAILURE_COUNT++;
  }

  /**
   * Increments our global success counter indicating the number of successful requests
   * (GET & POST combined)
   */
  synchronized private void incSuccess() {
    SUCCESS_COUNT++;
  }

  /**
   * Increments our global counter holding the total number of GET requests issued
   */
  synchronized private void incGet() {
    TOTAL_GET++;
  }

  /**
   * Increments our global counter holding the total number of POST requests issued
   */
  synchronized private void incPost() {
    TOTAL_POST++;
  }

  /**
   * Returns the total number of GET requests issued
   */
  synchronized public static int getTotalGets() {
    return TOTAL_GET;
  }

  /**
   * Returns the total number of POST requests issued
   */
  synchronized public static int getTotalPosts() {
    return TOTAL_POST;
  }

  /**
   * Returns the total number of failed requests (GET & POST combined)
   */
  synchronized public static int getFailureCount() {
    return FAILURE_COUNT;
  }

  /**
   * Returns the total number of successful requests (GET & POST combined)
   */
  synchronized public static int getSuccessCount() {
    return SUCCESS_COUNT;
  }

  /**
   * @return the integer array holding the response times of our GET requests
   */
  synchronized public static int[] getGetHistogram() {
    return GET_HISTOGRAM;
  }

  /**
   * @return the integer array holding the response times of our POST requests
   */
  synchronized public static int[] getPostHistogram() {
    return POST_HISTOGRAM;
  }

  /**
   * @return the total number of GET request responses that took longer than 5 seconds
   */
  synchronized public static int getGetOverflow() {
    return GET_OVERFLOW;
  }

  /**
   * @return the total number of POST request responses that took longer than 5 seconds
   */
  synchronized public static int getPostOverflow() {
    return POST_OVERFLOW;
  }
}