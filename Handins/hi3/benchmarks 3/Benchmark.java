// Simple microbenchmark setups
// sestoft@itu.dk * 2013-06-02, 2015-09-15

import java.util.function.IntToDoubleFunction;

class Benchmark {
  public static void main(String[] args) {
    SystemInfo();
    //Mark0();
    //Mark1();
    //Mark2();
    //Mark3();
    //Mark4();
    //Mark5();
    //Mark6("multiply", Benchmark::multiply);
    //Mark7("multiply", Benchmark::multiply);
    MathFunctionBenchmarks();
    //final java.util.Random rnd = new java.util.Random();
    //final int n = 1638400;
    //Mark8("random_index", i -> rnd.nextInt(n));
    //SearchBenchmarks();
    //SearchScalabilityBenchmarks1();
    //SearchScalabilityBenchmarks2();
    //GetPseudorandomItems();
    //SortingBenchmarks();
    //SortingScalabilityBenchmarks();
  }

  // ========== Example functions and benchmarks ==========

  private static double multiply(int i) {
    double x = 1.1 * (double)(i & 0xFF);
     return x * x * x * x * x * x * x * x * x * x
          * x * x * x * x * x * x * x * x * x * x;
  }

  private static void MathFunctionBenchmarks() {
    Mark7("pow", i -> Math.pow(10.0, 0.1 * (i & 0xFF)));
    Mark7("exp", i -> Math.exp(0.1 * (i & 0xFF)));
    Mark7("log", i -> Math.log(0.1 + 0.1 * (i & 0xFF)));
    Mark7("sin", i -> Math.sin(0.1 * (i & 0xFF)));
    Mark7("cos", i -> Math.cos(0.1 * (i & 0xFF)));
    Mark7("tan", i -> Math.tan(0.1 * (i & 0xFF)));
    Mark7("asin", i -> Math.asin(1.0/256.0 * (i & 0xFF)));
    Mark7("acos", i -> Math.acos(1.0/256.0 * (i & 0xFF)));
    Mark7("atan", i -> Math.atan(1.0/256.0 * (i & 0xFF)));
  }

  private static void SearchBenchmarks() {
    final int[] intArray = SearchAndSort.fillIntArray(10_000);  // sorted [0,1,...]
    final int successItem = 4900, failureItem = 14000;
    Mark7("linear_search_success",
          i -> SearchAndSort.linearSearch(successItem, intArray));
    Mark7("binary_search_success",
          i -> SearchAndSort.binarySearch(successItem, intArray));
  }

  private static void SearchScalabilityBenchmarks1() {
    for (int size = 100; size <= 10_000_000; size *= 2) {
      final int[] intArray = SearchAndSort.fillIntArray(size);  // sorted [0,1,...]
      final int successItem = (int)(0.49 * size);
      Mark8("binary_search_success",
            String.format("%8d", size),
            i -> SearchAndSort.binarySearch(successItem, intArray));
    }
  }

  private static void SearchScalabilityBenchmarks2() {
    for (int size = 100; size <= 10_000_000; size *= 2) {
      final int[] intArray = SearchAndSort.fillIntArray(size);  // sorted [0,1,...]
      final int[] items = SearchAndSort.fillIntArray(size);
      final int n = size;
      SearchAndSort.shuffle(items);
      Mark8("binary_search_success",
            String.format("%8d", size),
            i -> {
              int successItem = items[i % n];
              return SearchAndSort.binarySearch(successItem, intArray);
            });
    }
  }

  private static void GetPseudorandomItems() {
    for (int size = 100; size <= 10_000_000; size *= 2) {
      final int[] items = SearchAndSort.fillIntArray(size);
      final int n = size;
      SearchAndSort.shuffle(items);
      Mark8("get_pseudorandom_items",
            String.format("%8d", size),
            i -> {
              int successItem = items[i % n];
              return successItem; });
    }
  }

  private static void SortingBenchmarks() {
    final int[] intArray = SearchAndSort.fillIntArray(10_000);
    Mark7("shuffle int",
          i -> { SearchAndSort.shuffle(intArray); return 0.0; });
    Mark8Setup("shuffle",
       new Benchmarkable() {
         public double applyAsDouble(int i)
         { SearchAndSort.shuffle(intArray); return 0.0; } });
    Mark8Setup("selection_sort",
       new Benchmarkable() {
         public void setup() { SearchAndSort.shuffle(intArray); }
         public double applyAsDouble(int i)
         { SearchAndSort.selsort(intArray); return 0.0; } });
    Mark8Setup("quicksort",
       new Benchmarkable() {
         public void setup() { SearchAndSort.shuffle(intArray); }
         public double applyAsDouble(int i)
         { SearchAndSort.quicksort(intArray); return 0.0; } });
    Mark8Setup("heapsort",
       new Benchmarkable() {
         public void setup() { SearchAndSort.shuffle(intArray); }
         public double applyAsDouble(int i)
         { SearchAndSort.heapsort(intArray); return 0.0; } });
  }

