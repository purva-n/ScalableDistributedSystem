package servlethelpers;

import java.util.ArrayList;

/**
 * Mostly a wrapper class around an ArrayList for proper JSON display value conforming to
 * Swagger API spec
 */
public class ResortList {
  private ArrayList<Resort> resorts;

  /**
   * Simple constructor that initializes our underlying ArrayList
   */
  public ResortList() {
    this.resorts = new ArrayList<>();
  }

  /**
   * Used add a new resort to the list and does mutate the internal list
   * @param resort is the new Resort object we would like to add
   * @throws IllegalArgumentException if a resort with the same ID has already been added
   */
  public void addResort(Resort resort) throws IllegalArgumentException {
    for (Resort curResort : resorts) {
      if (resort.getResortID() == curResort.getResortID()) {
        throw new IllegalArgumentException("Resort already added to this list");
      }
    }
    resorts.add(resort);
  }

  /**
   * Checks through our resort list to check if the given ID matches a resort in our list
   * @param id the integer ID of the resort to be searched for
   * @return true if the resort ID is found, false otherwise
   */
  public boolean resortExists(int id) {
    for (Resort resort : resorts) {
      if (id == resort.getResortID()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks through our resort list to check if the given name matches a resort name in our list
   * @param name is the String name of the resort to be searched for
   * @return true if the resort name is found, false otherwise
   */
  public boolean resortExists(String name) {
    for (Resort resort : resorts) {
      if (name.equals(resort.getResortName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks through our resort list to get the ID of the given resort name. Assumes all resort IDs
   * are positive integers.
   * @param name is the resort name whose integer ID is returned
   * @return the integer ID of the given resort name if found, -1 otherwise
   */
  public int getID(String name) {
    for (Resort resort : resorts) {
      if (name.equals(resort.getResortName())) {
        return resort.getResortID();
      }
    }
    return -1;
  }
}
