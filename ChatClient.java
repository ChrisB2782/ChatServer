package Lab5Assignment; 

import Lab5Assignment.ChatClient.SoundUtil;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

/**The ChatClient class of this chat application with an integrated game.
* Creates the GUI for the client and game, sends incoming messages and recieves outgoing messages, and sends over game actions.
* @author Christopher Blair
* @version 1.0
*/
public class ChatClient extends JFrame {
    JTextArea incoming;
    JTextField nameField;
    JTextField to;
    JTextField outgoing;
    JTextField targetField;
    JTextField health;
    public JPanel gamePanel;
    JButton shootButton;
    JButton healButton;
    JButton sawButton;
    JButton rackButton;
    JButton resetButton;
    boolean usedHeal = false;
    boolean usedSaw = false;
    boolean usedRack = false;
    BufferedReader reader;
    PrintWriter writer;
    Socket sock;
    public JCheckBox checkEncrypt = new JCheckBox("Encrypt");
    public JCheckBox checkFancy = new JCheckBox("Fancy");
    public Color TCUColors   = new Color(77,25,121);
    public Color InverseTCU = new Color(25, 121, 77);
    public Color ReverseTCU = new Color(121, 25, 77);
    Calendar now; String todayS, timeS, minS, secS; int year,month,day,hour,min,sec;
    Hashtable<Integer, Color> colorTable = new Hashtable();
    public int clientNum = 0;

    /**Static method to load the class as object in memory.
    * @param args String[] with the console arguments.
    * @throws IOException if an error occurs during initialization.
    */   
    public static void main(String args[]) throws IOException
    {
		// Construct the frame
        new ChatClient();
	}

