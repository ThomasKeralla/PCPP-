// For week 7
// sestoft@itu.dk * 2014-10-18

// From JSR 305 jar file jsr305-3.0.0.jar:
// import javax.annotation.concurrent.GuardedBy;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


public class TestLiftGui {
  public static void main(String[] args) {
    // The lift model and associated graphics
    final LiftShaft shaft1 = new LiftShaft(),
      shaft2 = new LiftShaft(), shaft3 = new LiftShaft(), shaft4 = new LiftShaft();
    final Lift lift1 = new Lift("Lift1", shaft1),
      lift2 = new Lift("Lift2", shaft2), lift3 = new Lift("Lift3", shaft3), lift4 = new Lift("Lift4", shaft4);
    final LiftDisplay lift1Display = new LiftDisplay(lift1, true),
      lift2Display = new LiftDisplay(lift2, false), lift3Display = new LiftDisplay(lift3, true),
      lift4Display = new LiftDisplay(lift4, false);
    LiftController controller = new LiftController(lift1, lift2, lift3, lift4);
    //Thread t1 = new Thread(lift1), t2 = new Thread(lift2), t3 = new Thread(lift3), t4 = new Thread(lift4) ;
    //t1.start(); t2.start();t3.start(); t4.start();
    ScheduledThreadPoolExecutor scheduledPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4);

    ScheduledFuture<?> periodicLift1 = scheduledPool.scheduleAtFixedRate(lift1, 62500L,62500L , TimeUnit.MICROSECONDS);
    ScheduledFuture<?> periodicLift2 = scheduledPool.scheduleAtFixedRate(lift2, 62500L,62500L , TimeUnit.MICROSECONDS);
    ScheduledFuture<?> periodicLift3 = scheduledPool.scheduleAtFixedRate(lift3, 62500L,62500L , TimeUnit.MICROSECONDS);
    ScheduledFuture<?> periodicLift4 = scheduledPool.scheduleAtFixedRate(lift4, 62500L,62500L , TimeUnit.MICROSECONDS);
    //scheduledPool.scheduleAtFixedRate(lift1, 62500L,62500L , TimeUnit.MICROSECONDS);
    //scheduledPool.scheduleAtFixedRate(lift2, 62500L,62500L , TimeUnit.MICROSECONDS);
    //scheduledPool.scheduleAtFixedRate(lift3, 62500L,62500L , TimeUnit.MICROSECONDS);
    //scheduledPool.scheduleAtFixedRate(lift4, 62500L,62500L , TimeUnit.MICROSECONDS);


    // The graphical presentation
    final JFrame frame = new JFrame("TestLiftGui");
    final JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    frame.add(mainPanel);
    final JPanel panel = new JPanel();
    mainPanel.add(panel, BorderLayout.WEST);
    panel.setLayout(new BorderLayout());
    panel.add(lift1Display, BorderLayout.WEST);
    mainPanel.add(new OutsideLiftButtons(controller), BorderLayout.CENTER);
    panel.add(lift2Display, BorderLayout.EAST);
    final JPanel panel2 = new JPanel();
    mainPanel.add(panel2, BorderLayout.EAST);
    panel2.setLayout(new BorderLayout());
    panel2.add(lift3Display, BorderLayout.WEST);
    //panel2.add(new OutsideLiftButtons(controller), BorderLayout.CENTER);
    panel2.add(lift4Display, BorderLayout.EAST);
    //panel.add(lift3Display, BorderLayout.WEST);
    //panel.add(lift4Display, BorderLayout.EAST);
    frame.pack(); frame.setVisible(true);

    try {
              scheduledPool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
              e.printStackTrace();
        }

        scheduledPool.shutdown();

  }
}

class LiftDisplay extends JPanel {
  public LiftDisplay(Lift lift, boolean buttonsLeft) {
    setLayout(new BorderLayout());
    JPanel buttons = new InsideLiftButtons(lift);
    add(buttons, buttonsLeft ? BorderLayout.WEST : BorderLayout.EAST);
    add(lift.shaft, buttonsLeft ? BorderLayout.EAST : BorderLayout.WEST);
  }
}

class LiftShaft extends Canvas {
  public final int lowFloor = -2, highFloor = 10;
  private double atFloor = 0.0,         // in [lowFloor, highFloor]
    doorOpen = 0.0;                     // in [0, 1]

