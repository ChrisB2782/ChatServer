package Lab5Assignment;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**The ChatServer class of this chat application with an integrated game.
* Creates the GUI for the server, handles incoming, outgoing messages, and the game.
* @author Christopher Blair
* @version 1.0
*/
public class ChatServer extends Frame
{   
    public final boolean verbose = true;
    boolean reset = false;
    Vector<PrintWriter> clientOutputStreams;
    Vector<String> clientNames;
    HashMap<String, PrintWriter> clientMap = new HashMap<>();
    List<String> players = new ArrayList<>();
    public  TextArea  result = new TextArea(40,40);
    Label timeLabel = new Label("Date and Time ", Label.RIGHT);
    TextField timeField = new TextField("");
    Panel  displayTime = new Panel(new GridLayout(1,2));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH : mm : ss");
    int year,month,day,hour,min,sec;
    String todayS, timeS, minS, secS;
    Calendar now;
    RouletteGame game = new RouletteGame();
    
    /**Static method to load the class as object in memory.
    * @param args String[] with the console arguments.
    * @throws InterruptedException in case of interruptions during thread sleep.
    */ 
    public static void main(String args[]) throws InterruptedException { 
		System.out.println("Chat Service");
	    ChatServer server = new ChatServer();
	    server.go();
	}

    
        /**Constructor that sets up the server frame and initializes the displayTime and timeField.*/
        public ChatServer()
        { 
                setLayout(new BorderLayout());
                setSize(600, 800);
                result.setBackground(Color.BLACK);
                result.setForeground(Color.WHITE);
                result.setEditable(false);
                timeField.setEditable(false);
                displayTime.add(timeLabel);displayTime.add(timeField);
                add(displayTime,BorderLayout.NORTH);
                add(result,BorderLayout.CENTER);
                setBackground(Color.orange);
                setVisible(true);
	    }
    
