import java.util.Random;
import java.io.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Arrays;
import java.util.UUID;

class MergeSort {

public static void main(String[] args) {

Scanner s = new Scanner(System.in);
int[] list;

System.out.print("Enter no. of elements you want in array, or type 100 for default Array:");
int first =s.nextInt();
//use default values given in the assignment
if(first == 100) {
  int[] list2 = {6,5,3,1,8,7,2,4};
  list = list2;
}
//use values from terminal, first value defines the size of the array to be sorted
else {
list = new int[first];

System.out.println("Enter all the elements:");

for(int i = 0; i < list.length; i++) {
    list[i] = s.nextInt();
  }
}
System.out.println("Merging: "+Arrays.toString(list));
//get/create the system from Singleton
ActorSystem system = ActorSystemSingleton.getSystem();
  final ActorRef starter = system.actorOf(Props.create(Starter.class), "Starter");
  starter.tell(new Data("start",list, null), ActorRef.noSender());
  }
}

class Starter extends UntypedActor {
  Data data;

public void onReceive(Object o) {
  data = (Data) o;

  if(data.getInstruction().equals("start")) {
    ActorSystem system = ActorSystemSingleton.getSystem();

    final ActorRef tester = system.actorOf(Props.create(Tester.class), "Tester");
    final ActorRef sorter = system.actorOf(Props.create(Sorter.class), "Sorter");
    //tell the tester to start
    tester.tell(new Data("start", data.getList(), sorter), ActorRef.noSender());
  } else
      System.out.println("Unknown message at "+getSelf());
  }
}


class Tester extends UntypedActor {
  ActorRef sorter;
  Data data;

  public void onReceive(Object o) {
    this.data = (Data) o;
    this.sorter = data.getActor();
    //activates the sorter, that will then spawn other sorteres and mergers to solve the sorting
    if(data.getInstruction().equals("start"))
      data.getActor().tell(new Data("sort",data.getList(), getSelf()), ActorRef.noSender());
      //prints result
    else if(data.getInstruction().equals("merge")) {
      System.out.println(""+getSelf()+" printing result: "+Arrays.toString(data.getList()));
      ActorSystemSingleton.getSystem().shutdown();
    }
    else
      System.out.println("Unknown message at: "+getSelf());
  }
}

class Sorter extends UntypedActor {

  Data data;
  ActorRef destination;

  public void onReceive(Object o) {
    this.data = (Data) o;
    if(data.getInstruction().equals("sort")) {
      destination = data.getActor();

      if(data.getList().length > 1) {
      int n = data.getList().length;
      //divide recieved Array into two Arrays, if odd number of elements add one extra to Array 2
      int[] left = new int[n/2];
      int[] right = new int[(n/2)+n%2];

      System.arraycopy(data.getList(), 0, left, 0, left.length);
      System.arraycopy(data.getList(), left.length, right, 0, right.length);

      ActorSystem system = ActorSystemSingleton.getSystem();
      //used random UUID to ensure uniqueness so the system dont crash
      final ActorRef sorter1 = system.actorOf(Props.create(Sorter.class), "SorterLeft"+left.length+","+UUID.randomUUID());
      final ActorRef sorter2 = system.actorOf(Props.create(Sorter.class), "SorterRight"+right.length+","+UUID.randomUUID());
      final ActorRef merger = system.actorOf(Props.create(Merger.class), "Merger"+","+UUID.randomUUID());
      //tell the merger where to sent merged Array
      merger.tell(new Data("destination", null, destination), ActorRef.noSender());
      //what would be considered the recursive call. Keep doing this until the basecase is hit
      sorter1.tell(new Data("sort",left,merger), ActorRef.noSender());
      sorter2.tell(new Data("sort",right,merger), ActorRef.noSender());
      } else {
        //send back the one element (basecase)
        System.out.println("Basecase: "+Arrays.toString(data.getList()));
        data.getActor().tell(new Data("merge", data.getList(), data.getActor()), ActorRef.noSender());
      }
  } else
     System.out.println("Unknown message at "+getSelf());
  }
}

class Merger extends UntypedActor {

  ActorRef returnAddress;
  int[] left = null;
  int[] right = null;
  Data data;

  public void onReceive(Object o) {
    data = (Data) o;
    if(data.getInstruction().equals("destination")) {
      //set the address of next merger in the tree
      returnAddress = data.getActor();
    }
    else if (data.getInstruction().equals("merge")) {
      if(left==null)
      //if no other list have been recieved, set left to recieved list and wait for next list
        left = data.getList();
      else if (right == null) {
        //merge left and right
        right = data.getList();
        int[] mergeList = new int[left.length+right.length];
        int index = 0;
        int indexRight = 0;
        int indexLeft = 0;

        while(indexLeft < left.length && indexRight < right.length) {
          if(left[indexLeft]< right[indexRight])
            mergeList[index++] = left[indexLeft++];
          else
            mergeList[index++] = right[indexRight++];
        }
        //add last elements if any
        if(indexLeft < left.length)
          for(int i = indexLeft; i<left.length;i++)
            mergeList[index++] = left[i];
        else
          for(int i = indexRight; i<right.length;i++)
            mergeList[index++]=right[i];

        System.out.println("Merged: "+Arrays.toString(mergeList));
        //send merged list to next merger
        returnAddress.tell(new Data("merge",mergeList, getSelf()), ActorRef.noSender());
      } else
          System.out.println("Recieved more then 2 lists for merge at: "+getSelf());
    } else
      System.out.println("Unknown message at: "+getSelf());
  }
}


//carries the three pieces of information needed and methods to get them
class Data implements Serializable {
  private final ActorRef actor;
  private final String instruction;
  private final int[] list;

  public Data(String instruction, int[] list, ActorRef destination) {
    this.instruction = instruction;
    this.actor = destination;
    this.list = list;
  }

  public String getInstruction(){
    return instruction;
  }
  public int[] getList() {
    return list;
  }
  public ActorRef getActor() {
    return actor;
  }
}

//Singleton for holding ActorSystem
class ActorSystemSingleton {
    private static ActorSystem sys = null;

    private ActorSystemSingleton () {
    }

    public static synchronized ActorSystem getSystem() {
        if (sys == null) {
            sys = ActorSystem.create("MergeSort");
        }
        return sys;
    }
    public void shutdown() {
      sys.shutdown();
    }
}
