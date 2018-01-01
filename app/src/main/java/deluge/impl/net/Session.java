package deluge.impl.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLSocket;

import deluge.Util;

public class Session
{
    public interface DataCallback
    {
        public void dataRecived(byte[] data);
    }

    public static byte[] decompressByteArray(byte[] data) throws IOException
    {
        final InputStream from = new InflaterInputStream(new ByteArrayInputStream(data));
        final ByteArrayOutputStream to = new ByteArrayOutputStream();
        Util.copy(from, to);
        byte[] output = to.toByteArray();
        from.close();
        to.close();
        return output;
    }

    private static byte[] decompress(byte[] input) throws DataFormatException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final Inflater decompresser = new Inflater(false);

        decompresser.setInput(input, 0, input.length);
        final byte[] result = new byte[1024];
        while (!decompresser.finished())
        {
            final int resultLength = decompresser.inflate(result);
            baos.write(result, 0, resultLength);
        }
        decompresser.end();

        final byte[] returnValue = baos.toByteArray();
        try
        {
            baos.close();
        }
        catch (final IOException e)
        {
        }
        return returnValue;
    }

    private final BlockingQueue<byte[]> queue  = new ArrayBlockingQueue<byte[]>(50);
    private SSLSocket                   mySocket;
    String                              myAddress;
    int                                 myPort;
    Thread                              sender = null;

    public final CountDownLatch         latch  = new CountDownLatch(1);

    public Session(String address, int port)
    {
        this.myAddress = address;
        this.myPort = port;
    }

    public byte[] compress(byte[] data) throws IOException
    {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final Deflater d = new Deflater();
        final DeflaterOutputStream dout = new DeflaterOutputStream(baos, d);
        dout.write(data);
        dout.close();

        final byte[] output = baos.toByteArray();
        baos.close();
        return output;
    }

    private void createSocket()
    {
        if (this.mySocket == null)
        {
            try
            {
                this.mySocket = SSL3Socket.createSSLv3Socket(this.myAddress, this.myPort);
                this.mySocket.startHandshake();
            }
            catch (final Exception e1)
            {
                e1.printStackTrace();
                this.mySocket = null;
            }
        }
        this.latch.countDown();
    }

    public void listen(final DataCallback cb) throws IOException
    {
        new Thread(new Runnable()
        {

            public void run()
            {
                createSocket();
                System.out.println("Listening Thread started");
                try
                {
                    while (Session.this.mySocket != null)
                    {

                        final InputStream inputStream = new BufferedInputStream(Session.this.mySocket.getInputStream());

                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        int bytesRead;
                        final byte[] buffer = new byte[1024];
                        while ((bytesRead = inputStream.read(buffer)) != -1)
                        {
                            baos.write(buffer);

                            if (bytesRead < 1024)
                            {
                                final byte[] unpacked = Session.decompressByteArray(baos.toByteArray());
                                baos.reset();
                                cb.dataRecived(unpacked);
                            }
                        }
                    }
                }
                catch (final UnsupportedEncodingException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (final IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }).start();

        try
        {
            this.latch.await(3, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e)
        {
        }
        if (this.mySocket == null)
        {
            throw new IOException();
        }
    }

    public void send(byte[] request) throws IOException
    {
        if (this.sender == null)
        {
            sender();
        }
        try
        {
            this.queue.put(request);
        }
        catch (final InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void sender() throws IOException
    {
        this.sender = new Thread(new Runnable()
        {

            public void run()
            {
                createSocket();
                System.out.println("Sending Thread started");
                try
                {
                    while (Session.this.mySocket != null)
                    {
                        byte[] packedData;
                        try
                        {
                            final byte[] x = Session.this.queue.take();
                            packedData = compress(x);

                            final OutputStream out = new BufferedOutputStream(Session.this.mySocket.getOutputStream());
                            out.write(packedData);
                            out.flush();
                        }
                        catch (final InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }

                }
                catch (final IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
        this.sender.start();

        try
        {
            this.latch.await(3, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e)
        {
        }
        if (this.mySocket == null)
        {
            throw new IOException();
        }
    }

}
