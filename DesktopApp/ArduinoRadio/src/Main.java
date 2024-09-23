import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javax.sound.sampled.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Main implements ActionListener {
    SerialPort sp;

    Properties persistence = new Properties();

    private Color background = new Color(28, 28, 31);
    private final String primaryFont = "Stencil";
    private Float frequency = 89.9f;
    private Float volume = 1.0f;
    private ArrayList<String> stations;
    private String commName = "COM6";
    private boolean muted = false;
    private boolean comboBoxesPopulated = false;

    // Declare the components as private instance variables
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel upperLeftPanel;
    private JPanel labelPanel;
    private JPanel inputPanel;
    private JPanel frequencyButtonPanel;
    private JScrollPane lowerLeftPanel;
    private JPanel lowerLeftPanelHeader;
    private JPanel scrollPanel;
    private JPanel rightPanel;
    private JSlider volumeSlider;
    private JButton frequencySetButton;
    private JButton frequencySaveButton;
    private JButton stationSearchButton;
    private JLabel label;
    private JTextField frequencyInput;

    // Audio variables
    private TargetDataLine microphone;
    private String microphoneName;
    private SourceDataLine speakers;
    private String speakerName;
    private final AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
    private JComboBox<String> portComboBox;
    private JComboBox<String> inputComboBox;
    private JComboBox<String> outputComboBox;

    public Main() {
        // Read persistence file
        File configDir = new File(new File(System.getProperty("user.home")), ".flamefishradio");
        if (!configDir.exists()) {
            if (configDir.mkdir()) {
                System.out.println("Created config directory.");
            } else {
                throw new RuntimeException("Failed to create config directory.");
            }
        }

        String configDirPath = configDir.getAbsolutePath();

        try {
            persistence.load(new FileInputStream(configDirPath + "/config.properties"));
            System.out.println("Config successfully loaded.");
        } catch (IOException e) {
            System.out.println("Config file doesn't exist. Creating.");
            try {
                File configFile = new File(configDirPath + "/config.properties");
                configFile.createNewFile();
//                Files.createFile(Path.of(configDirPath + "/config.properties"));
                System.out.println("Created config file.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        // Get persistence stuff
        frequency = Float.parseFloat(persistence.getProperty("frequency", "89.9"));
        commName = persistence.getProperty("commName", "COM6");
        microphoneName = persistence.getProperty("microphoneName", "Microphone (High Definition Audio Device)");
        speakerName = persistence.getProperty("speakerName", "Primary Sound Driver");
        stations = new ArrayList<String>(Arrays.asList(
                persistence.getProperty("stations", "").split(";")));
        volume = Float.parseFloat(persistence.getProperty("volume","1"));

        System.out.println(speakerName);

        // Set up serial communication
        for (SerialPort port : SerialPort.getCommPorts()) {
            System.out.println(port.getSystemPortName());
        }
        System.out.println(Arrays.toString(SerialPort.getCommPorts()));

        sp = SerialPort.getCommPort(commName);
        System.out.println(sp.getPortDescription());

        sp.setComPortParameters(9600, 8, 1, 0);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 10000, 10000);

        sp.openPort();
//        if (!sp.openPort()) {
//            System.out.println("\nCOM port not available\n");
//            return;
//        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(frequency);
        try {
            sp.getOutputStream().write((frequency.toString() + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("Exception while setting frequency: " + e.getMessage());
        }

        sp.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;

                byte[] newData = new byte[sp.bytesAvailable()];
                int numRead = sp.readBytes(newData, newData.length);

                // Convert the byte array to a String
                String newString = new String(newData, 0, numRead);

                // Print the received string
                System.out.println(newString);

                if (stations.contains(newString)) {
                    return;
                }
                stations.add(newString);
                updateStationsList();
            }
        });


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
        frequencyInput.addActionListener(this);
        inputPanel.add(frequencyInput, BorderLayout.CENTER);

        frequencyButtonPanel = new JPanel();
        frequencyButtonPanel.setLayout(new GridLayout(1, 0));
        inputPanel.add(frequencyButtonPanel, BorderLayout.EAST);

        frequencySetButton = new JButton("Set");
        frequencySetButton.addActionListener(this);
        frequencySetButton.setFont(new Font(primaryFont, Font.BOLD, 14));
        frequencyButtonPanel.add(frequencySetButton);

        frequencySaveButton = new JButton("Save");
        frequencySaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stations.add(frequency.toString());
                updateStationsList();
            }
        });
        frequencySaveButton.setFont(new Font(primaryFont, Font.BOLD, 14));
        frequencyButtonPanel.add(frequencySaveButton);

        scrollPanel = new JPanel();
        scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));


        // Adjust constraints for the lowerLeftPanel to take up more space
        gbc.gridy = 1;
        gbc.weighty = 0.9; // Larger weight for the lowerLeftPanel to take up more space
        lowerLeftPanel = new JScrollPane(scrollPanel);
        lowerLeftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lowerLeftPanel.setBackground(background);
        lowerLeftPanel.getVerticalScrollBar().setUnitIncrement(4);
        leftPanel.add(lowerLeftPanel, gbc);

        stationSearchButton = new JButton("Scan For Stations");
        stationSearchButton.setBackground(Color.DARK_GRAY);
        stationSearchButton.setForeground(Color.WHITE);
        stationSearchButton.addActionListener(this);
        JViewport columnHeaderViewport = new JViewport();
        columnHeaderViewport.setView(stationSearchButton);
        lowerLeftPanel.setColumnHeader(columnHeaderViewport);


        // Set up the right panel and add it to the main panel
        rightPanel = new JPanel();
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rightPanel.setLayout(new GridLayout(6, 1)); // Adding grid layout to accommodate combo boxes
        rightPanel.setBackground(background);
        mainPanel.add(rightPanel);

        // Create the combo boxes for input and output device selection
        portComboBox = new JComboBox<>();
        portComboBox.addActionListener(this);
        inputComboBox = new JComboBox<>();
        inputComboBox.addActionListener(this);
        outputComboBox = new JComboBox<>();
        outputComboBox.addActionListener(this);

        // Populate the combo boxes with available mixers
        populateComboBoxes();

        // Add the combo boxes to the right panel
        JLabel portLabel = new JLabel("Select port:");
        portLabel.setForeground(Color.WHITE);
        portLabel.setFont(new Font(primaryFont, Font.PLAIN, 14));
        rightPanel.add(portLabel);
        rightPanel.add(portComboBox);

        JLabel inputDeviceLabel = new JLabel("Select Input Device:");
        inputDeviceLabel.setForeground(Color.WHITE);
        inputDeviceLabel.setFont(new Font(primaryFont, Font.PLAIN, 14));
        rightPanel.add(inputDeviceLabel);
        rightPanel.add(inputComboBox);

        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new GridBagLayout());
        outputPanel.setBackground(background);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel outputDeviceLabel = new JLabel("Select Output Device:");
        outputPanel.add(outputDeviceLabel,gbc);

        gbc.insets = new Insets(0, 0, 0, 15);
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        volumeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, (int) (volume * 100));
        volumeSlider.createStandardLabels(25,0);
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                volume = volumeSlider.getValue() / 100.0f;
            }
        });
        outputPanel.add(volumeSlider,gbc);

        BufferedImage unmutedImage;
        BufferedImage mutedImage;
        try {
            unmutedImage = ImageIO.read(getClass().getResource("/resources/images/unmuted_icon.png"));
            mutedImage = ImageIO.read(getClass().getResource("/resources/images/muted_icon.png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);

        JButton muteButton = new JButton();


        Icon unmutedIcon = new ImageIcon(unmutedImage);
        Icon mutedIcon = new ImageIcon(mutedImage);

        muteButton.setIcon(unmutedIcon);
        muteButton.setHorizontalAlignment(SwingConstants.RIGHT);
        muteButton.setBorder(BorderFactory.createEmptyBorder());
        muteButton.setContentAreaFilled(false);
        muteButton.setMaximumSize(new Dimension(48,48));
        muteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                muted = !muted;

                muteButton.setIcon((muted) ? mutedIcon : unmutedIcon);
            }
        });

        outputPanel.add(muteButton,gbc);

        rightPanel.add(outputPanel);

        outputDeviceLabel.setForeground(Color.WHITE);
        outputDeviceLabel.setFont(new Font(primaryFont, Font.PLAIN, 14));
        rightPanel.add(outputComboBox);

        // Finalize the frame setup
        frame.pack();
        frame.setVisible(true);

        updateStationsList();

        // Set up the audio loopback in a separate thread
        new Thread(this::setupAudioLoopback).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing");
            try {
                // Close the audio lines
                if (microphone != null && microphone.isOpen()) {
                    microphone.stop();
                    microphone.close();
                }
                if (speakers != null && speakers.isOpen()) {
                    speakers.stop();
                    speakers.close();
                }
                // Write to properties file
                persistence.setProperty("frequency", Float.toString(frequency));
                System.out.println(frequency);
                persistence.setProperty("commName", commName);
                System.out.println(commName);
                persistence.setProperty("microphoneName", microphoneName);
                System.out.println(microphoneName);
                persistence.setProperty("speakerName", speakerName);
                System.out.println(speakerName);
                persistence.setProperty("stations", arrayToString(stations));
                System.out.println(arrayToString(stations));
                persistence.setProperty("volume",volume.toString());

                System.out.println(configDirPath + "/config.properties");
                persistence.store(new FileWriter(configDirPath + "/config.properties"), null);

//                if (sp.isOpen()) {
//                    // Send a message to the Arduino before closing
//                    sp.getOutputStream().write("0\n".getBytes());
//                    Thread.sleep(100); // Shorter sleep to avoid long delay
//                    sp.closePort(); // Close the serial port gracefully
//                }
            } catch (IOException e) {
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

        for (SerialPort port : SerialPort.getCommPorts()) {
            portComboBox.addItem(port.getSystemPortName());
        }

        // Set default selections (system default)
        try {
            inputComboBox.setSelectedItem(microphoneName);
        } catch (Exception e) {
            inputComboBox.setSelectedItem(AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]).getMixerInfo().getName());
        }

        try {
            outputComboBox.setSelectedItem(speakerName);
        } catch (Exception e) {
            outputComboBox.setSelectedItem(AudioSystem.getMixer(AudioSystem.getMixerInfo()[0]).getMixerInfo().getName());
        }

        try {
            portComboBox.setSelectedItem(commName);
        } catch (Exception e) {
            System.out.println("Default port not available.");
        }


        comboBoxesPopulated = true;
    }

    private void setupAudioLoopback() {
        try {
            // Set a threshold for the noise gate
            final int NOISE_THRESHOLD = 20;

            // Low-pass filter parameters
            final float ALPHA = 0.9f; // Smoothing factor

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
                byte[] previousBuffer = new byte[4096]; // Buffer to store the previous output
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
                            if (Math.abs(buffer[i]) > NOISE_THRESHOLD && !muted) {
                                isSilent = false;
                                break;
                            }
                        }

                        if (!isSilent) {
                            // Apply a low-pass filter
                            for (int i = 0; i < bytesRead; i++) {
                                buffer[i] = (byte) ((ALPHA * buffer[i]) + ((1 - ALPHA) * previousBuffer[i]));
                            }

                            // Store the filtered output for the next iteration
                            System.arraycopy(buffer, 0, previousBuffer, 0, bytesRead);

                            // Apply volume and write to speakers
                            for (int i = 0; i < bytesRead; i++) {
                                buffer[i] = (byte) (buffer[i] * volume);
                            }
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
        if (e.getSource() == frequencySetButton || e.getSource() == frequencyInput) {
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
                System.out.println("Exception while setting frequency: " + ex.getMessage());
            }
        } else if (Objects.equals(e.getActionCommand(), "comboBoxChanged")
                && e.getSource() == portComboBox && comboBoxesPopulated) {
            try {
                System.out.println(e.getSource() == portComboBox);
                commName = (String) portComboBox.getSelectedItem();
                sp.closePort();
                SerialPort new_sp = SerialPort.getCommPort(commName);
                assert new_sp != null;
                if (!new_sp.openPort()) {
                    System.out.println("\nCOM port not available\n");
                    return;
                }
                new_sp.setComPortParameters(9600, 8, 1, 0);
                new_sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 10000, 10000);
                sp = new_sp;

                Thread.sleep(3000);
                sp.getOutputStream().write((frequency.toString() + "\n").getBytes());

            } catch (Exception ex) {
                System.out.println("Switching comm port failed: " + ex.getMessage());
            }
            System.out.println(commName);
        } else if (e.getSource() == stationSearchButton) {
            scanStations();
        } else {
            System.out.println(e.getActionCommand());
        }

        System.out.println(comboBoxesPopulated);
        if (comboBoxesPopulated) {
            commName = (String) portComboBox.getSelectedItem();
            speakerName = (String) outputComboBox.getSelectedItem();
            microphoneName = (String) inputComboBox.getSelectedItem();
            System.out.println("Set commName speakerName and microphoneName.");
        }
    }

    public void scanStations() {
        try {
            sp.getOutputStream().write("search\n".getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void updateStationsList() { // Populates the stations scroll panel with the stations list array
        Color background1 = Color.GRAY.darker();
        Color background2 = Color.GRAY;
        Color foreground = Color.WHITE;

        int index = 0;

        scrollPanel.removeAll();
        for (String o : stations) {
            JPanel panel = new JPanel();
            panel.setBackground(Color.GRAY);

            GridBagLayout gbl = new GridBagLayout();

            panel.setLayout(gbl);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;

            gbc.weighty = 1;

            gbc.gridwidth = 0;
            gbc.gridheight= 0;

            gbc.fill = GridBagConstraints.BOTH;

            JButton button = new JButton(o);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            button.setBackground( (index % 2 == 0) ? background1 : background2 );
            button.setForeground(foreground);
            button.setFont(new Font(primaryFont, Font.BOLD,15));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frequencyInput.setText(button.getText());
                    frequencySetButton.doClick();
                }
            });

            JButton removeButton = new JButton("X");
            removeButton.setForeground(Color.RED);
            removeButton.setBackground(background1);
            removeButton.setFont(new Font(primaryFont, Font.BOLD,15));
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String frequency = button.getText();

                    stations.remove(frequency);
                    updateStationsList();
                }
            });
            scrollPanel.add(panel);
//            panel.add(button);
//            panel.add(removeButton);
            gbc.weightx = 0.9;
            addobjects(button,panel,gbl,gbc,0,0,2,1);
            gbc.weightx = 0.1;
            addobjects(removeButton,panel,gbl,gbc,2,0,1,1);
            index++;
        }
        scrollPanel.revalidate();
        scrollPanel.repaint();
    }

    public String arrayToString(ArrayList<String> array) {
        StringBuilder output = new StringBuilder();
        for (String o : array) {
            output.append(o).append(";");
        }
        return output.toString();
    }

    public void addobjects(Component component, Container yourcontainer, GridBagLayout layout, GridBagConstraints gbc, int gridx, int gridy, int gridwidth, int gridheight){

        gbc.gridx = gridx;
        gbc.gridy = gridy;

        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;

        layout.setConstraints(component, gbc);
        yourcontainer.add(component);
    }
}