import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.server.ExportException;
import java.util.Objects;

public class Main implements ActionListener {
    private int count = 0;
    private Color background = new Color(28, 28, 31);
    private final String primaryFont = "Stencil";
    private Float frequency = 89.9f;

    // Declare the components as private instance variables
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel upperLeftPanel;
    private JPanel labelPanel;
    private JPanel inputPanel;
    private JScrollPane lowerLeftPanel;
    private JPanel rightPanel;
    private JButton frequencySetButton;
    private JLabel label;
    private JTextField frequencyInput;

    public Main() {
        // Initialize the main frame and panel
        frame = new JFrame();
        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setLayout(new GridLayout(1, 0));
        mainPanel.setBackground(background);

        // Add the main panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("FlameFish Arduino Radio");
        frame.setBackground(background);
        frame.setMinimumSize(new Dimension(300, 300));
        frame.setPreferredSize(new Dimension(750, 500));

        // Set up the left panel with GridBagLayout
        leftPanel = new JPanel();
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setBackground(background);
        mainPanel.add(leftPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.2; // Set a smaller weight for the upperLeftPanel
        gbc.fill = GridBagConstraints.BOTH;

        // Set up the upper left panel with smaller height
        upperLeftPanel = new JPanel();
        upperLeftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        upperLeftPanel.setLayout(new GridLayout(2, 1)); // 2 rows, 1 column
        upperLeftPanel.setBackground(background);
        leftPanel.add(upperLeftPanel, gbc);

        // Set up the label panel and add it to the upper left panel
        labelPanel = new JPanel();
        labelPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        labelPanel.setLayout(new BorderLayout());
        labelPanel.setBackground(background);
        upperLeftPanel.add(labelPanel);

        label = new JLabel("Frequency: "+frequency+"FM");
        label.setForeground(Color.white);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font(primaryFont, Font.PLAIN, 14));
        labelPanel.add(label, BorderLayout.CENTER);

        // Set up the input panel with text field and button
        inputPanel = new JPanel();
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setBackground(background);
        upperLeftPanel.add(inputPanel);

        frequencyInput = new JTextField();
        frequencyInput.setFont(new Font(primaryFont, Font.PLAIN, 14));
        frequencyInput.setHorizontalAlignment(SwingConstants.CENTER);
        inputPanel.add(frequencyInput, BorderLayout.CENTER);

        frequencySetButton = new JButton("Set frequency");
        frequencySetButton.addActionListener(this);
        frequencySetButton.setFont(new Font(primaryFont, Font.BOLD, 14));
        inputPanel.add(frequencySetButton, BorderLayout.EAST);

        // Adjust constraints for the lowerLeftPanel to take up more space
        gbc.gridy = 1;
        gbc.weighty = 0.8; // Larger weight for the lowerLeftPanel to take up more space
        lowerLeftPanel = new JScrollPane();
        lowerLeftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lowerLeftPanel.setBackground(background);
        leftPanel.add(lowerLeftPanel, gbc);

        // Set up the right panel and add it to the main panel
        rightPanel = new JPanel();
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rightPanel.setLayout(new GridLayout(1, 1));
        rightPanel.setBackground(background);
        mainPanel.add(rightPanel);

        // Finalize the frame setup
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new Main();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle button click events
        if (Objects.equals(e.getActionCommand(), "Set frequency")) {
            try {
                frequency = Float.parseFloat(frequencyInput.getText().replace("FM",""));
                if (frequency == 0) {
                    label.setText("Muted");
                } else {
                    label.setText("Frequency: " + frequency + "FM");
                }
                frequencyInput.setText("");
            }
            catch (Exception exception) {
                frequencyInput.setText("Invalid");
            }
        }
    }
}