  public LiftShaft() {
    setPreferredSize(new Dimension(50, 280));
  }

  public void moveTo(double atFloor, double doorOpen) {
    this.atFloor = atFloor;
    this.doorOpen = doorOpen;
    repaint();
  }

  public void paint(Graphics g) {
    super.paint(g);
    final int floorCount = highFloor - lowFloor + 1;
    final int h = getHeight(), w = getWidth(), perFloor = (h-1) / floorCount,
      yOffset = highFloor * perFloor;
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, w-1, floorCount * perFloor);
    // Draw lift cage and doors in red
    g.setColor(Color.RED);
    final int liftTop = yOffset + 2 - (int)(atFloor * perFloor + 0.5);
    g.drawRect(2, liftTop, w-5, perFloor-4);
    final int doorWidth = (int)(0.5 + (w-7)/2.0 * (1.0 - doorOpen));
    g.drawRect(3, liftTop+1, doorWidth, perFloor-6); // left door
    g.drawRect(w-4-doorWidth, liftTop+1, doorWidth, perFloor-6); // right door
    // Draw floor lines in grey at bottom of floors
    g.setColor(Color.GRAY);
    for (int floor=highFloor-1; lowFloor<=floor; floor--) {
      final int y = yOffset - floor * perFloor;
      g.drawLine(1, y, w-2, y);
    }
  }
}

class InsideLiftButtons extends JPanel {
  public InsideLiftButtons(final Lift lift) {
    try {
      javax.swing.UIManager.setLookAndFeel( javax.swing.UIManager.getCrossPlatformLookAndFeelClassName() );
    } catch (Exception e) {
      e.printStackTrace();
    }
    setLayout(new GridBagLayout()); // To center button panel
    JPanel panel = new JPanel();
    panel.setBackground(Color.LIGHT_GRAY);
    add(panel);
    final int floorCount = lift.highFloor - lift.lowFloor + 1;
    panel.setLayout(new GridLayout(floorCount, 1));
    for (int floor=lift.highFloor; lift.lowFloor<=floor; floor--) {
      final int myFloor = floor;
      JButton button = new JButton(Integer.toString(floor));
      panel.add(button);
      button.addActionListener(new ActionListener() {

          public void actionPerformed(ActionEvent e) {
            lift.goTo(myFloor);
            Component[] components = panel.getComponents();
            for(int i = 0; i <components.length; i++)
            components[i].setBackground(Color.GRAY);
            int low = -lift.lowFloor;
            JButton active = (JButton) components[(components.length-1)-(myFloor+low)];
            active.setBackground(Color.RED);

            }});
    }
  }
}

class OutsideLiftButtons extends JPanel {
  public OutsideLiftButtons(LiftController controller) {
    final int floorCount = controller.highFloor - controller.lowFloor + 1;
    setLayout(new GridLayout(floorCount, 1));
    for (int floor=controller.highFloor; controller.lowFloor<=floor; floor--)
      add(new UpDownButtons(floor, controller));
  }
}

class UpDownButtons extends JPanel {
  public UpDownButtons(final int atFloor, final LiftController controller) {
    JPanel panel = new JPanel();
    panel.setBackground(Color.LIGHT_GRAY);
    add(panel);
    panel.setLayout(new GridLayout(2, 1));
    JButton up = new JButton("u"), down = new JButton("d");
    panel.add(up);
    panel.add(down);
    up.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          controller.someLiftTo(atFloor, Direction.Up);
        }});
    down.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          controller.someLiftTo(atFloor, Direction.Down);
        }});
    up.setVisible(atFloor < controller.highFloor);
    down.setVisible(atFloor > controller.lowFloor);
  }
}

// The lift model --------------------------------------------------

class LiftController {
  private final Lift[] lifts;
  public final int lowFloor, highFloor;

  public LiftController(Lift... lifts) {
    this.lifts = lifts;
    int lowFloor = Integer.MAX_VALUE, highFloor = Integer.MIN_VALUE;
    for (int i=0; i<lifts.length; i++) {
      lowFloor = Math.min(lowFloor, lifts[i].lowFloor);
      highFloor = Math.max(highFloor, lifts[i].highFloor);
    }
    this.lowFloor = lowFloor;
    this.highFloor = highFloor;
  }

