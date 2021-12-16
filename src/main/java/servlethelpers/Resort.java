package servlethelpers;

/**
 * Mostly a wrapper class for proper JSON display value conforming to our Swagger API spec.
 * This object represents a Resort which has a particular name and unique ID number
 */
public class Resort {
  private String resortName;
  private int resortID;

  /**
   * Construct a resort with a given name and ID number
   * @param name is the name associated with the Resort
   * @param id is the unique ID number representing this Resort
   */
  public Resort(String name, int id) {
    this.resortName = name;
    this.resortID = id;
  }

  /**
   * Simple getter to fetch the ID number of this Resort
   * @return the integer ID of this Resort object
   */
  public int getResortID() {
    return resortID;
  }

  /**
   * Simple getter to fetch the given name of this Resort
   * @return the String name of this Resort object
   */
  public String getResortName() {
    return resortName;
  }
}
