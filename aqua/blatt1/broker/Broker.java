package aqua.blatt1.broker;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.*;
import javax.swing.*;


public class Broker {
    private Endpoint ep = new Endpoint(Properties.PORT);

    private ClientCollection<InetSocketAddress> clientList = new ClientCollection<>();

    // List with fixed amount of Threads, which can take Tasks and schedules them to the threads
    private ExecutorService es = Executors.newFixedThreadPool(10);
    // read write lock for reading the clientList in the threads (Problem: with register / deregister)
    ReadWriteLock lock = new ReentrantReadWriteLock() ;
    // Flag for setting the stopRequest in the while loop (Problem: Broker can be stuck in blockingReceive)
    // Das Sichtbarmachen vom Anderungen mit ¨ volatile verhindert keine
    // Inkonsistenzen durch konkurrierenden Zugriff. Es stellt nur sicher, dass
    // schreibender Zugriff sofort in allen Threads sichtbar wird
    private static volatile boolean stopRequest = false;

    public void broker() {
        // Stopping Thread for setting the stopRequest
        es.execute(new stopRequest());

        while (!stopRequest) {
            Message m = ep.blockingReceive();

            //poison pill message
            if (m.getPayload() instanceof PoisonPill) {
                System.out.println("*Urgh* ive been poisoned by " + m.getSender() + "! This little Biiitt.... *dying sound*");
                break;
            }
            es.execute(new BrokerTask(m));
        }
        es.shutdownNow();
        System.out.println("I'm done!");
    }

    private int lastId = 0;

    public void register(Message m) {
        lock.writeLock().lock();
        String id = "tank " + lastId;
        InetSocketAddress sender = m.getSender();
        lastId++;
        clientList.add(id, sender);

        int index = clientList.indexOf(id);

        // Get the left and right neighbor
        InetSocketAddress left = clientList.getLeftNeighorOf(index);
        InetSocketAddress right = clientList.getRightNeighorOf(index);

        lock.writeLock().unlock();

        ep.send(m.getSender(), new RegisterResponse(id));

        // Hand out the Token to the first client
        if (lastId == 1) {System.out.println("Sending Token ...");ep.send(sender, new Token());};

        // Tell new client his new neighbors
        ep.send(sender, new NeighborUpdate(left, Direction.LEFT));
        ep.send(sender, new NeighborUpdate(right, Direction.RIGHT));

        // Tell neighbors that they have a new left or right neighbor
        ep.send(left, new NeighborUpdate(sender, Direction.RIGHT));
        ep.send(right, new NeighborUpdate(sender, Direction.LEFT));
    }

    public void deregister (Message m) {
        String id = ((DeregisterRequest) m.getPayload()).getId();

        lock.writeLock().lock();
        int index = clientList.indexOf(id);

        // Get the left and right neighbor
        InetSocketAddress left = clientList.getLeftNeighorOf(index);
        InetSocketAddress right = clientList.getRightNeighorOf(index);

        // Tell neighbors that they are each others new neighbor now
        ep.send(left, new NeighborUpdate(right, Direction.RIGHT));
        ep.send(right, new NeighborUpdate(left, Direction.LEFT));

        this.clientList = clientList.remove(clientList.indexOf(id));
        lock.writeLock().unlock();
    }

    public void handofFish(Message m) {
        HandoffRequest req = (HandoffRequest) m.getPayload();
        Direction d = req.getFish().getDirection();
        InetSocketAddress next;

        lock.readLock().lock();

        if (d.equals(Direction.LEFT)) {
            next = clientList.getLeftNeighorOf(clientList.indexOf(m.getSender()));
        } else {
            next = clientList.getRightNeighorOf(clientList.indexOf(m.getSender()));
        }

        lock.readLock().unlock();

        ep.send(next, new HandoffRequest(req.getFish()));
    }

    public void resolveName(Message m) {
        InetSocketAddress sender = m.getSender();
        NameResolutionRequest req = (NameResolutionRequest) m.getPayload();
        InetSocketAddress tankAdress = clientList.getClient(clientList.indexOf(req.getTankID()));
        ep.send(sender, new NameResolutionResponse(req.getRequestID(), tankAdress));
    }

    /*
    class for creating tasks that handle the messages received by the broker
     */
    private class BrokerTask implements Runnable {

        private Message m;

        public BrokerTask(Message msg) {
            this.m = msg;
        }

        @Override
        public void run() {
            processMsg();
        }

        public void processMsg() {
            Serializable payload = m.getPayload();

            if (payload instanceof RegisterRequest) {
                register(m);
            } else if (payload instanceof DeregisterRequest) {
                deregister(m);
            } else if (payload instanceof HandoffRequest) {
                handofFish(m);
            } else if (payload instanceof NameResolutionRequest) {
                resolveName(m);
            }
        }
    }

    /*
    class for creating a task that only displays a messageDialog which sets the stopRequest flag
     */
    private static class stopRequest implements Runnable {

        @Override
        public void run() {
            JOptionPane.showMessageDialog(null,"Du wollen beenden Broker ? Du drücken \"OK\"", "Set Stop Flag", JOptionPane.QUESTION_MESSAGE);
            stopRequest = true;
            System.out.println("Flag set to quit after next message received");
        }
    }

    public static void main(String [] args) {
        Broker broke = new Broker();
        broke.broker();
        System.exit(1);
    }

}