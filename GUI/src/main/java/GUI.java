import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI {
    public static void main(String[] args) {
        // Create a JFrame (the main window)
        JFrame frame = new JFrame("Player and Team Input");
        frame.setSize(600, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Create a label to display message for entering player name
        JLabel playerLabel = new JLabel("Enter a Player Name:");
        playerLabel.setBounds(150, 20, 300, 20);
        frame.add(playerLabel);

        // Create a text field for player name input
        JTextField playerField = new JTextField();
        playerField.setBounds(150, 50, 300, 30);
        frame.add(playerField);

        // Create a label to display message for entering team name
        JLabel teamLabel = new JLabel("Enter a Team Name:");
        teamLabel.setBounds(150, 100, 300, 20);
        frame.add(teamLabel);

        // Create a text field for team name input
        JTextField teamField = new JTextField();
        teamField.setBounds(150, 130, 300, 30);
        frame.add(teamField);

        // Create a button for submission
        JButton button = new JButton("Submit");
        button.setBounds(250, 180, 100, 30);
        frame.add(button);

        // Create a label to display the result
        JLabel resultLabel = new JLabel();
        resultLabel.setBounds(150, 230, 500, 30);
        frame.add(resultLabel);

        // Add an ActionListener to the button to handle button clicks
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String playerName = playerField.getText();
                String teamName = teamField.getText();
                
                resultLabel.setText("Player: " + playerName + ", Team: " + teamName);
            }
        });

        frame.setVisible(true);
    }
}
