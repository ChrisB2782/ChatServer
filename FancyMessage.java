package Lab5Assignment;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Random;
import javax.swing.*;

/**The FancyMessage class of this chat application with an integrated game.
* Displays a fancy animated message with a custom symbol and color changing text.
* @author Christopher Blair
* @version 1.0
*/
public class FancyMessage extends JFrame implements WindowListener {

	JLabel messageLabel;

	/**Constructor for FancyMessage that sets up the JFrame, a GIF, and a color-changing message surrounded by a fancy symbol.
    * @param input The input message to display inside the frame.
    * @throws MalformedURLException If there is an error while loading the image icon.
    */
    public FancyMessage(String input) throws MalformedURLException
    {
        addWindowListener(this);
        setTitle("Fancy Message");
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        Color TCUColors = new Color(77,25,121);
        mainPanel.setBackground(TCUColors);
		ImageIcon originalIcon = new ImageIcon(this.getClass().getResource("MagicMail.gif"));
		originalIcon.setImage(originalIcon.getImage().getScaledInstance(300, 200, Image.SCALE_DEFAULT));
   		JLabel mailLabel = new JLabel(originalIcon);
   		mainPanel.add(getContentPane().add(mailLabel));
		String symbol = fancySymbol();
		String message = symbol + input + symbol;
        messageLabel = new JLabel(message, JLabel.CENTER);
        messageLabel.setFont(new Font("Helvetica", Font.ITALIC + Font.BOLD, 13));
        messageLabel.setForeground(Color.WHITE);
        mainPanel.add(messageLabel);
        add(BorderLayout.CENTER, mainPanel);
        setBounds(550, 250, 300, 300);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        toFront();
		startColorAnimation();
    }

	/**Starts a color-changing animation by creating a Timer to periodically.*/
	private void startColorAnimation() {
		Timer timer = new Timer(1000, e -> rainbowMessage());
        timer.start();
    }

	/**Changes the foreground color of the messageLabel to a random color.*/
	public void rainbowMessage()
	{
		Random random = new Random();
		int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        Color newColor = new Color(red, green, blue);
		messageLabel.setForeground(newColor);
	}

	/**Selects a random fancy symbol from a set of predefined symbols to surround the message.
    * @return A string containing the selected symbol.
    */
    public String fancySymbol() {
        String[] symbols = {"♔", "♕", "♖", "♗", "♘", "♙", "♢", "♦"};
        Random random = new Random();
        return symbols[random.nextInt(symbols.length)];
    }

	/**
    Public Methods
    Used to control the windows
    **/	
	/**Called when the window is closing, disposing the JFrames and exiting the program
	*@param e action that causes the window event
	*/
	public void windowClosing(WindowEvent e) 
	{ 
		dispose(); 
	} 
	public void windowOpened(WindowEvent e) 
	{  

	} 
	public void windowIconified(WindowEvent e) 
	{  

	} 
	public void windowClosed(WindowEvent e) 
	{

	} 
	public void windowDeiconified(WindowEvent e) 
	{  

	} 
	public void windowActivated(WindowEvent e) 
	{  

	} 
	public void windowDeactivated(WindowEvent e) 
	{  

	}
}
