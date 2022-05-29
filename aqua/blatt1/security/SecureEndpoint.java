package aqua.blatt1.security;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.Map;


public class SecureEndpoint extends Endpoint {

    private Endpoint ep;
    // private SecretKeySpec sk;

    private KeyPairGenerator kpg;
    private KeyPair akp;
    private Map<InetSocketAddress, Key> partner_keys = new HashMap<>();

    private Cipher decrypt;

    public SecureEndpoint (int port) {
        try {
            // this.sk = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
            this.kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            this.akp = kpg.genKeyPair();

            this.ep = new Endpoint(port);

            this.decrypt = Cipher.getInstance("RSA");
            decrypt.init(Cipher.DECRYPT_MODE, akp.getPrivate());

        } catch (Exception e) {
            System.out.println("Mistake @ SecureEdnpoint constructor: " + e.getMessage());
        }
    }

    public SecureEndpoint () {
        try {
            // this.sk = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
            this.kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            this.akp = kpg.genKeyPair();

            this.ep = new Endpoint();

            this.decrypt = Cipher.getInstance("RSA");
            decrypt.init(Cipher.DECRYPT_MODE, akp.getPrivate());

        } catch (Exception e) {
            System.out.println("Mistake @ SecureEdnpoint constructor: " + e.getMessage());
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {

        if (!partner_keys.containsKey(receiver)) {
            // sende KeyExchange
            ep.send(receiver, new KeyExchangeMessage(akp.getPublic()));
            blockingReceive();
        }

        Key receiver_public_key = partner_keys.get(receiver);

        try {
            Cipher encrypt = Cipher.getInstance("RSA");
            encrypt.init(Cipher.ENCRYPT_MODE, receiver_public_key);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(payload);
            byte[] bytes = baos.toByteArray();
            byte[] encrypted_bytes = encrypt.doFinal(bytes);
            ep.send(receiver, encrypted_bytes);
        } catch (Exception var7) {
            throw new RuntimeException(var7);
        }
    }

    public Message blockingReceive() {
        Message m = ep.blockingReceive();
        if (m.getPayload() instanceof KeyExchangeMessage) {
            partner_keys.put(m.getSender(), ((KeyExchangeMessage) m.getPayload()).getPublicKey());
            ep.send(m.getSender(), new KeyExchangeMessage(akp.getPublic()));
            return new Message("Der hat sich den Key gezogen", m.getSender());
        }
        return decryptMessage(m);
    }

    public Message nonBlockingReceive() {
        Message m = ep.nonBlockingReceive();
        if (m.getPayload() instanceof KeyExchangeMessage) {
            if (partner_keys.containsKey(m.getSender())) {
                ep.send(m.getSender(), new KeyExchangeMessage(akp.getPublic()));
            }
            partner_keys.put(m.getSender(), (Key) m.getPayload());
            nonBlockingReceive();
        }
        return decryptMessage(m);
    }

    private Message decryptMessage(Message m) {
        try {
            byte[] decrypted_payload_bytes = decrypt.doFinal((byte[]) m.getPayload());
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decrypted_payload_bytes));
            return new Message((Serializable) ois.readObject(), m.getSender());
        } catch (Exception e) {
            System.out.println("couldnt decrypt message: " + m.getPayload());
        }
        return null;
    }


}
