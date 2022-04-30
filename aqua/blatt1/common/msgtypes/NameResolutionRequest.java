package aqua.blatt1.common.msgtypes;
import java.io.Serializable;

@SuppressWarnings("serial")
public final class NameResolutionRequest implements Serializable {
    private String tankID;
    private String requestID;

    public NameResolutionRequest(String tankID, String requestID) {
        this.tankID = tankID;
        this.requestID = requestID;
    }

    public String getRequestID() {
        return requestID;
    }

    public String getTankID() {
        return tankID;
    }
}
