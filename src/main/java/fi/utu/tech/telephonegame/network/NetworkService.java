package fi.utu.tech.telephonegame.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * A concrete implementation of Network interface.
 * Should be able to listen for peers, connect to
 * peer network (by connecting to a peer in existing
 * peer network) as well as send and receive
 * messages from neighbouring peers
 * 
 * You probably need to create more methods and attributes
 * to this class as well as create additional classes
 * to be able to implement all the required functionality
 */
public class NetworkService extends Thread implements Network {

	private ServerSocket serverSocket;
	private List<Socket> clientSockets = new ArrayList<>();
	private ConcurrentLinkedQueue<Object> messageQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Object> sendQueue = new ConcurrentLinkedQueue<>();

	/*
	 * No need to change the construtor
	 */
	public NetworkService() {
		this.start();
	}

	/**
	 * Creates a server instance and starts listening for new peers on specified port
	 * 
	 * The port used for listening for incoming connections is provided automatically
	 * by the Resolver upon calling.
	 * 
	 * @param serverPort Which port should we start to listen to?
	 * 
	 */


	public void startListening(int serverPort) {
		System.out.printf("I should start listening for new peers at TCP port %d%n", serverPort);

		try {
			serverSocket = new ServerSocket(serverPort);
			new Thread (() -> {
				while (!serverSocket.isClosed()) {
					try {
						Socket clientSocket = serverSocket.accept();
						clientSockets.add(clientSocket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will be called when connecting to a peer (other whispers
	 * instance)
	 * The IP address and port will be provided by the template (by the resolver)
	 * upon calling.
	 * 
	 * @param peerIP   The IP address to connect to
	 * @param peerPort The TCP port to connect to
	 */
	public void connect(String peerIP, int peerPort) throws IOException, UnknownHostException {
		System.out.printf("I should connect myself to %s, TCP port %d%n", peerIP, peerPort);

		Socket peerSocket = new Socket(peerIP, peerPort);
		clientSockets.add(peerSocket);
		new Thread(() -> handleIncomingMessages(peerSocket)).start();
	}

	public void handleIncomingMessages(Socket clientSocket) {
		try {
			ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
			while (!clientSocket.isClosed()) {
				Object message = ois.readObject();
				messageQueue.add(message);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to send the message to all connected neighbours (directly connected nodes)
	 * 
	 * @param out The serializable object to be sent to all the connected nodes
	 * 
	 */
	private void sendToNeighbours(Serializable out) {
		// Send the object to all neighbouring nodes
		for (Socket socket : clientSockets) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(out);
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Add an object to the queue for sending it to the peer network (all neighbours)
	 * 
	 * Note: This method is made for others to use. Ie. The implementation
	 * here is called by others (eg. MessageBroker) to post messages INTO
	 * network. To work out the structure of the internal implementation
	 * see run method and sendToNeighbours
	 * 
	 * @param out The Serializable object to be sent
	 */
	public void postMessage(Serializable out) {
		sendQueue.add(out);
	}

	/**
	 * Read the next message from the queue of received messages.
	 * 
	 * Note: This method is made for others to use. Ie. The implementation
	 * here is called by others (eg. MessageBroker) to get access to messages
	 * received from the peer network.
	 * 
	 * @return The next message
	 */
	public Object retrieveMessage() throws InterruptedException {
		return messageQueue.poll();
	}

	/**
	 * Waits for messages from the core application and forwards them to the network
	 * 
	 * Ie. When MessageBroker calls postMessage, the message-to-be-sent should be spooled
	 * into some kind of a producer-consumer-friendly data structure and picked up here for
	 * the actual delivery over sockets.
	 * 
	 * Thread running this method is started in the constructor of NetworkService.
	 * 
	 */
	public void run() {
		while (true) {
			try {
				if (!sendQueue.isEmpty()) {
					Serializable message = (Serializable) sendQueue.poll();
					sendToNeighbours(message);
				}
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