  private static void SortingScalabilityBenchmarks() {
    for (int size = 100; size <= 50000; size *= 2) {
      final int[] intArray = SearchAndSort.fillIntArray(size);
      Mark8Setup("selection_sort",
                 String.format("%8d", size),
                 new Benchmarkable() {
                   public void setup() { SearchAndSort.shuffle(intArray); }
                   public double applyAsDouble(int i)
                   { SearchAndSort.selsort(intArray); return 0.0; } });
    }
    System.out.printf("%n%n"); // data set divider
    for (int size = 100; size <= 2000000; size *= 2) {
      final int[] intArray = SearchAndSort.fillIntArray(size);
      Mark8Setup("quicksort",
                 String.format("%8d", size),
                 new Benchmarkable() {
                   public void setup() { SearchAndSort.shuffle(intArray); }
                   public double applyAsDouble(int i)
                   { SearchAndSort.quicksort(intArray); return 0.0; } });
    }
    System.out.printf("%n%n"); // data set divider
    for (int size = 100; size <= 2000000; size *= 2) {
      final int[] intArray = SearchAndSort.fillIntArray(size);
      Mark8Setup("heapsort",
                 String.format("%8d", size),
                 new Benchmarkable() {
                   public void setup() { SearchAndSort.shuffle(intArray); }
                   public double applyAsDouble(int i)
                   { SearchAndSort.heapsort(intArray); return 0.0; } });
    }
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

  public static void Mark0() {         // USELESS
    Timer t = new Timer();
    double dummy = multiply(10);
    double time = t.check() * 1e9;
    System.out.printf("%6.1f ns%n", time);
  }

  public static void Mark1() {         // NEARLY USELESS
    Timer t = new Timer();
    int count = 100_000_000;
    for (int i=0; i<count; i++) {
      double dummy = multiply(i);
    }
    double time = t.check() * 1e9 / count;
    System.out.printf("%6.1f ns%n", time);
  }

  public static double Mark2() {
    Timer t = new Timer();
    int count = 100_000_000;
    double dummy = 0.0;
    for (int i=0; i<count; i++)
      dummy += multiply(i);
    double time = t.check() * 1e9 / count;
    System.out.printf("%6.1f ns%n", time);
    return dummy;
  }

  public static double Mark3() {
    int n = 10;
    int count = 100_000_000;
    double dummy = 0.0;
    for (int j=0; j<n; j++) {
      Timer t = new Timer();
      for (int i=0; i<count; i++)
        dummy += multiply(i);
      double time = t.check() * 1e9 / count;
      System.out.printf("%6.1f ns%n", time);
    }
    return dummy / n;
  }

  public static double Mark4() {
    int n = 10;
    int count = 100_000_000;
    double dummy = 0.0;
    double st = 0.0, sst = 0.0;
    for (int j=0; j<n; j++) {
      Timer t = new Timer();
      for (int i=0; i<count; i++)
        dummy += multiply(i);
      double time = t.check() * 1e9 / count;
      st += time;
      sst += time * time;
    }
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%6.1f ns +/- %6.3f%n", mean, sdev);
    return dummy / n;
  }

  public static double Mark5() {
    int n = 10, count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do {
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++)
          dummy += multiply(i);
        runningTime = t.check();
        double time = runningTime * 1e9 / count;
        st += time;
        sst += time * time;
        totalCount += count;
      }
      double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
      System.out.printf("%6.1f ns +/- %8.2f %10d%n", mean, sdev, count);
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    return dummy / totalCount;
  }

  public static double Mark6(String msg, IntToDoubleFunction f) {
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
      double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
      System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean, sdev, count);
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    return dummy / totalCount;
  }

  public static double Mark7(String msg, IntToDoubleFunction f) {
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
    System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean, sdev, count);
    return dummy / totalCount;
  }

  public static double Mark8(String msg, String info, IntToDoubleFunction f,
                             int n, double minTime) {
    int count = 1, totalCount = 0;
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
    } while (runningTime < minTime && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
    return dummy / totalCount;
  }

  public static double Mark8(String msg, IntToDoubleFunction f) {
    return Mark8(msg, "", f, 10, 0.25);
  }

  public static double Mark8(String msg, String info, IntToDoubleFunction f) {
    return Mark8(msg, info, f, 10, 0.25);
  }

  public static double Mark8Setup(String msg, String info, Benchmarkable f,
                                  int n, double minTime) {
    int count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do {
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) {
          t.pause();
          f.setup();
          t.play();
          dummy += f.applyAsDouble(i);
        }
        runningTime = t.check();
        double time = runningTime * 1e9 / count;
        st += time;
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < minTime && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
    return dummy / totalCount;
  }

  public static double Mark8Setup(String msg, Benchmarkable f) {
    return Mark8Setup(msg, "", f, 10, 0.25);
  }

  public static double Mark8Setup(String msg, String info, Benchmarkable f) {
    return Mark8Setup(msg, info, f, 10, 0.25);
  }
}
