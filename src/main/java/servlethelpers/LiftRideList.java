package servlethelpers;

import java.util.ArrayList;

/**
 * Helper object for our servlets.LiftRideServlet which allows for proper JSON display conforming to our
 * Swagger API spec. This object represents a list of life rides taken by a client.
 */
public class LiftRideList {
  ArrayList<LiftRide> rides;

  /**
   * Simple constructor that establishes the underlying array.
   */
  public LiftRideList() {
    this.rides = new ArrayList<>();
  }

  /**
   * Adds a new ride to this list and returns true or returns false if a matching LiftRideId was
   * already found in the underlying list.
   * @param newRide is the new LiftRide to be added to this list
   * @return true if the ride was successfully added, false if the rideID already exists
   * @throws NullPointerException if the given new LiftRide is null
   */
  public boolean addRide(LiftRide newRide) throws NullPointerException {
    if (newRide == null) {
      throw new NullPointerException("Ride cannot be null");
    } else if (rides.isEmpty()) {
      rides.add(newRide);
      return true;
    }

    for (LiftRide ride : rides) {
      if (ride.getLiftRideId() == newRide.getLiftRideId()) {
        return false;
      }
    }
    rides.add(newRide);
    return true;
  }

  /**
   * Simply gets the most recent ride ID added to this list.
   * @return the integer ride ID of the last ride in the underlying list or -1 if empty list
   */
  public int getLastRideId() {
    if (rides.size() == 0) {
      return -1;
    }
    return rides.get(rides.size()-1).getLiftRideId();
  }
}
