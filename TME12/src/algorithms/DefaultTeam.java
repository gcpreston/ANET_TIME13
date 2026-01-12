package algorithms;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
// import java.util.Collections;
// import java.util.Random;
import java.util.Set;

public class DefaultTeam {

  public ArrayList<Point> calculFVS(ArrayList<Point> points, int edgeThreshold) {
    /* IDEA
     * - Start with FVS of every point
     * - Iterate thru points to remove and find one each time
     * - Once there are no more, look for remove 2 add 1s (TODO)
     */

    ArrayList<Point> fvs = new ArrayList<Point>(points);
    final int GENERATIONS = 150;

    for (int i = 0; i < GENERATIONS; i++) {
      ArrayList<Point> newFvs = evolve(points, fvs, edgeThreshold);

      if (newFvs == fvs) {
        break;
      }

      fvs = newFvs;
    }

    return fvs;
  }

  private ArrayList<Point> evolve(ArrayList<Point> points, ArrayList<Point> fvs, int edgeThreshold) {
    // System.out.println("Starting evolve");
    Evaluation eval = new Evaluation();

    // Step 1: Try removing a point from fvs
    for (int i = 0; i < fvs.size(); i++) {
      ArrayList<Point> testFvs = new ArrayList<Point>(fvs);
      testFvs.remove(i);

      boolean valid = eval.isValid(points, testFvs, edgeThreshold);
      // System.out.println("Size: " + testFvs.size() + ", valid: " + valid);

      if (valid) {
        return testFvs;
      }
    }

    // Step 2: Try adding 2 points and removing 1
    // - Make arraylist of not_included <- points \ fvs
    Set<Point> notIncluded = new HashSet<Point>(points);
    Set<Point> fvsSet = new HashSet<Point>(fvs);
    notIncluded.removeAll(fvsSet);

    // - Iterate over not_included pairs
    for (Point p1 : notIncluded) {
      for (Point p2 : notIncluded) {
        if (p1 == p2) continue;

        // - Iterate over fvs points
        for (Point fvsPoint : fvsSet) {
          Set<Point> testFvsSet = new HashSet<Point>(fvsSet);
          testFvsSet.remove(fvsPoint);
          testFvsSet.add(p1);
          testFvsSet.add(p2);

          ArrayList<Point> testFvs = new ArrayList<Point>(testFvsSet);

          // - If fvs - pair + point isValid, return
          if (eval.isValid(points, testFvs, edgeThreshold)) {
            return testFvs;
          }
        }
      }
    }


    // System.out.println("None valid, returning original of size " + fvs.size() + " (valid: " + eval.isValid(points, fvs, edgeThreshold) + ")");
    return fvs;
  }
}
