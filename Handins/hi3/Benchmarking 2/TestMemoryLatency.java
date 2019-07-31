// PCPP week 4
// sestoft@itu.dk * 2016-08-29

import java.util.ArrayList;
import java.util.Collections;
import java.util.BitSet;
import java.util.function.IntToDoubleFunction;

public class TestMemoryLatency {
  public static void main(String[] args) {
    latencies();
    // bandwidths();
  }

  private static void latencies() {
    SystemInfo();
    final int K = 1 << 10, KB = K / 4, MB = K * KB, maxSize = 128 * MB;
    System.out.printf("Memory latency: each test performs %,d identical loop iterations.%n", maxSize);
    System.out.printf("Time is ns/iteration; any variation is caused by memory (cache) effects.%n");
    final int[] arr = new int[maxSize];
    for (int size = 8*KB; size <= maxSize; size *= 2) {
      fillArray(arr, size);  
      Mark7(String.format("Size %,11d B", size*4), arr.length, 
	    (int i) -> jump(arr));
    }
  }

  // Fill arr[0..S-1] in such a way that it contains a random cyclic
  // chain of indices of length S, starting and ending at arr[0].
  private static void fillArray(int[] arr, int S) {
    ArrayList<Integer> indexes = new ArrayList<>();
    for (int k=0; k<S; k++) 
      indexes.add(k);
    Collections.shuffle(indexes);    
    // Make indexes[0] == 0.
    int shift = S - indexes.get(0);
    for (int k=0; k<S; k++) 
      indexes.set(k, (indexes.get(k) + shift) % S);
    // Make arr[0] = indexes[1], arr[arr[0]] = indexes[2], 
    // ..., arr^k[0] = indexes[k], ..., arr^S[0] = 0;
    // the cyclic chain starting at arr[0] will have length S.
    int i = 0;
    for (int k=0; k<S; k++) 
      i = arr[i] = indexes.get((k+1) % S);
  }

  // The length of the index chain starting at arr[0]; for debugging
  private static int coverage(int[] arr) {
    final BitSet indexes = new BitSet(arr.length);
    int k = 0;
    do {
      indexes.set(k);
      k = arr[k];
    } while (!indexes.get(k));
    return indexes.cardinality();
  }

  private static double jump(int[] arr) { 
    int k = 0;
    for (int j=0; j<arr.length; j++) 
      k = arr[k];
    return k;
  }

  private static void bandwidths() {
    SystemInfo();
    final int K = 1 << 10, KB = K / 8, MB = K * KB, maxSize = 128 * MB;
    System.out.printf("Memory bandwith: maxSize = %d%n", maxSize);
    final long[] arr = new long[maxSize];
    for (int size = 8*KB; size <= maxSize; size *= 2) {
      for (int j=0; j<size; j++)
	arr[j] = j;
      int theSize = size;
      Mark7(String.format("Size %9d B", size*4), theSize, 
	    (int i) -> run(arr, theSize));
    }
  }

  private static double run(long[] arr, int size) { 
    long dummy = 0;
    for (int j=0; j<size; j++) 
      dummy += arr[j];
    return dummy;
  }

  // ========== Infrastructure code ==========

  public static void SystemInfo() {
    System.out.printf("# OS:   %s; %s; %s%n", 
                      System.getProperty("os.name"), 
                      System.getProperty("os.version"), 
                      System.getProperty("os.arch"));
    System.out.printf("# JVM:  %s; %s%n", 
                      System.getProperty("java.vendor"), 
                      System.getProperty("java.version"));
    // The processor identifier works only on MS Windows:
    System.out.printf("# CPU:  %s; %d \"cores\"%n", 
		      System.getenv("PROCESSOR_IDENTIFIER"),
		      Runtime.getRuntime().availableProcessors());
    java.util.Date now = new java.util.Date();
    System.out.printf("# Date: %s%n", 
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now));
  }

  public static double Mark7(String msg, int jumps, IntToDoubleFunction f) {
    int n = 10, count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do { 
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) 
          dummy += f.applyAsDouble(i);
        runningTime = t.check();
        double time = runningTime * 1e9 / count;
        st += time; 
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean/jumps, sdev/jumps, count);
    return dummy / totalCount;
  }

  // Crude wall clock timing utility, measuring time in seconds
   
  static class Timer {
    private long start, spent = 0;
    public Timer() { play(); }
    public double check() { return (System.nanoTime()-start+spent)/1e9; }
    public void pause() { spent += System.nanoTime()-start; }
    public void play() { start = System.nanoTime(); }
  }  
}
