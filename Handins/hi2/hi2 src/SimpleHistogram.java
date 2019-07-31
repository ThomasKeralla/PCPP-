// For week 2
// sestoft@itu.dk * 2014-09-04
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;
class SimpleHistogram {
  public static void main(String[] args) {
    final Histogram2 histogram = new Histogram2(30);
    final int range = 5_000_000;
    final  int count = 0;
    final int threadsN = 10;
	final Thread[] threads = new Thread[threadsN];

  final long startTime = System.currentTimeMillis();


	for(int i =0; i<threadsN; i++){

		final int intStart = (i*(range/threadsN));
		final int intEnd = ((i+1)*(range/threadsN));

		threads[i] = new Thread(() -> {
			 for(int j=intStart; j< intEnd; j++){
		  		int factors = countFactors(j);
		  		histogram.increment(factors);
			}
		});
	   threads[i].start();
	}

	try {
		for(int t=0;  t<threadsN; t++){
			threads[t].join();
		}
  	} catch (InterruptedException e){}
      final long endTime = System.currentTimeMillis();
  	//System.out.println(histogram.getBins()[3]);
    dump(histogram);
    System.out.println(startTime + " ms " + endTime + " ms " + (endTime - startTime) + " ms ");
  }

  private static int countFactors(int p) {
           if (p < 2){  return 0;}
           int factorCount = 1;
           int k = 2;
           while (p >= k * k) {
                 if (p % k == 0) {
                    factorCount++;
                    p /= k;
                 }else{
                  k++;
                 }
          }
      return factorCount;
  }

  public static void dump(Histogram histogram) {
    int totalCount = 0;
    for (int bin=0; bin<histogram.getSpan(); bin++) {
      System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));

      totalCount += histogram.getCount(bin);
    }
    System.out.printf("      %9d%n", totalCount);
  }
}

interface Histogram {
  public void increment(int bin);
  public int getCount(int bin);
  public int getSpan();
  public int[] getBins();
}

class Histogram1 implements Histogram {
  private int[] counts;
  public Histogram1(int span) {
    this.counts = new int[span];
  }
  public void increment(int bin) {
    counts[bin] = counts[bin] + 1;
  }
  public int getCount(int bin) {
    return counts[bin];
  }
  public int getSpan() {
    return counts.length;
  }
  public int[] getBins(){
  	return null;
  }
 }

class Histogram2 implements Histogram {
  private int[] counts;
     public Histogram2(int span) {
       this.counts = new int[span];
     }
     public void increment(int bin) {
      synchronized(this){
       	counts[bin] = counts[bin] + 1;
       }
     }
     public int getCount(int bin) {
      synchronized(this){
       return counts[bin];
      }
     }
     public final int getSpan() {
       return counts.length;
     }
     public int[] getBins(){
      synchronized(this){
      	int[] arr = new int[counts.length];

     	for(int i=0; i<counts.length; i++){
     		arr[i] = counts[i];
     	}

     	return arr;
     	}
     }
}

class Histogram3 implements Histogram {

	private  List<AtomicInteger> myList;

	public Histogram3(int span){
		myList = new ArrayList<>(span);
		for(int i= 0; i<span; i++){
			myList.add(i,new AtomicInteger(0));
		}
	}

	public void increment(int bin){
		AtomicInteger var = myList.get(bin);
	   	var.getAndIncrement();
	     myList.set(bin,var);
	}
	public int getCount(int bin){

		return myList.get(bin).intValue();
	}
	public int getSpan(){
		return myList.size();
	}

	public int[] getBins(){
		int[] arr = new int[getSpan()];
		for(int i=0; i<arr.length; i++){
			arr[i] = getCount(i);
		}
	    return arr;
	}
}

class Histogram4 implements Histogram{
	private AtomicIntegerArray myList;
	public Histogram4(int span){
		myList = new AtomicIntegerArray(span);
	}
	public void increment(int bin){
		myList.getAndIncrement(bin);
	}
	public int getCount(int bin){
		return myList.get(bin);
	}
	public int getSpan(){
		return myList.length();
	}
	public int[] getBins(){
		int[] arr = new int[getSpan()];
		for(int i=0; i<arr.length; i++){
			arr[i] = getCount(i);
		}
		return arr;
	}
}

class Histogram5 implements Histogram{
	private LongAdder[] myList;

	public Histogram5(int span){

		myList = new LongAdder[span];
		for(int i=0; i< span; i++){
		 myList[i] = new LongAdder();
		}
	}
	public void increment(int bin){
		myList[bin].increment();
	}
	public int getCount(int bin){
		return  myList[bin].intValue();
	}
	public int getSpan(){
		return myList.length;
	}
	public int[] getBins(){
		int[] arr = new int[getSpan()];
		for(int i=0; i<myList.length; i++){
			arr[i] =  myList[i].intValue();
		}
		return arr;
	}
}
