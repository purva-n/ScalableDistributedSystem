package servlethelpers;

/**
 * Mostly a wrapper class around a String for proper JSON display value conforming to our
 * Swagger API spec. Used to represent a server message, i.e. an error message, via an
 * HTTP response body.
 */
public class Message {
  private String message;

  /**
   * Simply initializes this object to hold the given message
   * @param msg the String message this object represents
   */
  public Message(String msg) {
    this.message = msg;
  }

  /**
   * Simple getter method to retrieve the String message this object represents
   * @return the String message that this object holds
   */
  public String getMessage() {
    return message;
  }
}