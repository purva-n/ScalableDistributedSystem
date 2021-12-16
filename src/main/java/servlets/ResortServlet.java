package servlets;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;

import daos.ResortDAO;
import servlethelpers.*;

/**
 * Our Resort servlet that will respond to URLs matching the /resorts/* pattern
 */
@WebServlet(name = "ResortServlet", urlPatterns = {"/resorts", "/resorts/*"})
public class ResortServlet extends HttpServlet {
  private final Gson gson = new Gson();

  /**
   * Internal helper function to determine if the given URL is of superficial valid form for
   * further processing.
   * @param parts is the various parts of the URL normally separated by "/"
   * @return true if the URL is of a valid form, false otherwise
   */
  private boolean isValid(String[] parts) {
    // this is our /resorts/{resortID}/seasons path pattern
    if (parts.length == 3) {
      if (!parts[0].equals("") || !parts[2].equals("seasons")) {
        return false;
      }
      // resortID must be an integer to match API
      try {
        int id = Integer.parseInt(parts[1]);
      } catch (NumberFormatException e) {
        return false;
      }
      return true;
    }
    // return false if the path given is some other pattern
    return false;
  }

  /**
   * Helper function for our doGet and doPost that formulates (mostly error) messages to be returned
   * in the response body
   * @param response is the HttpServletResponse whose return code and body we are writing
   * @param code is the integer response code indicating success/failure (e.g. 2XX, 3XX, 4XX, 5XX)
   * @param msg is the string we want entered as the response body
   * @throws IOException if there is an issue writing to the response body
   */
  private void writeMessage(HttpServletResponse response, int code, String msg) throws IOException {
    response.setStatus(code);
    Message err = new Message(msg);
    String jsonErr = gson.toJson(err);
    response.getWriter().write(jsonErr);
  }

  /**
   * This handles the GET requests sent to our servlet. The only valid GET requests are to
   * get the list of resorts or to get the seasons of a specific resort identified by its unique ID.
   * @param request is the GET request received by the servlet
   * @param response is the HTTP response that this servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // our resorts
    ResortList resorts = new ResortDAO().getResortList();
    SeasonList validSeasons = new SeasonList();
    validSeasons.addSeason("2021");

    String path = request.getPathInfo();
    // this means that the /resorts endpoint given
    // return list of resorts
    if (path == null) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(gson.toJson(resorts));
      return;
    }

    // split our path into parts to validate and process
    String[] parts = path.split("/");
    // first ensure basic validity
    if (!isValid(parts)) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "URL or Resort ID invalid");
      return;
    }
     int id = Integer.parseInt(parts[1]);
     if (!resorts.resortExists(id)) {
       writeMessage(response, HttpServletResponse.SC_NOT_FOUND, "Resort not found");
       return;
     }
     // otherwise we've found our resort and return the only valid season of 2021
     response.setStatus(HttpServletResponse.SC_OK);
     response.getWriter().write(gson.toJson(validSeasons));
  }

  /**
   * Helper method for doPost which takes the request body and parses it under the assumption it is
   * JSON formatted.
   * @param request is the original HTTP request received
   * @return the JSON request body split into its key/value pair(s) as a String array
   */
  private String[] parseRequestBody(HttpServletRequest request) {
    try {
      // get the body and read it into a string
      BufferedReader body = request.getReader();
      String lines = body.lines().collect(Collectors.joining());
      lines = lines.replace(" ", "");
      // make parsing a little easier
      lines = lines.replace("{", "");
      lines = lines.replace("}", "");
      return lines.split(":");
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * This handles the POST requests sent to our servlet. The only valid POST request is to add a
   * new season to a specific resort identified by its unique ID.
   * For Assignment 2, this is not used as 2021 is the only valid season.
   * @param request is the POST request received by the servlet
   * @param response is the HTTP response that this servlet will return
   * @throws IOException if there was an issue writing to the response to be returned
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String path = request.getPathInfo();
    // must have a full path
    if (path == null || path.isEmpty()) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "URL missing");
      return;
    }
    // split our path into parts to validate and process
    String[] parts = path.split("/");

    // first ensure basic validity
    if (!isValid(parts)) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "URL or Resort ID invalid");
      return;
    }

    // our resorts
    ResortList resorts = new ResortDAO().getResortList();
    SeasonList validSeasons = new SeasonList();
    validSeasons.addSeason("2021");

    String[] field = parseRequestBody(request);
    // must be "year" followed by a valid 4 character year
    if (field == null || !field[0].equals("\"year\"") || field[1].length() != 6) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
      return;
    }

    // make sure we are safe to cast values to numbers
    try {
      int id = Integer.parseInt(parts[1]);
      int year = Integer.parseInt(field[1].replace("\"", ""));
      if (!resorts.resortExists(id)) {
        writeMessage(response, HttpServletResponse.SC_NOT_FOUND, "Resort not found");
        return;
      }
      // would be writing this year out to database, but not needed for Assignment 3
      response.setStatus(HttpServletResponse.SC_CREATED);
    // year provided must be convertible to a valid 4 digit int
    } catch (NumberFormatException e) {
      writeMessage(response, HttpServletResponse.SC_BAD_REQUEST,
              "Year in invalid format");
    }
  }
}
