package fi.utu.tech.telephonegame.network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private ConcurrentLinkedQueue<Object> messageQueue;

    public PeerHandler(Socket socket, ConcurrentLinkedQueue<Object> messageQueue){
        this.socket = socket;
        this.messageQueue = messageQueue;

        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void send(Serializable obj){
        try {
            oos.writeObject(obj);
            oos.flush();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
    @Override
    public void run() {
        try {

            while (!socket.isClosed()) {
                Object message = (Serializable) ois.readObject();
                messageQueue.add(message);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
