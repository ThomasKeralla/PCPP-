// For week 7
// sestoft@itu.dk * 2014-10-12

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;

public class TestButtonGui {
    public static void showThreads(String prefix) {
              for( Thread T : Thread.getAllStackTraces().keySet() ){
          System.out.println(prefix+T.getName());
      }
    }
    public static void main(String[] args) {
        showThreads("A ");
        final Random random = new Random();
    final JFrame frame = new JFrame("TestButtonGui");
        showThreads("Frame ");
    final JPanel panel = new JPanel();
        showThreads("Panel ");
    final JButton button = new JButton("Press here");
    frame.add(panel);
    panel.add(button);
    showThreads("Added ");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          panel.setBackground(new Color(random.nextInt()));
        }});
    showThreads("Listener ");
    frame.pack();
    showThreads("Pack ");
    frame.setVisible(true);
    showThreads("Visi ");

  }
}

