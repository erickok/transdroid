package deluge.impl.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class SSL3Socket
{

    public static SSLSocket createSSLv3Socket(String address, int port) throws KeyManagementException,
            UnknownHostException, IOException, NoSuchAlgorithmException
    {
        final TrustManager[] trustAllCerts = new TrustManager[] { new AcceptAllTrustManager() };

        final SSLContext sc = SSLContext.getInstance("SSLv3");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        final SSLSocket mySocket = (SSLSocket) sc.getSocketFactory().createSocket(address, port);

        final String[] protocols = { "SSLv3", "TLSv1" };
        mySocket.setEnabledProtocols(protocols);

        mySocket.addHandshakeCompletedListener(new HandshakeCompletedListener()
        {

            public void handshakeCompleted(HandshakeCompletedEvent event)
            {
                System.out.println("Handshake complete");
            }
        });

        return mySocket;
    }
}
