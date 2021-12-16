package client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SkierClientMultiThreaded {

  private final static int MAX_THREADS = 256;
  private final static int MAX_LIFTS = 60;
  private final static int MIN_LIFTS = 5;
  private final static int MAX_SKIERS = 50000;
  private final static int DEF_LIFTS = 40;
  private final static int DEF_TIME = 15;
  private final static CountDownLatch doneBarrier = new CountDownLatch(1);
  private static boolean doneFlag = false;
  private static int lastNumPosts = 0;
  private static int lastNumGets = 0;
  private static int requestsTimerSecs = 0;
  private static int port;
  private static String hostname;

  /**
   * Entry point into our program that takes a set of command line arguments and sends
   * a large number of POST/GET requests to our server.
   * @param args is our command line arguments. It expects the arguments to be of form (in any order):
   *            [-T desiredTestTime -t numThreads -s numSkiers -h hostname -p port -l numLifts].
   *             testTime & numLifts are optional (defaults will be used)
   * @throws NumberFormatException if the number of threads, skiers, port, lifts, or runs
   *                               could not be converted to an integer
   */
  public static void main(String[] args) throws InterruptedException, NumberFormatException {
    int numThreads = -1;
    int numSkiers = -1;
    int numLifts = DEF_LIFTS;
    // test time in minutes
    int testTime = DEF_TIME;
    // parse command line input
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-T":
          testTime = Integer.parseInt(args[i+1]);
          break;
        case "-t":
          numThreads = Integer.parseInt(args[i+1]);
          break;
        case "-s":
          numSkiers = Integer.parseInt(args[i+1]);
          break;
        case "-h":
          hostname = args[i+1];
          break;
        case "-p":
          port = Integer.parseInt(args[i+1]);
          break;
        case "-l":
          numLifts = Integer.parseInt(args[i+1]);
          break;
      }
    }
    // error check & run
    validateArgs(numThreads, numSkiers, numLifts, testTime);
    runTests(numThreads, numSkiers, numLifts, testTime);
  }

  /**
   * Checks the given integers to confirm they conform to valid ranges
   * @param numThreads is the total number of threads for the tests to use
   * @param numSkiers is the total number of skiers for the tests to use
   * @param numLifts is the total number of lifts for the tests to use
   * @param testTime is the total amount of time (in minutes) for the tests to run
   * @throws IllegalArgumentException if hostname is null, any of the integer values are
   *                                  less than 1 (or 5 for lifts), or if threads,
   *                                  skiers, or lifts exceeds max values
   */
  private static void validateArgs(int numThreads, int numSkiers, int numLifts, int testTime)
          throws IllegalArgumentException {
    if (numThreads > MAX_THREADS || numThreads < 1) {
      throw new IllegalArgumentException("Too many or too few threads provided");
    } else if (numSkiers > MAX_SKIERS || numSkiers < 1) {
      throw new IllegalArgumentException("Too many or too few skiers provided");
    } else if (numLifts > MAX_LIFTS || numLifts < MIN_LIFTS) {
      throw new IllegalArgumentException("Too many or too few lifts provided");
    } else if (testTime < 1){
      throw new IllegalArgumentException("Time must be a positive integer");
    } else if (hostname == null || port < 1) {
      throw new IllegalArgumentException("Invalid hostname or port");
    }
  }

  /**
   * Starts our tests which launch a given number of threads that will launch POST and GET
   * requests against our server until the specified amount of time has elapsed.
   * @param numThreads is the number of threads to launch
   * @param numSkiers is the number of skiers each thread can use
   * @param numLifts is the number of ski lifts each thread can use
   * @param testTime is the total time (in minutes) the tests should run
   * @throws InterruptedException if an error occurred while waiting on the countdown latch
   */
  private static void runTests(int numThreads, int numSkiers, int numLifts, long testTime)
          throws InterruptedException {
    long startTime = System.currentTimeMillis();
    // set up our timer that will flip the done flag & our periodic request printout
    Runnable doneSwitch = () -> { doneFlag = true; doneBarrier.countDown(); };
    Runnable requestsMade = () -> { int newGets = SkierClientThread.getTotalGets();
      int newPosts = SkierClientThread.getTotalPosts();
      requestsTimerSecs += 5;
      System.out.println("Seconds elapsed: " + requestsTimerSecs +
              "\nGETs/sec: " + ((newGets - lastNumGets) / 5) +
              "\nPOSTs/sec: " + ((newPosts - lastNumPosts) / 5) + "\n---");
      lastNumGets = newGets; lastNumPosts = newPosts;
    };
    ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(2);
    timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

    // launch our threads
    SkierClientThread[] phaseThreads = launchPhase(numThreads, numSkiers, numLifts,
            1, 420);
    // start our test timer & reqs/sec printout
    timer.schedule(doneSwitch, testTime, TimeUnit.MINUTES);
    timer.scheduleAtFixedRate(requestsMade,5, 5, TimeUnit.SECONDS);

    // wait for test time to finish
    doneBarrier.await();
    // make sure all of our threads finished
    for (SkierClientThread tid : phaseThreads) {
      tid.join();
    }
    long endTime = System.currentTimeMillis();
    // print our final stats (runtime in seconds)
    printStats(startTime, endTime);
    timer.shutdown();
  }

  /**
   * Takes the start and end time of our testing phase and prints a series of statistics
   * @param startTime is the time (in ms) that our test phase began
   * @param endTime is the time (in ms) that our test phase ended
   */
  private static void printStats(long startTime, long endTime) {
    // get all of our stats
    int runtime = ((int) (endTime - startTime)) / 1000;
    int totalReqs = SkierClientThread.getTotalGets() + SkierClientThread.getTotalPosts();

    // print summary
    System.out.println("Terminating...");
    System.out.println("Success: " + SkierClientThread.getSuccessCount());
    System.out.println("Fail: " + SkierClientThread.getFailureCount());
    System.out.println("Total Requests/second GET: " + SkierClientThread.getTotalGets() / runtime);
    System.out.println("Total Requests/second POST:" + SkierClientThread.getTotalGets() / runtime);
    System.out.println("Total Run Time: " + runtime + " seconds");
    System.out.println("Total requests/second: " + (totalReqs / runtime) + "\n---");
    printHistogram("POST", SkierClientThread.getPostHistogram(),
            SkierClientThread.getPostOverflow());
    printHistogram("GET", SkierClientThread.getGetHistogram(),
            SkierClientThread.getGetOverflow());
  }

  /**
   * Simply prints all non-zero indexes and entries in a given response time histogram
   * @param operation is the HTTP operation that the histogram is for
   * @param histogram is the integer array holding the counter for response times
   * @param overflow is the number of response times that is larger than the length
   *                 of the histogram
   */
  private static void printHistogram(String operation, int[] histogram, int overflow) {
    System.out.println("Operation Histogram: " + operation);
    // only care about non-zero entries
    for (int i = 0; i < histogram.length; i++) {
      if (histogram[i] != 0) {
        System.out.println("(" + i + ") - " + histogram[i]);
      }
    }
    if (overflow != 0) {
      System.out.println("(>=" + histogram.length + ") - " + overflow);
    }
    System.out.println("---");
  }

  /**
   * Launches a series of threads that will process a series of POST/GET requests against our
   * remote server for load testing.
   * @param numThreads is the total number of threads this phase will spawn
   * @param startTime is the minimum time of the ski day this phase sends requests for
   * @param endTime is the maximum time of the ski day this phase sends requests for
   * @return an array of the thread objects this phase spawned so they can be joined
   */
  private static SkierClientThread[] launchPhase(int numThreads, int numSkiers, int numLifts,
                                                 int startTime, int endTime) {
    SkierClientThread[] tids = new SkierClientThread[numThreads];
    System.out.println("Starting threads...");

    for (int i = 0; i < numThreads; i++) {
      // The first and last Skier ID each thread would be handling
      int startSkierID = i * (numSkiers / numThreads) + 1;
      int endSkierID = startSkierID + (numSkiers / numThreads);
      SkierClientThread sct = new SkierClientThread(hostname, port, startSkierID, endSkierID,
              startTime, endTime, numLifts);
      tids[i] = sct;
      sct.start();
    }
    return tids;
  }

  /**
   * Simply checks the status of our done flag indicating when the test should conclude
   * @return true if the tests are done and should end, false otherwise
   */
  public static boolean isDone() {
    return doneFlag;
  }
}
