package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt1.security.SecureEndpoint;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
    private final SecureEndpoint endpoint;

    public ClientCommunicator() {
        endpoint = new SecureEndpoint();
    }

    public class ClientForwarder {
        private final InetSocketAddress broker;

        private ClientForwarder() {
            this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
        }

        public void register() {
            endpoint.send(broker, new RegisterRequest());
        }

        public void deregister(String id) {
            endpoint.send(broker, new DeregisterRequest(id));
        }

        public void handOff(FishModel fish, TankModel tankModel) {
            // If the fish is swimming to the left: HandoffRequest to the left neighbor
            if (fish.getDirection().equals(Direction.LEFT)) {
                endpoint.send(tankModel.left, new HandoffRequest(fish));
            } else {
                endpoint.send(tankModel.right, new HandoffRequest(fish));
            }
        }

        public void sendSnapshotMarker(InetSocketAddress neighbor) {
            endpoint.send(neighbor, new SnapshotMarker());
        }

        public void handOffToken(InetSocketAddress neighbor) {
            endpoint.send(neighbor, new Token());
        }

        public void sendSnapshotCollector(InetSocketAddress neighbor, SnapshotCollector snapshotCollerctor) {
            endpoint.send(neighbor, snapshotCollerctor);
        }

        public void sendLocationRequest(InetSocketAddress location, String fishID) {
            endpoint.send(location, new LocationRequest(fishID));
        }

        public void sendLocationUpdate(InetSocketAddress homeTankAdress, String fishID) {

            endpoint.send(homeTankAdress, new LocationUpdate(fishID));
        }

        public void sendNameResolveRequest(String tankID, String requestID) {
            endpoint.send(broker, new NameResolutionRequest(tankID, requestID));
        }
    }

    public class ClientReceiver extends Thread {
        private final TankModel tankModel;

        private ClientReceiver(TankModel tankModel) {
            this.tankModel = tankModel;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                Message msg = endpoint.blockingReceive();

                if (msg.getPayload() instanceof RegisterResponse) {
                    tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLeaseDuration());
                }

                if (msg.getPayload() instanceof HandoffRequest) {
                    tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
                }

                if (msg.getPayload() instanceof NeighborUpdate) {
                    // If left neighbor gets updated
                    if (((NeighborUpdate) msg.getPayload()).getDirection().equals(Direction.LEFT)) {
                        tankModel.left = ((NeighborUpdate) msg.getPayload()).getNeighbor();
                        // Else right neighbor gets updated
                    } else {
                        tankModel.right = ((NeighborUpdate) msg.getPayload()).getNeighbor();
                    }
                }

                if (msg.getPayload() instanceof Token) {
                    tankModel.receiveToken();
                }

                if (msg.getPayload() instanceof SnapshotMarker) {
                    // Not in recording Mode
                    if (this.tankModel.recState == TankModel.RecordingState.IDLE) {
                        if (msg.getSender() == tankModel.right) {
                            this.tankModel.initiateSnapshot(TankModel.RecordingState.LEFT);
                        } else {
                            this.tankModel.initiateSnapshot(TankModel.RecordingState.RIGHT);
                        }
                    }
                    // In recording Mode
                    else {
                        if (msg.getSender() == tankModel.right) {
                            if (this.tankModel.recState == TankModel.RecordingState.BOTH) {
                                this.tankModel.recState = TankModel.RecordingState.LEFT;
                            } else {
                                this.tankModel.recState = TankModel.RecordingState.IDLE;
                            }
                        } else {
                            if (this.tankModel.recState == TankModel.RecordingState.BOTH) {
                                this.tankModel.recState = TankModel.RecordingState.RIGHT;
                            } else {
                                this.tankModel.recState = TankModel.RecordingState.IDLE;
                            }
                        }
                    }

                    if (this.tankModel.recState == TankModel.RecordingState.IDLE) {
                        System.out.println("LocaleSnapshot finished!");
                        if (this.tankModel.snapshotCollector != null) {
                            System.out.println("Sending Snapshot now!");
                            this.tankModel.handOffCollector();
                        }
                    }
                }

                if (msg.getPayload() instanceof SnapshotCollector) {
                    System.out.println("Received Collector, saving it for later!");
                    this.tankModel.snapshotCollector = (SnapshotCollector) msg.getPayload();
                    if (this.tankModel.recState == TankModel.RecordingState.IDLE) {
                        System.out.println("Sending Snapshot now!");
                        this.tankModel.handOffCollector();
                    }
                }

                if (msg.getPayload() instanceof LocationRequest) {
                    System.out.println("Received LocationRequest for: " + ((LocationRequest) msg.getPayload()).getRequestedFishID());
                    if (tankModel.useForwardingRefrence) {
                        tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getRequestedFishID());
                    } else {
                        // when using homeAgent no need to check if fish is here (must be here)
                        tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getRequestedFishID());
                    }
                }

                if (msg.getPayload() instanceof LocationUpdate) {
                    System.out.println("Received LocationUpdate for: " + (((LocationUpdate) msg.getPayload()).getFishID()));
                    String fishID = ((LocationUpdate) msg.getPayload()).getFishID();
                    InetSocketAddress location = msg.getSender();
                    tankModel.updateFishLocation(fishID, location);
                }

                if (msg.getPayload() instanceof NameResolutionResponse) {
                    System.out.println("Received NameResolutionRequest for: " + ((NameResolutionResponse) msg.getPayload()).getResponseID());
                    String fishID = (((NameResolutionResponse) msg.getPayload()).getResponseID());
                    InetSocketAddress tankAddress = ((NameResolutionResponse) msg.getPayload()).getTankAdress();
                    tankModel.sendFishUpdate(tankAddress, fishID);
                }

            }
            System.out.println("Receiver stopped.");
        }
    }

    public ClientForwarder newClientForwarder() {
        return new ClientForwarder();
    }

    public ClientReceiver newClientReceiver(TankModel tankModel) {
        return new ClientReceiver(tankModel);
    }

}
