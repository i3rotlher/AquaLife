package aqua.blatt1.security;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;


public class SecureEndpoint extends Endpoint {

    private Endpoint ep;
    private SecretKeySpec sk;

    private Cipher decrypt;
    private Cipher encrypt;

    public SecureEndpoint (int port) {
        try {
            this.sk = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
            this.ep = new Endpoint(port);

            this.encrypt = Cipher.getInstance("AES");
            encrypt.init(Cipher.ENCRYPT_MODE, sk);
            this.decrypt = Cipher.getInstance("AES");
            decrypt.init(Cipher.DECRYPT_MODE, sk);
        } catch (Exception e) {
            System.out.println("Mistake @ SecureEdnpoint constructor: " + e.getMessage());
        }
    }

    public SecureEndpoint () {
        try {
            this.sk = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
            this.ep = new Endpoint();

            this.encrypt = Cipher.getInstance("AES");
            encrypt.init(Cipher.ENCRYPT_MODE, sk);
            this.decrypt = Cipher.getInstance("AES");
            decrypt.init(Cipher.DECRYPT_MODE, sk);
        } catch (Exception e) {
            System.out.println("Mistake @ SecureEdnpoint constructor: " + e.getMessage());
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
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
        return decryptMessage(m);
    }

    public Message nonBlockingReceive() {
        Message m = ep.nonBlockingReceive();
        return decryptMessage(m);
    }

    private Message decryptMessage(Message m) {
        try {
            byte[] decrypted_payload_bytes = decrypt.doFinal((byte[]) m.getPayload());
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decrypted_payload_bytes));
            return new Message((Serializable) ois.readObject(), m.getSender());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


}
