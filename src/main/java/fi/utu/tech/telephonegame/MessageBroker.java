package fi.utu.tech.telephonegame;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fi.utu.tech.telephonegame.network.Network;
import fi.utu.tech.telephonegame.network.NetworkService;
import fi.utu.tech.telephonegame.network.Resolver;
import fi.utu.tech.telephonegame.network.Resolver.NetworkType;
import fi.utu.tech.telephonegame.network.Resolver.PeerConfiguration;
import fi.utu.tech.telephonegame.util.ConcurrentExpiringHashSet;

/**
 * MessageBroker should work as the "middle-man", the broker
 * that relays messages between different components of the application.
 *
 * MessageBroker should **not** directly handle any network-related code,
 * (nor GUI code). It should be able to access (consume) received messages
 * and send (provide) messages using methods proivided by Network interface.
 * Any socket related code should be in network package, not here.
 *
 * Access and modifications to GUI objects should be made through GuiIO,
 * not directly by modifying gui components
 *
 */
public class MessageBroker extends Thread {

    /*
     * No need to change these variables
     */
    private Network network;
    private Resolver resolver;
    // Abstracted access to graphical interface input and output
    private GuiIO gui_io;
    // Defualt listening port
    private final int rootServerPort = 8050;
    // This might come in handy
    private ConcurrentExpiringHashSet<UUID> prevMessages = new ConcurrentExpiringHashSet<UUID>(1000, 5000);


    /*
     * No need to edit the constructor
     */
    public MessageBroker(GuiIO gui_io) {
        this.gui_io = gui_io;
        network = new NetworkService();
    }

    /**
     * Processes message
     *
     * In the process method you need to:
     * 1. Test the type of the incoming object
     * 2. Keep track of messages that are alredy processed by this node
     * 3. Show the incoming message in the received message text area (GuiIO)
     * 4. Change the text and the color using the Refiner class
     * 5. Set the new color to the color area (see GuiIO)
     * 6. Show the refined message in the refined message text area (GuiIO)
     * 7. Return the processed message
     *
     * @param  The object to be processed
     *
     * @return The message processed in the aforementioned ways
     *
     */
    private synchronized Message process(Object procMessage) {
        if (procMessage instanceof Message message) {

            if (message.getId() != null && !prevMessages.containsKey(message.getId())) {

                // Asetetaan vastaanotettu viesti käyttöliittymään
                gui_io.setReceivedMessage(message.getMessage());

                // Jalostetaan viesti ja väri refinerin avulla
                String refinedText = Refiner.refineText(message.getMessage());
                int refinedColor = Refiner.refineColor(message.getColor());

                // Asetetaan jalostettu viesti ja väri käyttöliittymään
                gui_io.setRefinedMessage(refinedText);
                gui_io.setSignal(refinedColor);

                // palautus
                message.setMessage(refinedText);
                message.setColor(refinedColor);
                prevMessages.put(message.getId());
                return message;
            }
            System.out.println(prevMessages);
        }
        return null;
    }

    /**
     * This run method will be executed in a separate thread automatically by
     * the template (see initialize() method in App.java)
     *
     * In the run method you need to check:
     *
     * 1. If there are any received objects offered by network package
     * 2. When a new object is available it has to be processed using the "process"
     * method
     * 3. Send the processed message back to network (see send method in this class)
     *
     */
    public void run() {
        while (true) {
            try {
                Object received = network.retrieveMessage();

                if (received != null) {
                    Message processedMessage = process(received);

                    if (processedMessage != null) {
                        send(processedMessage);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends message to the peer network using methods provided by network
     * interface
     *
     * You need to make changes here
     * @param message The Message object to be sent
     */
    public synchronized void send(Message message) {
        if (message != null ) {
            network.postMessage(message);
            prevMessages.put(message.getId());
        }
    }

    /**
     * Wraps the String into a new Message object
     * and adds it to the sending queue to be processed by the network component
     * Called when sending a new message
     * @param text The text to be wrapped and sent
     */
    public void send(String text) {
        Message message = new Message(text, 0);
        prevMessages.put(message.getId());
        this.send(message);
    }

    /*
     * Do not edit anything below this point.
     */

    /**
     * Determines which peer to connect to (or if none)
     * Called when user wants to "Discover and connect" or "Start waiting for peers"
     * Calls the appropriate methods in Network object with correct arguments
     *
     * @param netType
     * @param rootNode
     * @param rootIPAddress
     */
    public void connect(NetworkType netType, boolean rootNode, String rootIPAddress) {
        resolver = new Resolver(netType, rootServerPort, rootIPAddress);
        if (rootNode) {
            System.out.println("Root node");
            // Use the default port for the server and start listening for peers
            network.startListening(rootServerPort);
            // As a root node, we are responsible for answering resolving requests - start resolver server
            resolver.startResolverServer();
            // No need to connect to anybody since we are the first node, the "root node"
        } else {
            System.out.println("Leaf node");
            try {
                // Broadcast a resolve request and wait for a resolver server (root node) to send peer configuration
                PeerConfiguration addresses = resolver.resolve();
                // Start listening for new peers on the port sent by the resolver server
                network.startListening(addresses.listeningPort);
                // Connect to a peer using addresses sent by the resolver server
                network.connect(addresses.peerAddr, addresses.peerPort);
            } catch (UnknownHostException | NumberFormatException e) {
                System.err.println("Peer discovery failed (maybe there are no root nodes or broadcast messages are not supported on current network)");
                gui_io.enableConnect();
            } catch (IOException e) {
                System.err.println("Error connecting to the peer");
                gui_io.enableConnect();
            }
        }
    }

}
