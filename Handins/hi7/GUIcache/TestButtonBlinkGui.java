// For week 7
// sestoft@itu.dk * 2014-10-12, 2016-10-14

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;

public class TestButtonBlinkGui {
  private static volatile boolean showBar = false;

  public static void main(String[] args) {
    final Random random = new Random();
    final JFrame frame = new JFrame("TestButtonBlinkGui");
    final JPanel panel = new JPanel() {
        public void paint(Graphics g) {
          super.paint(g);
          if (showBar) {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 10, getHeight());
          }
        }
      };
    final JButton button = new JButton("Press here");
    frame.add(panel);
    panel.add(button);
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          panel.setBackground(new Color(random.nextInt()));
        }});
    frame.pack(); frame.setVisible(true);
    while (true) {
      try { Thread.sleep(800); } // milliseconds
      catch (InterruptedException exn) { }
      showBar = !showBar;
      panel.repaint();
    }
  }
}

