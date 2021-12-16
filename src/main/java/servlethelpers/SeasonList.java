package servlethelpers;

import java.util.ArrayList;

/**
 * Mostly a wrapper class around an ArrayList for proper JSON display value conforming to
 * Swagger API spec
 */
public class SeasonList {
  private ArrayList<String> seasons;

  /**
   * Simple constructor that initializes our underlying ArrayList
   */
  public SeasonList() {
    this.seasons = new ArrayList<>();
  }

  /**
   * Adds the given year to our seasons list, but expects the season to be non-null and convertible
   * to an integer
   * @param season is the numeric year given as a String
   * @throws IllegalArgumentException if a String is given that cannot be converted to an int
   *    or if the given String exceeds 4 characters
   * @throws NullPointerException if a null String is given
   */
  public void addSeason(String season) throws IllegalArgumentException, NullPointerException {
    if (season == null) {
      throw new NullPointerException("Season cannot be null");
    } else if (season.length() == 4) {
      try {
        int seasonTest = Integer.parseInt(season);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Season must be a valid number as String");
      }
      this.seasons.add(season);
    } else {
      throw new IllegalArgumentException("Season must be 4 characters");
    }
  }

  /**
   * Simply returns a copy of the seasons this list currently holds
   * @return a copied ArrayList of the seasons in this list
   */
  public ArrayList<String> getSeasons() {
    return new ArrayList<>(this.seasons);
  }
}
