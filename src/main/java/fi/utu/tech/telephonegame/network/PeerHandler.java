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
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            while (!socket.isClosed()) {
                Object message = ois.readObject();
                messageQueue.add(message);
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