  public void someLiftTo(int floor, Direction dir) {
    double bestTime = Double.POSITIVE_INFINITY;
    int bestLift = -1;
    for (int i=0; i<lifts.length; i++) {
      double thisLiftTime = lifts[i].timeToServe(floor, dir);
      if (thisLiftTime < bestTime) {
        bestTime = thisLiftTime;
        bestLift = i;
      }
    }
    lifts[bestLift].customerAt(floor, dir);
  }
}

class Lift implements Runnable {
  public final int lowFloor, highFloor;
  public final String name;
  public final LiftShaft shaft;
  // These two fields are accessed only on the Lift's own thread:
  private double floor;         // In interval [lowFloor...highFloor]
  private Direction direction;  // None or Up or Down
  // @GuardedBy("this")
  private final Direction[] stops;
  private Double doorOpen = 0.0;
  private Double doorClose = 1.0;
  private Boolean doorsStatus = false;

  public Lift(String name, LiftShaft shaft) {
    this.lowFloor = shaft.lowFloor;
    this.highFloor = shaft.highFloor;
    this.name = name;
    this.shaft = shaft;
    this.floor = 0.0;
    this.direction = Direction.None;
    this.stops = new Direction[highFloor-lowFloor+1];
  }

  // All these private methods are used in the lift's
  // up-sweep/down-sweep operation:

  private synchronized Direction getStop(int floor) {
    return stops[floor-lowFloor];
  }

  private synchronized void setStop(int floor, Direction dir) {
    stops[floor-lowFloor] = dir;
  }

  private synchronized void subtractFromStop(int floor, Direction dir) {
    switch (dir) {
    case Down:
      if (floor == lowestStop())
        setStop(floor, null);
      else
        setStop(floor, dir.subtractFrom(getStop(floor)));
      break;
    case Up:
      if (floor == highestStop())
        setStop(floor, null);
      else
        setStop(floor, dir.subtractFrom(getStop(floor)));
      break;
    default:
      throw new RuntimeException("impossible Lift.subtractFromStop");
    }
  }

  private synchronized int highestStop() {
    for (int floor=highFloor; lowFloor<=floor; floor--)
      if (getStop(floor) != null)
        return floor;
    return Integer.MIN_VALUE;
  }

  private synchronized int lowestStop() {
    for (int floor=lowFloor; floor<=highFloor; floor++)
      if (getStop(floor) != null)
        return floor;
    return Integer.MAX_VALUE;
  }

  private synchronized int stopsBetween(double from, double to, Direction dir) { // from <= to
    final int fromFloor = (int)(Math.floor(from)), toFloor = (int)(Math.ceil(to));
    int count = 0;
    for (int floor=fromFloor; floor<=toFloor; floor++)
      if (dir.stopFor(getStop(floor)))
        return count++;
    return count;
  }

  // Estimate the lift's time to serve toFloor in the desired
  // direction; called by lift controller on the event thread
  public synchronized double timeToServe(int toFloor, Direction thenDir) {
    switch (direction) {
    case Down:
      final int lowestStop = lowestStop();
      if (floor > toFloor+0.5 && (thenDir != Direction.Up || toFloor <= lowestStop))
        // lift is above requested floor and request is for going
        // down, or serving the requested floor is a continuation of
        // this down sweep; serve during this down sweep
        return (floor - toFloor) + 2 * stopsBetween(toFloor, floor, direction);
      else
        // lift is not above the requested floor or the request is
        // for going up; serve in an up sweep after completing this
        // down sweep
        return (floor - lowestStop) + 2 * stopsBetween(lowestStop, floor, Direction.Down)
          + (toFloor - lowestStop) + 2 * stopsBetween(lowestStop, toFloor, Direction.Up);
    case Up:
      final int highestStop = highestStop();
      if (floor < toFloor-0.5 && (thenDir != Direction.Down || toFloor >= highestStop))
        return (toFloor - floor) + 2 * stopsBetween(floor, toFloor, Direction.Up);
      else
        return (highestStop - floor) + 2 * stopsBetween(floor, highestStop, Direction.Up)
            + (highestStop - toFloor) + 2 * stopsBetween(toFloor, highestStop, Direction.Down);
      case None:
        return Math.abs(floor - toFloor);
    default:
      throw new RuntimeException("impossible timeToServe");
    }
  }

