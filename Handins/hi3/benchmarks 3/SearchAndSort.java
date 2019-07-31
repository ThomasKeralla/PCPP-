// Example code for microbenchmark note
// sestoft@itu.dk * 2013-08-01

public class SearchAndSort {
  static int linearSearch(int x, int[] arr) {
    int n = arr.length, i = 0;                             
    while (i < n) 
      if (arr[i] != x) 
        i++;
      else 
        return i;                     
    return -1;
  }
  
  static int binarySearch(int x, int[] arr) {
    int n = arr.length, a = 0, b = n-1;                 
    while (a <= b) {                                 
      int i = (a+b) / 2;
      if (x < arr[i]) 
        b = i-1;
      else if (arr[i] < x) 
        a = i+1;
      else 
        return i;                  
    }                                 
    return -1;
  }

  // Utility for sorting
  private static void swap(int[] arr, int s, int t) {
    int tmp = arr[s];  arr[s] = arr[t];  arr[t] = tmp;
  }

  // Selection sort
  public static void selsort(int[] arr) { 
    int n = arr.length;
    for (int i = 0; i < n; i++) {
      int least = i;                                      
      for (int j = i+1; j < n; j++) 
        if (arr[j] < arr[least])
          least = j;
      swap(arr, i, least);
    }
  }

  // Quicksort
  private static void qsort(int[] arr, int a, int b) { 
    // sort arr[a..b]
    if (a < b) { 
      int i = a, j = b;
      int x = arr[(i+j) / 2];                
      do {                                   
        while (arr[i] < x) i++;              
        while (arr[j] > x) j--; 
        if (i <= j) {
          swap(arr, i, j);
          i++; j--;
        }                                    
      } while (i <= j);                      
      qsort(arr, a, j);                      
      qsort(arr, i, b);                      
    }                                        
  }

  public static void quicksort(int[] arr) {
    qsort(arr, 0, arr.length-1);
  }

  // Heapsort
  private static void heapify(int[] arr, int i, int k) {
    // heapify node arr[i] in the tree arr[0..k]
    int j = 2 * i + 1;                          
    if (j <= k) {
      if (j+1 <= k && arr[j] < arr[j+1])
        j++;                                  
      if (arr[i] < arr[j]) {
        swap(arr, i, j);                    
        heapify(arr, j, k);                 
      }           
    }                                         
  }

  public static void heapsort(int[] arr) {
    int n = arr.length;
    for (int m=n/2; m >= 0; m--) 
      heapify(arr, m, n-1);
    for (int m=n-1; m >= 1; m--) { 
      swap(arr, 0, m);           
      heapify(arr, 0, m-1);      
    }                            
  }

  public static int[] fillIntArray(int n) {
    int [] arr = new int[n];
    for (int i = 0; i < n; i++)
      arr[i] = i;
    return arr;
  }

  private static final java.util.Random rnd = new java.util.Random();

  public static void shuffle(int[] arr) {
    for (int i = arr.length-1; i > 0; i--)
      swap(arr, i, rnd.nextInt(i+1));
  }
}