    /**Constructor that sets up the buttons, panels, scrollPanes, textAreas, and title of the frame.
    * @throws IOException if an error occurs during initialization.
    * Catches Exception for ex
    */
    public ChatClient() throws IOException
    {
        JFrame frame = new JFrame("Chat Client");
        int sec = Integer.parseInt(processTime(3));
        frame.setBounds(200+5*sec,8*sec,600,350);
        JPanel mainPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        gamePanel = new JPanel(new GridLayout(1, 7, 5, 5));
        if(sec > 50 || sec > 30) 
        {
            mainPanel.setBackground(TCUColors); checkEncrypt.setBackground(TCUColors); checkFancy.setBackground(TCUColors); gamePanel.setBackground(ReverseTCU);
        }
        else if(sec > 40 || sec > 20) 
        {
            mainPanel.setBackground(InverseTCU); checkEncrypt.setBackground(InverseTCU); checkFancy.setBackground(InverseTCU); gamePanel.setBackground(TCUColors);
        }
        else
        {
            mainPanel.setBackground(ReverseTCU); checkEncrypt.setBackground(ReverseTCU); checkFancy.setBackground(ReverseTCU); gamePanel.setBackground(InverseTCU);
        }
        incoming = new JTextArea(10, 50);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);
        incoming.setFont(new Font("Helvetica", Font.PLAIN, 13));
        JScrollPane qScroller = new JScrollPane(incoming);
        qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        outgoing = new JTextField(20);
        nameField = new JTextField(20);
        to = new JTextField(20);
        health = new JTextField(10);
        health.setForeground(Color.RED);
        health.setText("LIVES: 3");
        health.setEditable(false);
        targetField = new JTextField(10);
        JLabel nameLabel = new JLabel("Name", JLabel.RIGHT);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Helvetica", Font.BOLD, 13));
        JLabel toLabel = new JLabel("To", JLabel.RIGHT);
        toLabel.setForeground(Color.WHITE);
        toLabel.setFont(new Font("Helvetica", Font.BOLD, 13));
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new SendButtonListener());
        sendButton.setEnabled(false);
        shootButton = new JButton("SHOOT");
        shootButton.addActionListener(new GameButtonsListener());
        shootButton.setEnabled(true);
        healButton = new JButton("HEAL");
        healButton.addActionListener(new GameButtonsListener());
        healButton.setEnabled(true);
        sawButton = new JButton("SAW");
        sawButton.addActionListener(new GameButtonsListener());
        sawButton.setEnabled(true);
        rackButton = new JButton("RACK");
        rackButton.addActionListener(new GameButtonsListener());
        rackButton.setEnabled(true);
        resetButton = new JButton("RESET");
        resetButton.addActionListener(new GameButtonsListener());
        resetButton.setEnabled(false);
        checkEncrypt.setFont(new Font("Helvetica", Font.BOLD, 13));
        checkEncrypt.setForeground(Color.WHITE);
        checkFancy.setFont(new Font("Helvetica", Font.BOLD, 13));
        checkFancy.setForeground(Color.WHITE);
        mainPanel.add(nameLabel);
        mainPanel.add(nameField);
        mainPanel.add(toLabel);
        mainPanel.add(to);
        mainPanel.add(checkEncrypt);
        mainPanel.add(checkFancy);
        mainPanel.add(outgoing);
        mainPanel.add(sendButton);
        gamePanel.add(health);
        gamePanel.add(targetField);
        gamePanel.add(shootButton);
        gamePanel.add(healButton);
        gamePanel.add(rackButton);
        gamePanel.add(sawButton);
        gamePanel.add(resetButton);
        frame.add(BorderLayout.NORTH, mainPanel);
        frame.add(BorderLayout.CENTER, qScroller);
        frame.add(BorderLayout.SOUTH, gamePanel);
        gamePanel.setVisible(false);
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        Color clientColor = new Color(red, green, blue);
        colorTable.put(clientNum, clientColor);
        incoming.setForeground(colorTable.get(clientNum));
        nameField.addActionListener(new ActionListener() { @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String clientName = nameField.getText().isEmpty() ? "ANONYMOUS " + random.nextInt(100) : nameField.getText();
                    writer.println(clientName);
                    writer.flush();
                    nameField.setText(clientName);
                    nameField.setEditable(false);
                    sendButton.setEnabled(true);
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        clientNum++;
        setUpNetworking();
        Thread readerThread = new Thread(new IncomingReader());
        readerThread.start();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.validate();   
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
    }

    /*Establishes a connection to the server and initializes input/output streams.
    * Catches IOException for ex 
    */
    private void setUpNetworking() {
        try {
            sock = new Socket("127.0.0.1", 5001);
            InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(streamReader);
            writer = new PrintWriter(sock.getOutputStream());
            System.out.println("Networking Protocol Established");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    /**Listens for and handles the sendButton click event
    * Sends a message to ChatServer, optionally applying encryption or fancy formatting.
    * Catches Exception for ex
    */
    public class SendButtonListener implements ActionListener {
        /**Method Action Performed to listen to an event and sends a String associated with messages to the ChatServer.
        @param e action that causes the event
        */
        public void actionPerformed(ActionEvent e)
        {
            try {
                String name = nameField.getText();
                String recipient = to.getText().isEmpty() ? "ALL" : to.getText();
                String message = outgoing.getText();
                if (checkEncrypt.isSelected()) {
                    message += "encrypt";
                    SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\Normal.wav");
                }
                else if (checkFancy.isSelected()) {
                    message = "fancy" + message;
                    SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\Fancy.wav");
                }
                else 
                {
                    SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\Normal.wav");
                }
                message = name + ": <" + recipient + "> " + message;
                to.setText(recipient);
                writer.println(message);
                writer.flush();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            outgoing.setText("");
            outgoing.requestFocus();
        }
    }

    /**Listens for and handles the gamePanel button clicks (shoot, heal, saw, rack, reset).*/
    public class GameButtonsListener implements ActionListener { @Override
        /**Method Action Performed to listen to an event and sends a String associated with a game action to ChatServer.
        * @param e action that causes the event.
        * Catches Exception for ex
        */
        public void actionPerformed(ActionEvent e) {
        JButton source = (JButton) e.getSource();
        String action = source.getText();
        try {
            switch (action) {
                case "SHOOT":
                if (targetField.getText().equals("")) {
                    incoming.append("Please specify a target to shoot.\n");
                    break;
                }
                    writer.println(nameField.getText() + ":SHOOT" + targetField.getText());
                    writer.flush();
                    targetField.setText("");
                    targetField.requestFocus();
                    break;
                case "HEAL":
                    writer.println(nameField.getText() + ":HEAL");
                    writer.flush();
                    healButton.setEnabled(false);
                    usedHeal = true;
                    break;
                case "SAW":
                    writer.println(nameField.getText() + ":SAW");
                    writer.flush();
                    sawButton.setEnabled(false);
                    usedSaw = true;
                    break;
                case "RACK":
                    writer.println(nameField.getText() + ":RACK");
                    writer.flush();
                    rackButton.setEnabled(false);
                    usedRack = true;
                    break;
                case "RESET":
                    writer.println("RESET");
                    writer.flush();
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    }

    /**Internal class to handle the IncomingReader Handler
    The benefit of using an internal class is that we have access to the various objects of the 
    external class
    */
    class IncomingReader implements Runnable {
    	/**Method RUN to be executed when thread.start() is executed.
        * Catches IOException for ex
        */
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    incoming.setCaretPosition(incoming.getDocument().getLength());
                    if (message.equals("RESET")) {
                        incoming.setText("");
                        health.setText("LIVES: 3");
                        usedHeal = false;
                        usedRack = false;
                        usedSaw = false;
                        shootButton.setEnabled(true);
                        healButton.setEnabled(true);
                        sawButton.setEnabled(true);
                        rackButton.setEnabled(true);
                        resetButton.setEnabled(false);
                        continue;
                    }
                    if (message.startsWith("Game Start:")) {
                        gamePanel.setVisible(true);
                    }
                    if (message.startsWith("TURN:")) {
                        String currentTurnPlayer = message.substring(6, message.indexOf("(")).trim();
                        boolean isTurn = currentTurnPlayer.equals(nameField.getText().trim());
                        updateTurnState(isTurn);
                    }
                    if (message.startsWith("Shotgun loaded with ")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunReload.wav");
                    }
                    if (message.equals(nameField.getText() + " SHOT themselves with a blank shell and gets another turn!") || message.contains(" SHOT " + nameField.getText() + " with a blank shell!")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunBlank.wav");
                    }
                    if (message.equals(nameField.getText() + " SHOT themselves with a live shell!") || message.equals(nameField.getText() + " SHOT themselves with a SAWED shell! Shotgun damage is reset to normal.")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunBlast.wav");
                        if (message.contains("with a SAWED shell!")) {
                            int newHealth = Integer.parseInt(health.getText().substring(7));
                            newHealth = Math.max(0, newHealth - 2);
                            health.setText("LIVES: " + newHealth);
                        }
                        else {
                            int newHealth = Integer.parseInt(health.getText().substring(7));
                            newHealth = Math.max(0, newHealth - 1);
                            health.setText("LIVES: " + String.valueOf(newHealth));
                        }
                    }
                    else if (message.contains(" SHOT " + nameField.getText() + " with a live shell!") || message.contains(" SHOT " + nameField.getText() + " with a SAWED shell!")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunBlast.wav");
                        String target = message.substring(message.indexOf("SHOT ") + 5, message.indexOf(" with"));
                        if (target.equals(nameField.getText())) {
                            if (message.contains("with a SAWED shell!")) {
                                int newHealth = Integer.parseInt(health.getText().substring(7));
                                newHealth = Math.max(0, newHealth - 2);
                                health.setText("LIVES: " + newHealth);
                            }
                            else {
                                int newHealth = Integer.parseInt(health.getText().substring(7));
                                newHealth = Math.max(0, newHealth - 1);
                                health.setText("LIVES: " + newHealth);
                            }
                        }
                    }
                    if (message.equals(nameField.getText() + " used HEAL and gained 1 life.")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\Healing.wav");
                        int newHealth = Integer.parseInt(health.getText().substring(7)) + 1;
                        health.setText("LIVES: " + String.valueOf(newHealth));
                    }
                    if (message.equals(nameField.getText() + " used SAW! Shotgun damage is now doubled.")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunSaw.wav");
                    }
                    if (message.equals(nameField.getText() + " used RACK to remove the current shell.")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\ShotgunRack.wav");
                    }
                    if (message.equals(nameField.getText() + " tried to use SAW, but shotgun damage is already doubled.")) {
                        SoundUtil.playSound("C:\\Users\\cblai\\Downloads\\VS\\Lab5Assignment\\Lab5Assignment\\Sounds\\Failure.wav");
                        sawButton.setEnabled(true);
                        usedSaw = false;
                    }
                    if (message.contains(" wins the game!")) {
                        shootButton.setEnabled(false);
                        healButton.setEnabled(false);
                        rackButton.setEnabled(false);
                        sawButton.setEnabled(false);
                        resetButton.setEnabled(true);
                    }
                else {
                	incoming.append(message + "\n");
                }
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /**Updates the enabled state of gamePanel based on whether it's the player's turn. 
    *@param isTurn True if it is the player's turn; false otherwise.
    */
    public void updateTurnState(boolean isTurn) {
        shootButton.setEnabled(isTurn);
        if (!usedHeal) healButton.setEnabled(isTurn);
        if (!usedSaw) sawButton.setEnabled(isTurn);
        if (!usedRack) rackButton.setEnabled(isTurn);
        targetField.setEditable(isTurn);
    }

    /**Utility class for playing sound effects during chat and game interactions.*/
    public class SoundUtil {
    /**Plays a sound effect from the specified file path.
    * @param soundFilePath The file path of the sound effect.
    * Catches UnsupportedAudioFileException, IOException, and LineUnavailableException for e
    */
    public static void playSound(String soundFilePath) {
        try {
            File soundFile = new File(soundFilePath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
    /**Processes and retrieves the current time or date as a string, formatted based on the given option.
    *@param option Determines the formatting of the time
    *@return A string representing the processed time or date
    */
    public String processTime(int option)
	{    
        now = Calendar.getInstance();
	    year = now.get(Calendar.YEAR); 
        month = now.get(Calendar.MONTH)+1; 
        day = now.get(Calendar.DAY_OF_MONTH);
        hour = now.get(Calendar.HOUR);
        min =  now.get(Calendar.MINUTE);	  
        sec =  now.get(Calendar.SECOND);
        if (min < 10 )  minS =  "0" + min ;  else  minS = "" + min;
        if (sec < 10 )  secS =  "0" + sec ;  else  secS = "" + sec;
        todayS =  month + " / " + day + " / " + year;  
        timeS  = hour + " : " + minS + " : " + secS; 
        switch(option) {
        case (0):  return todayS  ; 
        case (1):  return timeS;
        case (2):  return todayS + " @ " + timeS ; 
        case (3): return secS;
        } 
        return null;  // should not get here
     }
}
