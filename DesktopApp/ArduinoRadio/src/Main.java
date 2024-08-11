import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import com.fazecast.jSerialComm.SerialPort;
import javax.sound.sampled.*;

public class Main implements ActionListener {
    SerialPort sp;

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

    // Audio variables
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private final AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
    private JComboBox<String> inputComboBox;
    private JComboBox<String> outputComboBox;

    public Main() {
        // Set up serial communication
        System.out.println(Arrays.toString(SerialPort.getCommPorts()));

        sp = SerialPort.getCommPort("COM6");

        sp.setComPortParameters(9600,8,1,0);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING|SerialPort.TIMEOUT_READ_BLOCKING,10000,10000);

        if(!sp.openPort()) {
            System.out.println("\nCOM port not available\n");
            return;
        }

        // Initialize the main frame and panel
        frame = new JFrame();
        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setLayout(new GridLayout(1, 2));
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
        gbc.weighty = 0.1; // Set a smaller weight for the upperLeftPanel
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

        label = new JLabel("Frequency: " + frequency + "FM");
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
        gbc.weighty = 0.9; // Larger weight for the lowerLeftPanel to take up more space
        lowerLeftPanel = new JScrollPane();
        lowerLeftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lowerLeftPanel.setBackground(background);
        leftPanel.add(lowerLeftPanel, gbc);

        // Set up the right panel and add it to the main panel
        rightPanel = new JPanel();
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rightPanel.setLayout(new GridLayout(4, 1)); // Adding grid layout to accommodate combo boxes
        rightPanel.setBackground(background);
        mainPanel.add(rightPanel);

        // Create the combo boxes for input and output device selection
        inputComboBox = new JComboBox<>();
        outputComboBox = new JComboBox<>();

        // Populate the combo boxes with available mixers
        populateComboBoxes();

        // Add the combo boxes to the right panel
        JLabel inputDeviceLabel = new JLabel("Select Input Device:");
        inputDeviceLabel.setForeground(Color.WHITE);
        inputDeviceLabel.setFont(new Font(primaryFont, Font.PLAIN, 14));
        rightPanel.add(inputDeviceLabel);
        rightPanel.add(inputComboBox);

        JLabel outputDeviceLabel = new JLabel("Select Output Device:");
        rightPanel.add(outputDeviceLabel);
        outputDeviceLabel.setForeground(Color.WHITE);
        outputDeviceLabel.setFont(new Font(primaryFont, Font.PLAIN, 14));
        rightPanel.add(outputComboBox);

        // Finalize the frame setup
        frame.pack();
        frame.setVisible(true);

        // Set up the audio loopback in a separate thread
        new Thread(this::setupAudioLoopback).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing");
            try {
                if (sp.isOpen()) {
                    // Send a message to the Arduino before closing
                    sp.getOutputStream().write("0\n".getBytes());
                    Thread.sleep(100); // Shorter sleep to avoid long delay
                    sp.closePort(); // Close the serial port gracefully
                }
                // Close the audio lines
                if (microphone != null && microphone.isOpen()) {
                    microphone.stop();
                    microphone.close();
                }
                if (speakers != null && speakers.isOpen()) {
                    speakers.stop();
                    speakers.close();
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }, "Shutdown-thread"));
    }

    private void populateComboBoxes() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
                inputComboBox.addItem(mixerInfo.getName());
            }
            if (mixer.isLineSupported(new DataLine.Info(SourceDataLine.class, format))) {
                outputComboBox.addItem(mixerInfo.getName());
            }
        }

        // Set default selections (system default)
        try {
            inputComboBox.setSelectedItem("Microphone (High Definition Audio Device)");
        } catch (Exception e) {
            inputComboBox.setSelectedItem(AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]).getMixerInfo().getName());
        }
        outputComboBox.setSelectedItem(AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]).getMixerInfo().getName());
    }

    private void setupAudioLoopback() {
        try {
            // Set a threshold for the noise gate
            final int NOISE_THRESHOLD = 20;

            while (true) { // Continuous loop to check for device selection changes
                // Get the selected input and output devices
                String selectedInputDevice = (String) inputComboBox.getSelectedItem();
                String selectedOutputDevice = (String) outputComboBox.getSelectedItem();

                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                Mixer inputMixer = null;
                Mixer outputMixer = null;

                // Find the selected input mixer
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().equals(selectedInputDevice)) {
                        inputMixer = AudioSystem.getMixer(mixerInfo);
                        break;
                    }
                }

                // Find the selected output mixer
                for (Mixer.Info mixerInfo : mixers) {
                    if (mixerInfo.getName().equals(selectedOutputDevice)) {
                        outputMixer = AudioSystem.getMixer(mixerInfo);
                        break;
                    }
                }

                if (inputMixer == null || outputMixer == null) {
                    System.err.println("Unable to find the selected audio devices.");
                    return;
                }

                // Open the input and output lines
                microphone = (TargetDataLine) inputMixer.getLine(new DataLine.Info(TargetDataLine.class, format));
                speakers = (SourceDataLine) outputMixer.getLine(new DataLine.Info(SourceDataLine.class, format));

                microphone.open(format);
                speakers.open(format);
                microphone.start();
                speakers.start();

                byte[] buffer = new byte[4096];
                int bytesRead;

                // Capture and play audio in a loop
                while (true) {
                    // Check if the user changed the input/output device
                    if (!selectedInputDevice.equals(inputComboBox.getSelectedItem()) ||
                            !selectedOutputDevice.equals(outputComboBox.getSelectedItem())) {
                        // If so, break out of the loop to reset the devices
                        microphone.stop();
                        microphone.close();
                        speakers.stop();
                        speakers.close();
                        break;
                    }

                    bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // Apply a simple noise gate
                        boolean isSilent = true;
                        for (int i = 0; i < bytesRead; i++) {
                            if (Math.abs(buffer[i]) > NOISE_THRESHOLD) {
                                isSilent = false;
                                break;
                            }
                        }

                        if (!isSilent) {
                            speakers.write(buffer, 0, bytesRead);
                        } else {
                            // If noise gate is triggered, write silence
                            Arrays.fill(buffer, (byte) 0);
                            speakers.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new Main();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Objects.equals(e.getActionCommand(), "Set frequency")) {
            try {
                frequency = Float.parseFloat(frequencyInput.getText());
                if (frequency != 0.0f) {
                    label.setText("Frequency: " + frequency + "FM");
                } else {
                    label.setText("Muted");
                }
                
                // Send the frequency value to the Arduino
                sp.getOutputStream().write((frequency.toString() + "\n").getBytes());
                frequencyInput.setText("");
            } catch (Exception ex) {
                frequencyInput.setText("Invalid");
            }
        }
    }

    public void scanStations() {

    }
}
