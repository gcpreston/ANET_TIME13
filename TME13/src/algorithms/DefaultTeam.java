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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class DefaultTeam {
  private enum SearchMode {
    GREEDY,
    NAIVE_LOCAL,
    GEOMETRIC_K3,
    CONVEX
  }

  private static final SearchMode MODE = SearchMode.GEOMETRIC_K3;
  private static final long LOCAL_SEARCH_TIME_LIMIT_MS = 1500;

  public ArrayList<Point> calculDominatingSet(ArrayList<Point> points, int edgeThreshold) {
    boolean readFromFile = false;

    if (readFromFile)
      return readFromFile("output0.points");

    // saveToFile("output",result);
    return switch (MODE) {
      case GREEDY -> greedy(points, edgeThreshold);
      case NAIVE_LOCAL -> greedy(points, edgeThreshold); // placeholder (partner implementation expected)
      case GEOMETRIC_K3 -> geometricLocalSearchK3(points, edgeThreshold, LOCAL_SEARCH_TIME_LIMIT_MS);
      case CONVEX -> convexOptimizedSearch(points, edgeThreshold, LOCAL_SEARCH_TIME_LIMIT_MS);
    };
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

  private static ArrayList<Point> geometricLocalSearchK3(ArrayList<Point> points, int edgeThreshold, long timeLimitMs) {
    BitSet[] closedNeighborhood = buildClosedNeighborhoods(points, edgeThreshold);
    return localSearchK3(points, edgeThreshold, timeLimitMs, closedNeighborhood);
  }

  private static ArrayList<Point> convexOptimizedSearch(ArrayList<Point> points, int edgeThreshold, long timeLimitMs) {
    BitSet[] closedNeighborhood = buildClosedNeighborhoodsSpatial(points, edgeThreshold);
    return localSearchK3(points, edgeThreshold, timeLimitMs, closedNeighborhood);
  }

  private static ArrayList<Point> localSearchK3(
      ArrayList<Point> points,
      int edgeThreshold,
      long timeLimitMs,
      BitSet[] closedNeighborhood) {
    ArrayList<Point> ds = greedyByCoverage(points, closedNeighborhood);
    if (ds.size() < 3) {
      return ds;
    }

    int n = points.size();
    Map<Point, Integer> indexByPoint = new HashMap<>(n * 2);
    for (int i = 0; i < n; i++) {
      indexByPoint.put(points.get(i), i);
    }

    long deadline = System.nanoTime() + timeLimitMs * 1_000_000L;
    boolean changed = true;

    while (changed && System.nanoTime() < deadline && ds.size() >= 3) {
      changed = false;

      BitSet dsSet = toIndexSet(ds, indexByPoint, n);
      int[] dominationCount = computeDominationCounts(dsSet, closedNeighborhood, n);

      if (removeRedundantVertices(ds, dsSet, dominationCount, closedNeighborhood, indexByPoint, deadline)) {
        changed = true;
        continue;
      }

      int[] orderedPositions = orderDsPositionsByRedundancy(ds, indexByPoint, dominationCount, closedNeighborhood);

      for (int oi = 0; oi < orderedPositions.length && System.nanoTime() < deadline; oi++) {
        for (int oj = oi + 1; oj < orderedPositions.length && System.nanoTime() < deadline; oj++) {
          for (int ok = oj + 1; ok < orderedPositions.length && System.nanoTime() < deadline; ok++) {
            int posA = orderedPositions[oi];
            int posB = orderedPositions[oj];
            int posC = orderedPositions[ok];

            int a = indexByPoint.get(ds.get(posA));
            int b = indexByPoint.get(ds.get(posB));
            int c = indexByPoint.get(ds.get(posC));

            int[] replacement = findK3Replacement(a, b, c, dsSet, dominationCount, closedNeighborhood, points, edgeThreshold);
            if (replacement == null) {
              continue;
            }

            applyReplacementByPositions(ds, points, posA, posB, posC, replacement);
            changed = true;
            break;
          }
          if (changed) {
            break;
          }
        }
        if (changed) {
          break;
        }
      }
    }

    return ds;
  }

  private static ArrayList<Point> greedyByCoverage(ArrayList<Point> points, BitSet[] closedNeighborhood) {
    int n = points.size();
    BitSet notDominated = new BitSet(n);
    notDominated.set(0, n);

    BitSet selected = new BitSet(n);
    ArrayList<Point> ds = new ArrayList<>();

    while (!notDominated.isEmpty()) {
      int best = -1;
      int bestCover = -1;

      for (int u = 0; u < n; u++) {
        if (selected.get(u)) {
          continue;
        }

        BitSet cover = (BitSet) closedNeighborhood[u].clone();
        cover.and(notDominated);
        int coverCount = cover.cardinality();

        if (coverCount > bestCover) {
          best = u;
          bestCover = coverCount;
          if (bestCover == notDominated.cardinality()) {
            break;
          }
        }
      }

      if (best < 0) {
        break;
      }

      selected.set(best);
      ds.add(points.get(best));
      notDominated.andNot(closedNeighborhood[best]);
    }

    return ds;
  }

  private static BitSet[] buildClosedNeighborhoods(ArrayList<Point> points, int edgeThreshold) {
    int n = points.size();
    BitSet[] closedNeighborhood = new BitSet[n];
    for (int i = 0; i < n; i++) {
      closedNeighborhood[i] = new BitSet(n);
      closedNeighborhood[i].set(i);
    }

    for (int i = 0; i < n; i++) {
      Point pi = points.get(i);
      for (int j = i + 1; j < n; j++) {
        if (pi.distance(points.get(j)) <= edgeThreshold) {
          closedNeighborhood[i].set(j);
          closedNeighborhood[j].set(i);
        }
      }
    }

    return closedNeighborhood;
  }

  private static BitSet[] buildClosedNeighborhoodsSpatial(ArrayList<Point> points, int edgeThreshold) {
    int n = points.size();
    int cellSize = Math.max(1, edgeThreshold);
    long threshold2 = (long) edgeThreshold * (long) edgeThreshold;

    Map<Long, ArrayList<Integer>> buckets = new HashMap<>(n * 2);
    int[] cellX = new int[n];
    int[] cellY = new int[n];

    for (int i = 0; i < n; i++) {
      Point p = points.get(i);
      int cx = Math.floorDiv(p.x, cellSize);
      int cy = Math.floorDiv(p.y, cellSize);
      cellX[i] = cx;
      cellY[i] = cy;

      long key = bucketKey(cx, cy);
      buckets.computeIfAbsent(key, unused -> new ArrayList<>()).add(i);
    }

    BitSet[] closedNeighborhood = new BitSet[n];
    for (int i = 0; i < n; i++) {
      BitSet bs = new BitSet(n);
      bs.set(i);

      int cx = cellX[i];
      int cy = cellY[i];
      int px = points.get(i).x;
      int py = points.get(i).y;

      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          ArrayList<Integer> bucket = buckets.get(bucketKey(cx + dx, cy + dy));
          if (bucket == null) {
            continue;
          }

          for (int index : bucket) {
            if (index == i) {
              continue;
            }

            Point q = points.get(index);
            long ddx = (long) px - (long) q.x;
            long ddy = (long) py - (long) q.y;
            if (ddx * ddx + ddy * ddy <= threshold2) {
              bs.set(index);
            }
          }
        }
      }

      closedNeighborhood[i] = bs;
    }

    return closedNeighborhood;
  }

  private static long bucketKey(int cx, int cy) {
    return (((long) cx) << 32) ^ (cy & 0xffffffffL);
  }

  private static BitSet toIndexSet(ArrayList<Point> ds, Map<Point, Integer> indexByPoint, int n) {
    BitSet dsSet = new BitSet(n);
    for (Point p : ds) {
      Integer index = indexByPoint.get(p);
      if (index != null) {
        dsSet.set(index);
      }
    }
    return dsSet;
  }

  private static int[] computeDominationCounts(BitSet dsSet, BitSet[] closedNeighborhood, int n) {
    int[] dominationCount = new int[n];
    for (int u = dsSet.nextSetBit(0); u >= 0; u = dsSet.nextSetBit(u + 1)) {
      for (int v = closedNeighborhood[u].nextSetBit(0); v >= 0; v = closedNeighborhood[u].nextSetBit(v + 1)) {
        dominationCount[v]++;
      }
    }
    return dominationCount;
  }

  private static int[] findK3Replacement(
      int a,
      int b,
      int c,
      BitSet dsSet,
      int[] dominationCount,
      BitSet[] closedNeighborhood,
      ArrayList<Point> points,
      int edgeThreshold) {
    int n = points.size();

    BitSet removed = new BitSet(n);
    removed.set(a);
    removed.set(b);
    removed.set(c);

    BitSet potentiallyAffected = (BitSet) closedNeighborhood[a].clone();
    potentiallyAffected.or(closedNeighborhood[b]);
    potentiallyAffected.or(closedNeighborhood[c]);

    BitSet undominated = new BitSet(n);
    for (int v = potentiallyAffected.nextSetBit(0); v >= 0; v = potentiallyAffected.nextSetBit(v + 1)) {
      int removedDominators = 0;
      if (closedNeighborhood[v].get(a)) {
        removedDominators++;
      }
      if (closedNeighborhood[v].get(b)) {
        removedDominators++;
      }
      if (closedNeighborhood[v].get(c)) {
        removedDominators++;
      }

      if (dominationCount[v] - removedDominators == 0) {
        undominated.set(v);
      }
    }

    if (undominated.isEmpty()) {
      return new int[] { -1, -1 };
    }

    if (hasThreePairwiseFarVertices(undominated, points, edgeThreshold)) {
      return null;
    }

    BitSet outside = new BitSet(n);
    outside.set(0, n);
    outside.andNot(dsSet);

    int pivot = undominated.nextSetBit(0);
    BitSet candidatesX = (BitSet) closedNeighborhood[pivot].clone();
    candidatesX.and(outside);

    for (int x = candidatesX.nextSetBit(0); x >= 0; x = candidatesX.nextSetBit(x + 1)) {
      BitSet remainingToCover = (BitSet) undominated.clone();
      remainingToCover.andNot(closedNeighborhood[x]);

      if (remainingToCover.isEmpty()) {
        return new int[] { x, -1 };
      }

      BitSet yCandidates = (BitSet) outside.clone();
      yCandidates.clear(x);
      for (int v = remainingToCover.nextSetBit(0); v >= 0; v = remainingToCover.nextSetBit(v + 1)) {
        yCandidates.and(closedNeighborhood[v]);
        if (yCandidates.isEmpty()) {
          break;
        }
      }

      int y = yCandidates.nextSetBit(0);
      if (y >= 0) {
        return new int[] { x, y };
      }
    }

    return null;
  }

  private static boolean hasThreePairwiseFarVertices(BitSet vertices, ArrayList<Point> points, int edgeThreshold) {
    double limit = 2.0 * edgeThreshold;
    int[] reps = new int[3];
    int repCount = 0;

    for (int v = vertices.nextSetBit(0); v >= 0; v = vertices.nextSetBit(v + 1)) {
      boolean farFromAll = true;
      for (int i = 0; i < repCount; i++) {
        if (points.get(v).distance(points.get(reps[i])) <= limit) {
          farFromAll = false;
          break;
        }
      }
      if (!farFromAll) {
        continue;
      }

      reps[repCount++] = v;
      if (repCount >= 3) {
        return true;
      }
    }

    return false;
  }

  private static void applyReplacementByPositions(ArrayList<Point> ds, ArrayList<Point> points, int posA, int posB, int posC, int[] replacement) {
    int[] positions = new int[] { posA, posB, posC };
    Arrays.sort(positions);
    ds.remove(positions[2]);
    ds.remove(positions[1]);
    ds.remove(positions[0]);

    if (replacement[0] >= 0) {
      ds.add(points.get(replacement[0]));
    }
    if (replacement[1] >= 0) {
      ds.add(points.get(replacement[1]));
    }
  }

  private static boolean removeRedundantVertices(
      ArrayList<Point> ds,
      BitSet dsSet,
      int[] dominationCount,
      BitSet[] closedNeighborhood,
      Map<Point, Integer> indexByPoint,
      long deadline) {
    boolean removedAny = false;

    for (int i = 0; i < ds.size() && System.nanoTime() < deadline;) {
      int u = indexByPoint.get(ds.get(i));
      BitSet uNeighborhood = closedNeighborhood[u];

      boolean removable = true;
      for (int v = uNeighborhood.nextSetBit(0); v >= 0; v = uNeighborhood.nextSetBit(v + 1)) {
        if (dominationCount[v] <= 1) {
          removable = false;
          break;
        }
      }

      if (!removable) {
        i++;
        continue;
      }

      ds.remove(i);
      dsSet.clear(u);
      for (int v = uNeighborhood.nextSetBit(0); v >= 0; v = uNeighborhood.nextSetBit(v + 1)) {
        dominationCount[v]--;
      }
      removedAny = true;
    }

    return removedAny;
  }

  private static int[] orderDsPositionsByRedundancy(
      ArrayList<Point> ds,
      Map<Point, Integer> indexByPoint,
      int[] dominationCount,
      BitSet[] closedNeighborhood) {
    int m = ds.size();
    long[] keyedPositions = new long[m];

    for (int pos = 0; pos < m; pos++) {
      int u = indexByPoint.get(ds.get(pos));
      int uniqueCovered = 0;
      BitSet uNeighborhood = closedNeighborhood[u];
      for (int v = uNeighborhood.nextSetBit(0); v >= 0; v = uNeighborhood.nextSetBit(v + 1)) {
        if (dominationCount[v] == 1) {
          uniqueCovered++;
        }
      }
      keyedPositions[pos] = (((long) uniqueCovered) << 32) | (pos & 0xffffffffL);
    }

    Arrays.sort(keyedPositions);

    int[] orderedPositions = new int[m];
    for (int i = 0; i < m; i++) {
      orderedPositions[i] = (int) keyedPositions[i];
    }
    return orderedPositions;
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
