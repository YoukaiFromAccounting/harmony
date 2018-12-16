/*  [Client.java]
    Custom socket retrieval for connection to server and communication
    Author: Brian Zhang
    ICS4UE
    Date: 12/04/18
 */

import java.net.*;
import java.io.*;

public class Client  {

    //Input Output Streams
    private ObjectInputStream listenInput;
    private ObjectOutputStream listenOutput;
    private Socket socket;

    //GUI Connection
    private ClientGUI template;

    //String containers
    private String server, username;
    private int port;

    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    Client(String server, int port, String username, ClientGUI template) {
        this.server = server;
        this.port = port;
        this.username = username;

        this.template = template;
    }

    public boolean start() {
        //Attempt to connect to socket
        try {
            socket = new Socket(server, port);
        }
        catch(Exception ec) {
            display("error connecting to server: " + ec);
            return false;
        }

        String msg = "connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try
        {
            listenInput  = new ObjectInputStream(socket.getInputStream());
            listenOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("exception creating new input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try
        {
            listenOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    private void display(String msg) {
        template.append(msg + "\n");
    }

    //Error here
    public void sendMessage(ChatMessage msg) {
        try {
            listenOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if(listenInput != null) listenInput.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(listenOutput != null) listenOutput.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {} // not much else I can do

        template.connectionFailed();

    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {
        boolean keepGoing = true;
        ChatMessage chatmsg;
        public void run() {
            try {
                while (keepGoing) {
                    try {
                        chatmsg = (ChatMessage) listenInput.readObject();
                    } catch (EOFException ex) {
                        break;
                    } catch (IOException e) {
                        display("Server has closed the connection: ");
                        template.connectionFailed();
                        break;
                    }
                    // can't happen with a String object but need the catch anyhow
                    catch (ClassNotFoundException e2) {
                    }
                    // if console mode print the message and add back the prompt
                    if (chatmsg != null) {
                        if (chatmsg.getType() == ChatMessage.USERNAME) {
                            //if(!msg.getMessage().equals(username)) {
                            template.appendUserList(chatmsg.getMessage());
                            //}
                        } else if (chatmsg.getType() == ChatMessage.MESSAGE) {
                            template.append(chatmsg.getMessage());
                        }
                    }

                }
        }
        finally {
            try {
                listenInput.close();
            }
            catch(IOException ex) {
                {
                    System.err.println("An IOException was caught: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    }
}
