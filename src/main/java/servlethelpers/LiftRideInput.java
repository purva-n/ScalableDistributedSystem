package servlethelpers;

/**
 * Helper class for our servlets.LiftRideServlet which represents a request body sent to our servlet.
 * This allows us to read a JSON string body into this object and easily pull out
 * the attributes held in an HTTP's request body.
 */
public class LiftRideInput {
  private final int skier;
  private final int resort;
  private final int lift;
  private final int time;

  /**
   * Constructs this object to hold the given skierID, resortID, liftID, and time
   * @param skier is the integer ID of the client taking this ride
   * @param resort is the resort ID where this lift ride occurred
   * @param lift is the integer ID of the lift taken by a particular client
   * @param time is an integer representing the time the lift was taken
   */
  public LiftRideInput(int skier, int resort, int lift, int time) {
    this.skier = skier;
    this.resort = resort;
    this.lift = lift;
    this.time = time;
  }

  /**
   * Simple getter that returns the client ID held by this object
   * @return an integer client ID
   */
  public int getSkierID() {
    return this.skier;
  }

  /**
   * Simple getter that returns the resort ID held by this object
   * @return an integer resort ID
   */
  public int getResortID() {
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
