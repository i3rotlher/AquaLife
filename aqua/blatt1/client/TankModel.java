package aqua.blatt1.client;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotCollector;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 300;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final ClientCommunicator.ClientForwarder forwarder;
    protected InetSocketAddress left;
    protected InetSocketAddress right;

    protected ArrayList<Serializable> localState;
    protected RecordingState recState = RecordingState.IDLE;

    // Map that keeps track of every fish that has been in this tank with his state
    protected Map<String, FishState> fishTraces = new HashMap<>();
    // Map that keeps every fish that belongs to this tank and his current address
    protected Map<String, InetSocketAddress> homeAgent = new HashMap<>();

    // true = forwardingRefrende, false=Heimatgestuetzter Ansatz
    protected boolean useForwardingRefrence = false;



    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
    }

    /**
     * updates the location of the fish
     * @param fishID the ID of the fish
     * @param location the new address of the fish
     */
    public void updateFishLocation(String fishID, InetSocketAddress location) {
        homeAgent.put(fishID, location);
    }

    protected enum FishState {
        HERE,
        LEFT,
        RIGHT
    }

    protected enum RecordingState {
        IDLE,
        LEFT,
        RIGHT,
        BOTH
    }

    /**
     * locate the fish with the given ID
     *
     * @param fishID the ID of the Fish
     */
    public void locateFishGlobally(String fishID) {

        if (useForwardingRefrence) {
            // fish Here
            if (locateFishLocally(fishID)) {
                return;
            } else {
                InetSocketAddress neighbor = fishTraces.get(fishID) == FishState.LEFT ? left : right;
                forwarder.sendLocationRequest(neighbor, fishID);
            }
        } else {
            InetSocketAddress location = homeAgent.get(fishID);
            // fish here
            if (location == null) {
                locateFishLocally(fishID);
            } else {
                forwarder.sendLocationRequest(location, fishID);
            }
        }
    }

    /**
     * try to locate the fish locally and tag him if present
     *
     * @param fishID the fish to search for
     * @return true if the fish was found, false if the fish is not present
     */
    public boolean locateFishLocally(String fishID) {
        for (FishModel f : fishies) {
            if (f.getId().equals(fishID)) {
                f.toggle();
                return true;
            }
        }
        return false;
    }

    protected SnapshotCollector snapshotCollector = null;
    protected int globalState = -1;

    private boolean doIhaveIt = false;
    private Timer timer = new Timer();

    synchronized void initiateSnapshot(RecordingState channels) {
        System.out.println("Initiating Snapshot!");
        // List for saving incoming changes
        this.localState = new ArrayList<Serializable>();
        // start recording all incoming channels
        this.recState = channels;
        // send markers to all outgoing channels
        forwarder.sendSnapshotMarker(left);
        forwarder.sendSnapshotMarker(right);
    }

    synchronized void initCollector() {
        // create SnapshotCollector with own id as initiator and add localState to it
        SnapshotCollector collectToken = new SnapshotCollector(id);
        collectToken.addSnapshot(fishCounter + localState.size());
        // send to the left neighbor
        forwarder.sendSnapshotCollector(left, collectToken);
        System.out.println("Sending Collector!");
    }

    synchronized void handOffCollector() {
        SnapshotCollector tmp = this.snapshotCollector;
        this.snapshotCollector = null;
        // if initiator receives the SnapshotCollector back
        if (tmp.getInitiator().equals(id)) {
            globalState = tmp.getFishCount();
            return;
        }
        // add localeSnapshot to the SnapshotCollector and send it
        tmp.addSnapshot(fishCounter + localState.size());
        forwarder.sendSnapshotCollector(left, tmp);
    }

    synchronized void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    /**
     * sends an update that the fish is now in this tank
     * @param homeTank the tank where the fish is from
     * @param fishID the ID of the fish
     */
    public synchronized void sendFishUpdate(InetSocketAddress homeTank, String fishID) {
        forwarder.sendLocationUpdate(homeTank, fishID);
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishies.add(fish);

            // add the fish to the tracking lists
            fishTraces.put(fish.getId(), FishState.HERE);
            homeAgent.put(fish.getId(), null);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        if (useForwardingRefrence) {
            // add/update Trace
            fishTraces.put(fish.getId(), FishState.HERE);
        } else {
            // update homeAgent
            // fish is from this tank
            if (fish.getTankId().equals(id)) {
                homeAgent.put(fish.getId(), null);
            } else {
                forwarder.sendNameResolveRequest(fish.getTankId(), fish.getId());
            }
        }

        // Record Channel if recState says so
        Direction direction = fish.getDirection();
        if (direction == Direction.LEFT && this.recState == RecordingState.LEFT || this.recState == RecordingState.BOTH) {
            this.localState.add(fish);
        } else if (direction == Direction.RIGHT && this.recState == RecordingState.RIGHT) {
            this.localState.add(fish);
        }

        fish.setToStart();
        fishies.add(fish);
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge())
                // If i dont have the token reverse the fish
                if (!doIhaveIt) {
                    fish.reverse();
                } else {
                    forwarder.handOff(fish, this);

                    if (useForwardingRefrence) {
                        // update Trace with the direction the fish went
                        FishState state = fish.getDirection() == Direction.LEFT ? FishState.LEFT : FishState.RIGHT;
                        fishTraces.put(fish.getId(), state);
                    }
                }

            if (fish.disappears())
                it.remove();
        }
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void receiveToken() {
        this.doIhaveIt = true;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                doIhaveIt = false;
                forwarder.handOffToken(left);
            }
        }, 3000);
    }

    public boolean hasToken() {
        return doIhaveIt;
    }

    public synchronized void finish() {
        forwarder.deregister(id);
    }

}