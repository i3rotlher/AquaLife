package aqua.blatt1.common.msgtypes;
import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NameResolutionResponse implements Serializable {
    private final String responseID;
    private final InetSocketAddress tankAdress;

    public NameResolutionResponse(String responseID, InetSocketAddress tankAdress) {
        this.responseID = responseID;
        this.tankAdress = tankAdress;
    }

    public InetSocketAddress getTankAdress() {
        return tankAdress;
    }

    public String getResponseID() {
        return responseID;
    }
}