package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.Key;

@SuppressWarnings("serial")
public final class KeyExchangeMessage implements Serializable {
    private final Key publicKey;

    public KeyExchangeMessage(Key publicKey) {
        this.publicKey = publicKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }
}