  // External request from lift controller (on event thread):
  public synchronized void customerAt(int floor, Direction thenDir /* not null */) {
    setStop(floor, thenDir.add(getStop(floor)));
  }

  // Internal request from the lift's own buttons (on event thread):
  public synchronized void goTo(int floor) {
    setStop(floor, Direction.None.add(getStop(floor)));
  }

  public void run() {
    final double steps = 16.0;
    //while (true) {
      //try { Thread.sleep((int)(1000.0/steps)); }
      //catch (InterruptedException exn) { }

      ;
      switch (direction) {
      case Up:
        if(doorsStatus) {
        openAndCloseDoors();
        break;
        }
        if ((int)floor == floor) { // At a floor, maybe stop here
          Direction afterStop = getStop((int)floor);
          if (afterStop != null && (afterStop != Direction.Down || (int)floor == highestStop())) {
            doorsStatus = true;
            openAndCloseDoors();
            break;
            //subtractFromStop((int)floor, direction);
          }
        }
        if (floor < highestStop()) {
          floor += direction.delta / steps;
          shaft.moveTo(floor, 0.0);
        } else
          direction = Direction.None;
        break;
      case Down:
        if(doorsStatus) {
          openAndCloseDoors();
          break;
        }
        if ((int)floor == floor) { // At a floor, maybe stop here
          Direction afterStop = getStop((int)floor);
          if (afterStop != null && (afterStop != Direction.Up || (int)floor == lowestStop())) {
            doorsStatus = true;
            openAndCloseDoors();
            break;
            //subtractFromStop((int)floor, direction);
          }
        }
        if (floor > lowestStop()) {
          floor += direction.delta / steps;
          shaft.moveTo(floor, 0.0);
        } else
          direction = Direction.None;
        break;
      case None:
        final int lowestStop = lowestStop(), highestStop = highestStop();
        if (floor >= lowestStop)
          direction = Direction.Down;
        else if (floor <= highestStop)
          direction = Direction.Up;
        break;
      default: throw new RuntimeException("impossible Lift.move");
      }
    //}
  }

  private void openAndCloseDoors() {
    final double steps = 16.0;
    //try {
      //for (double doorOpen=0.0; doorOpen <= 1; doorOpen += 1.0/steps) {
        //Thread.sleep((int)(1000.0/steps));
        //shaft.moveTo(floor, doorOpen);
        if(doorOpen <= 1.0) {
          shaft.moveTo(floor, doorOpen);
          doorOpen += 1.0/steps;
        }
        else {
        shaft.moveTo(floor, doorClose);
        doorClose -= 1.0/steps;
        if(doorClose == 0.0) {
          doorClose = 1.0;
          doorOpen = 0.0;
          doorsStatus = false;
          subtractFromStop((int)floor, direction);
        }

        //}

      //}
      //for (double doorOpen=1.0; doorOpen >= 0; doorOpen -= 1.0/steps) {

        //Thread.sleep((int)(1000.0/steps));

        //shaft.moveTo(floor, doorOpen);
      //}
    } //catch (InterruptedException exn) { }
  }
}

enum Direction {
  Down(-1), None(0), Up(+1), Both(2);
  public final int delta;
  private Direction(int delta) {
    this.delta = delta;
  }

  public boolean stopFor(Direction dirAfter) { // may be null
    return dirAfter != null && (dirAfter == Both || dirAfter == this);
  }

  public Direction add(Direction dir) {
    switch (this) {
    case Up:
      return (dir==Down || dir==Both) ? Both : this;
    case Down:
      return (dir==Up   || dir==Both) ? Both : this;
    case None:
      return dir != null ? dir : this;
    case Both:
      return this;
    default: throw new RuntimeException("impossible Direction.add");
    }
  }

  public Direction subtractFrom(Direction dirAfter) {  // may be null
    switch (this) {
    case Up:
      return (dirAfter==Down || dirAfter==Both) ? Down : null;
    case Down:
      return (dirAfter==Up   || dirAfter==Both) ? Up : null;
    default: throw new RuntimeException("impossible Direction.subtractFrom");
    }
  }
}

class MoveTo {
  public final int floor;

  public MoveTo(int floor) {
    this.floor = floor;
  }
}
