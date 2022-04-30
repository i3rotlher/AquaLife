package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public final class LocationRequest implements Serializable {
    private final String fishID;

    public LocationRequest(String fishID) {
        this.fishID = fishID;
    }

    public String getRequestedFishID() {
        return fishID;
    }
}