    /**Processes and retrieves the current time or date as a string, formatted based on the given option.
    *@param option Determines the formatting of the time
    *@return A string representing the processed time or date
    */
    public String processTime(int option)
	   {    now = Calendar.getInstance();
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
           } 
           return null;  // should not get here
        }
    
    /**Initializes the server socket and handles incoming client connections.
    * It also manages the game state, starting a new game when certain amount of clients are registered.
    * @throws InterruptedException in case of interruptions during thread execution.
    * Catches InterruptedException for e and Exception for ex
    */
    public void go() throws InterruptedException {
        clientOutputStreams = new Vector<PrintWriter>();
        clientNames = new Vector<String>();
        try {
            ServerSocket serverSock = new ServerSocket(5001);
             if (verbose) System.out.println("Server IP 127.0.0.1. ready at port 5000 " );
                if (verbose) System.out.println(" ___________________"
                    +"________________________________________________"  ); 
            result.append("Server started on " + processTime(2)+"\n");
            if (verbose) { System.out.println ("\nStart Server on " + processTime(2)); 
             }   
            Thread timeThread = new Thread(() -> {
                while (true) {
                    LocalTime currentTime = LocalTime.now();
                    String formattedTime = currentTime.format(formatter);
                    timeField.setText(processTime(0) + " @ " + formattedTime);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            timeThread.start();
            while(true) {
                Socket clientSocket = serverSock.accept();
                BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientName = clientReader.readLine();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                clientMap.put(clientName, writer);
                clientNames.add(clientName);
                players.add(clientName);
                clientOutputStreams.add(writer);
                if (players.size() >= 2 && players.size() <= 4) {
                    reset = true;
                    game.gameInfo("RESET");
                    game.startGame();
                }
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    
    /**Broadcasts a message to all registered clients.
    * @param input The message to be sent to all clients.
    * @throws IOException if there is an issue sending the message.
    * Catches Exception for ex
    */
    public void tellEveryone(String input) throws IOException {
        String message = processedMessage(input);
        Iterator<PrintWriter> it = clientOutputStreams.iterator();
        result.append(message + " broadcasted on " + processTime(2)+ "\n");
        while (it.hasNext()) {
            try {
                PrintWriter writer = (PrintWriter) it.next();
                System.out.println("message  resent to all => " + message);
                writer.println(message.substring(0, message.indexOf(":") + 2) + message.substring(message.indexOf(">") + 2));
                writer.flush();
            } catch (Exception ex) { result.append("Error sending message to client.\n"); ex.printStackTrace(); }
        }
    }

    /**Broadcasts a message to the specific client.
    * @param sender The client sending the message.
    * @param recipient The client to recieve the message.
    * @param input The message to be sent to the client.
    * @throws IOException if there is an issue sending the message.
    * Catches Exception for ex
    */
    public void tellClient(String sender, String recipient, String input) throws IOException {
        String message = processedMessage(input);
        PrintWriter senderWriter = clientMap.get(sender);
        PrintWriter clientWriter = clientMap.get(recipient);
        result.append(message + " broadcasted on " + processTime(2)+ "\n"); 
        if (senderWriter != null && clientWriter != null) {
            try {
                System.out.println("message  sent to " + recipient + "=> " + message);
                senderWriter.println(message.substring(0, message.indexOf(":") + 2) + message.substring(message.indexOf(">") + 2));
                senderWriter.flush();
                clientWriter.println(message.substring(0, message.indexOf(":") + 2) + message.substring(message.indexOf(">") + 2));
                clientWriter.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        } else {
            result.append("Recipient " + recipient + " not found.\n");
        }
    }

    /**Processes the message to format and handle encryption or other modifications.
    * @param input The raw input message to be processed.
    * @return The processed message.
    * @throws IOException if there is an issue processing the message.
    */
    public String processedMessage(String input) throws IOException {
        String name = input.substring(0, input.indexOf(":") + 2); 
        String recipient = input.substring(input.indexOf("<"), input.indexOf(">") + 2);
        String message = input.substring(input.indexOf(">") + 2);
        if (message.endsWith("encrypt"))
        {
            message = message.replaceAll("encrypt", "");
            message = encryptMessage(message);
        }
        if (message.startsWith("fancy"))
        {
            message = message.replaceFirst("fancy", "");
            FancyMessage fancyMessage = new FancyMessage(message);
        }
        return name + recipient + message;
    }

    /**Encrypts the contents of a String.
    * @param inFile the message
    * @throws IOException if there is an issue with the encryption process.
    */
    public String encryptMessage(String message) throws IOException
    {
        Random random = new Random();
        int key = random.nextInt(10) + 1;   
        String encryptedMessage = "";
        for (char c : message.toCharArray()) {
            encryptedMessage += ((char) (c + key));
        }
        return encryptedMessage;
    }

    /** Internal class to handle the Client Handler
    The benefit of using an internal class is that we have access to the various objects of the 
    external class
    */
    public class ClientHandler  implements Runnable { 
        private Socket sock;
        private BufferedReader reader;

        /**Constructor of the inner class
        * @param clientSocket
        * Catches Exception for ex
        */
         public ClientHandler(Socket clientSocket) {
              try {
                  sock = clientSocket;
                  reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                  if (verbose) System.out.println("new client " );
                  if (verbose) System.out.println(" new client at address "
                    +  sock.getLocalAddress() + " at port " +  sock.getLocalPort()); 
                  if (verbose) System.out.println(" Client at address " 
                      +  sock.getInetAddress() + " at port " +  sock.getPort()  );
                  if (verbose) System.out.println(" ___________________"
                      +"________________________________________________"  ); 
  
                  
              } catch (Exception ex) { ex.printStackTrace(); }
          }

          /**Method RUN to be executed when thread.start() is executed
          * Catches Exception for ex and IOException for e
          */
          public void run() {
              String message;
              try {
                  while ((message = reader.readLine()) != null) {
                    if (message.equals("RESET")) {
                        reset = true;
                        game.gameInfo("RESET");
                        game.startGame();
                    }
                    else if (message.contains(message.substring(0, message.indexOf(":") + 1) + "SHOOT") || message.contains(message.substring(0, message.indexOf(":") + 1) + "HEAL") || message.contains(message.substring(0, message.indexOf(":") + 1) + "SAW") || message.contains(message.substring(0, message.indexOf(":") + 1) + "RACK")) 
                    {
                        game.gameState(message);
                    }
                    else
                    {
                      result.append("message received " + message + " on " + processTime(2)+"\n");
                      String sender = message.substring(0, message.indexOf(":"));
                      String recipient = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
                      if (recipient.isEmpty() || recipient.equalsIgnoreCase("all")) 
                      {
                        tellEveryone(message);
                      } 
                      else 
                      {
                        tellClient(sender, recipient, message);
                      }
                    }

                  }
              } catch (Exception ex) { ex.printStackTrace(); }
              finally {
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
          }
      }

      /**Runs a multiplayer game where players take turns firing a shotgun.
      * Contains random live or blank shells. Players can use various items to heal or alter the state of the shotgun.
      */
      public class RouletteGame {
        private List<Boolean> shotgun = new ArrayList<>();
        private int currentPlayer = 0;
        private Hashtable<String, Integer> playerLives = new Hashtable();
        private List<String> eliminatedPlayers = new ArrayList<>();
        private boolean gameInProgress = false;
        private int shotgunDamage = 1;
        private boolean sawUsed = false;
        private final int MAX_LIVES = 3;
        private final int MAX_SHELLS = 8;

        /** Starts the game, initializes player lives, shotgun shells, and game state.
        * Provides game rules to the players and starts the first turn.
        * @throws IOException If there is an issue sending information to clients.
        */
        public void startGame() throws IOException {
            gameInProgress = true;
            if (reset) {
                shotgun.clear();
                currentPlayer = 0;
                playerLives.clear();
                sawUsed = false;
                for (String player : eliminatedPlayers) {
                    players.add(player);
                    playerLives.put(player, MAX_LIVES);
                }
                eliminatedPlayers.clear();
                reset = false;
            }
            String rules = """
            Game Start: 
            Here are the rules!
            1. The shotgun contains a random number of shells, each being either live or blank.
            2. A live shell inflicts damage (1 life) when shot.
            3. A blank shell does no harm and grants another turn if shot at yourself.
            4. Players have 3 lives each. When you lose all lives, you're eliminated.
            5. Items available (one use each per round):
                - Heal: Restores 1 life.
                - Rack: Removes the current shell from the shotgun.
                - Hand Saw: Doubles the shotgun's damage.
            6. If all shells are expended and players remain, the shotgun is reloaded, items reset, and the game continues.""";
            for (String player : players) {
                playerLives.put(player, MAX_LIVES);
            }
            gameInfo(rules);
            reloadShotgun();
            gameInfo("TURN: " + players.get(currentPlayer) + " (type a user's name to shoot at them)");
        }

        /**Broadcasts game information to all registered clients.
        * @param input The info to be sent to all clients.
        * @throws IOException if there is an issue sending the message.
        * Catches Exception for ex
        */
        private void gameInfo(String info) throws IOException {
            Iterator<PrintWriter> it = clientOutputStreams.iterator();
            while (it.hasNext()) {
                try {
                    PrintWriter writer = (PrintWriter) it.next();
                    writer.println(info);
                    writer.flush();
                } catch (Exception ex) { result.append("Error sending message to client.\n"); ex.printStackTrace(); }
        }
        }

        /**Handles player actions during their turn, such as shooting, healing, racking, and sawing.
        * @param action The action taken by the player, containing the action type and the target.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void gameState(String action) throws IOException {
            String currentPlayerName = players.get(currentPlayer);
            if (action.contains("SHOOT")) {
                handleShot(action);
            } else if (action.contains("HEAL")) {
                handleHeal(action);
            } else if (action.contains("SAW")) {
                handleSaw(action);
            } else if (action.contains("RACK")) {
                handleRack(action);
            } else {
                gameInfo("Invalid Action.");
            }
        }

        /**Processes the shooting action, handling whether the shell is live or blank.
        * @param input The input string containing the shooter and target information.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void handleShot(String input) throws IOException {
            String shooter = input.substring(0, input.indexOf(":"));
            String target = input.substring(input.indexOf(":") + 6);
            if (!gameInProgress) return;

            if (!players.contains(target)) {
                gameInfo("Invalid Target.");
                return;
            }

            if (shotgun.isEmpty()) {
                reloadShotgun();
            }

            boolean isLiveShell = shotgun.remove(0);
            if (shooter.equals(target)) {
                if (isLiveShell) {
                    playerLives.put(shooter, playerLives.get(shooter) - shotgunDamage);
                    if (sawUsed) {
                        shotgunDamage /= 2;
                        sawUsed = false;
                        gameInfo(shooter + " SHOT themselves with a SAWED shell! Shotgun damage is reset to normal.");
                    }
                    else
                    {
                        gameInfo(shooter + " SHOT themselves with a live shell!");
                    }
                    nextPlayer();
                } else {
                    gameInfo(shooter + " SHOT themselves with a blank shell and gets another turn!");
                }
            } else {
                if (isLiveShell) {
                    playerLives.put(target, playerLives.get(target) - shotgunDamage);
                    if (sawUsed) {
                        shotgunDamage /= 2;
                        sawUsed = false;
                        gameInfo(shooter + " SHOT " + target + " with a SAWED shell! Shotgun damage is reset to normal.");
                    }
                    else {
                        gameInfo(shooter + " SHOT " + target + " with a live shell!");
                    }
                } else {
                    gameInfo(shooter + " SHOT " + target + " with a blank shell! No damage.");
                }
                if (shotgun.isEmpty()) reloadShotgun();
                nextPlayer();
            }
            for (String player : players) {
                if (playerLives.get(player) <= 0)
                {
                    eliminatedPlayers.add(player);
                }
            }
            players.removeIf(player -> playerLives.get(player) <= 0);
            if (players.size() == 1) {
                gameInfo(players.get(0) + " wins the game!");
                gameInProgress = false;
            }
        }

        /**Processes the healing action, restoring 1 life to the specified player.
        * @param input The input string containing the player who is healing.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void handleHeal (String input) throws IOException {
            if (!gameInProgress) return;
            if (input.contains(":HEAL")) {
                String player = input.substring(0, input.indexOf(":"));
                if (playerLives.containsKey(player)) {
                    int currentLives = playerLives.get(player);
                    playerLives.put(player, currentLives + 1);
                    gameInfo(player + " used HEAL and gained 1 life.");
                }
            }
        }

        /**Processes the saw action, doubling the shotgun's damage until it's fired.
        * @param input The input string containing the player using the saw.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void handleSaw (String input) throws IOException {
            if (!gameInProgress) return;
            if (input.contains(":SAW")) {
                String player = input.substring(0, input.indexOf(":"));
                if (!sawUsed) {
                    shotgunDamage *= 2;
                    sawUsed = true;
                    gameInfo(player + " used SAW! Shotgun damage is now doubled.");
                }
                else {
                    gameInfo(player + " tried to use SAW, but shotgun damage is already doubled.");
                }
            }
        }

        /**Processes the rack action, removing the current shell from the shotgun.
        * @param input The input string containing the player using the rack.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void handleRack (String input) throws IOException {
            if (!gameInProgress) return;
            if (input.contains(":RACK")) {
                String player = input.substring(0, input.indexOf(":"));
                if (!shotgun.isEmpty()) {
                    shotgun.remove(0);
                    gameInfo(player + " used RACK to remove the current shell.");
                }
            }
        }

        /**Moves the turn to the next player, broadcasting the current turn to the players.
        * @throws IOException if there is an issue sending information to clients.
        */
        private void nextPlayer() throws IOException {
            currentPlayer = (currentPlayer + 1) % players.size();
            gameInfo("TURN: " + players.get(currentPlayer) + " (type a user's name to shoot at them)");
        }

        /**Reloads the shotgun with a random number of shells, broadcasting the new shell count to the players.
        * @throws IOException If there is an issue sending information to clients.
        */
        private void reloadShotgun() throws IOException {
            if (!gameInProgress) return;
            int liveShells = 0;
            int blankShells = 0;
            shotgun.clear();
            Random random = new Random();
            for (int i = 0; i < random.nextInt(MAX_SHELLS - 4 + 1) + 4; i++) {
                boolean shellType = random.nextBoolean();
                shotgun.add(shellType);
                if (shellType) {
                    liveShells++;
                }
                else {
                    blankShells++;
                }
            }
            gameInfo("Shotgun loaded with " + liveShells + " live shells & " + blankShells + " blank shells.");
        }
}
}