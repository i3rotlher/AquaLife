package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public final class SnapshotCollector implements Serializable {
    private int fishCount = 0;
    String initiator;

    public SnapshotCollector(String initiator) {
        this.initiator = initiator;
    }

    public void addSnapshot(int fishCount) {
        this.fishCount += fishCount;
    }

    public int getFishCount() {
        return fishCount;
    }

    public String getInitiator() {
        return initiator;
    }
}
