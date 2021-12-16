package servlethelpers;

/**
 * Helper class servlets.LiftRideServlet which represents a response body returned from our servlet.
 */
public class LiftRide {
  private final int liftRideId;
  private String url;
  private final int skier;
  private final int resort;
  private final int lift;
  private final int time;

  /**
   * Constructs this object to hold the given rideID, skierID, resortID, liftID, and time
   * @param liftRideId is the integer ID that identifies this lift ride
   * @param skier is the integer ID of the client taking this ride
   * @param resort is the resort ID where this lift ride occurred
   * @param lift is the integer ID of the lift taken by a particular client
   * @param time is an integer representing the time the lift was taken
   */
  public LiftRide(int liftRideId, int skier, int resort, int lift, int time) {
    this.liftRideId = liftRideId;
    this.url = "/liftrides/" + liftRideId;
    this.skier = skier;
    this.resort = resort;
    this.lift = lift;
    this.time = time;
  }

  /**
   * Simple getter that returns the lift ride ID held by this object
   * @return an integer lift ride ID
   */
  public int getLiftRideId() {
    return this.liftRideId;
  }

  /**
   * Simple getter that returns the URL held by this object
   * @return a String url
   */
  public String getURL() {
    return this.url;
  }

  /**
   * Simple getter that returns the client ID held by this object
   * @return an integer client ID
   */
  public int getSkier() {
    return this.skier;
  }

  /**
   * Simple getter that returns the resort ID held by this object
   * @return an integer resort ID
   */
  public int getResort() {
    return this.resort;
  }

  /**
   * Simple getter that returns the liftID held by this object
   * @return an integer lift ID
   */
  public int getLiftID() {
    return lift;
  }

  /**
   * Simple getter that returns the time held by this object
   * @return an integer representing time
   */
  public int getTime() {
    return time;
  }
}
