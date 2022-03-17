package aqua.blatt1.broker;
import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import messaging.*;



public class Broker {
    private Endpoint ep = new Endpoint(Properties.PORT);

    private ClientCollection<InetSocketAddress> clientList = new ClientCollection<>();

    public void broker() {
        while (true) {
            Message m = ep.blockingReceive();
            Serializable paylaod = m.getPayload(); 

            if (paylaod instanceof RegisterRequest) {
                register(m);
            } else if(paylaod instanceof DeregisterRequest) {
                deregister(m);
            } else if(paylaod instanceof HandoffRequest) {
                handofFish(m);
            }
        }
    }

    private int lastId = 0;

    public void register(Message m) {
        String id = "tank " + lastId;
        lastId++;
        clientList.add(id, m.getSender());
        ep.send(m.getSender(), new RegisterResponse(id));
    }

    public void deregister (Message m) {
        String id = ((DeregisterRequest) m.getPayload()).getId();
        this.clientList = clientList.remove(clientList.indexOf(id));
    }

    public void handofFish(Message m) {
        HandoffRequest req = (HandoffRequest) m.getPayload();
        Direction d = req.getFish().getDirection();
        InetSocketAddress next;
        if(d.equals(Direction.LEFT)) {
            next = clientList.getLeftNeighorOf(clientList.indexOf(m.getSender()));
        } else {
            next = clientList.getRightNeighorOf(clientList.indexOf(m.getSender()));
        }
        ep.send(next, new HandoffRequest(req.getFish()));
    }

    public static void main(String [] args) {
        Broker broke = new Broker();
        broke.broker();
    }

}