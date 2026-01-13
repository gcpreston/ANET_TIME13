package algorithms;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DefaultTeam {
  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    boolean readFromFile = false;

    if (readFromFile)
      return readFromFile("output0.points");

    // saveToFile("output",result);
    return greedy(points, edgeThreshold);
  }

  // Greedily take points with the highest degree until a dominating set is
  // reached.
  private static ArrayList<Point> greedy(ArrayList<Point> points, int edgeThreshold) {
    ArrayList<Point> result = new ArrayList<Point>();
    ArrayList<Point> remaining = new ArrayList<Point>(points);

    while (!isDominatingSet(result, points, edgeThreshold)) {
      Point nextPoint = highestDegree(remaining, edgeThreshold);
      System.out.println("Taking point " + nextPoint + " with degree " + neighbors(nextPoint, points, edgeThreshold).size());
      result.add(nextPoint);
      remaining.remove(nextPoint);
      remaining.removeAll(neighbors(nextPoint, points, edgeThreshold));
    }

    return result;
  }

  private static Point highestDegree(ArrayList<Point> points, int edgeThreshold) {
    Point maxDegreePoint = points.get(0);
    int maxDegree = 0;

    for (Point p : points) {
      List<Point> pNeighbors = neighbors(p, points, edgeThreshold);

      if (pNeighbors.size() > maxDegree) {
        maxDegreePoint = p;
        maxDegree = pNeighbors.size();
      }
    }

    return maxDegreePoint;
  }

  public static List<Point> neighbors(Point p, ArrayList<Point> points, int edgeThreshold) {
    List<Point> result = new ArrayList<Point>();

    for (Point q : points) {
      if (p == q) {
        continue;
      }

      if (p.distance(q) <= edgeThreshold) {
        result.add(q);
      }
    }
    return result;
  }

  private static boolean isDominatingSet(ArrayList<Point> ds, ArrayList<Point> points, int edgeThreshold) {
    Set<Point> remaining = new HashSet<Point>(points);

    for (Point p : ds) {
      remaining.remove(p);
      remaining.removeAll(neighbors(p, points, edgeThreshold));
    }

    return remaining.isEmpty();
  }

  // FILE PRINTER
  private void saveToFile(String filename, ArrayList<Point> result) {
    int index = 0;
    try {
      while (true) {
        BufferedReader input = new BufferedReader(
            new InputStreamReader(new FileInputStream(filename + Integer.toString(index) + ".points")));

        try {
          input.close();
        } catch (IOException e) {
          System.err.println("I/O exception: unable to close " + filename + Integer.toString(index) + ".points");
        }
        index++;
      }
    } catch (FileNotFoundException e) {
      printToFile(filename + Integer.toString(index) + ".points", result);
    }
  }

  private void printToFile(String filename, ArrayList<Point> points) {
    try {
      PrintStream output = new PrintStream(new FileOutputStream(filename));

      for (Point p : points) {
        output.println(Integer.toString((int) p.getX()) + " " + Integer.toString((int) p.getY()));
      }

      output.close();
    } catch (FileNotFoundException e) {
      System.err.println("I/O exception: unable to create " + filename);
    }
  }

  // FILE LOADER
  private ArrayList<Point> readFromFile(String filename) {
    String line;
    String[] coordinates;
    ArrayList<Point> points = new ArrayList<Point>();

    try {
      BufferedReader input = new BufferedReader(
          new InputStreamReader(new FileInputStream(filename)));
      try {
        while ((line = input.readLine()) != null) {
          coordinates = line.split("\\s+");
          points.add(new Point(Integer.parseInt(coordinates[0]),
              Integer.parseInt(coordinates[1])));
        }
      } catch (IOException e) {
        System.err.println("Exception: interrupted I/O.");
      } finally {
        try {
          input.close();
        } catch (IOException e) {
          System.err.println("I/O exception: unable to close " + filename);
        }
      }
    } catch (FileNotFoundException e) {
      System.err.println("Input file not found.");
    }
    return points;
  }
}
