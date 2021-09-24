package se.dimovski.rencode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Rencode
{

    public static Object decode(byte[] data) throws IOException
    {
        final InputStream is = new ByteArrayInputStream(data);
        final RencodeInputStream inputStream = new RencodeInputStream(is);

        final Object decoded = inputStream.readObject();
        inputStream.close();

        return decoded;
    }

    public static byte[] encode(Object obj) throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final RencodeOutputStream output = new RencodeOutputStream(baos);
        output.writeObject(obj);
        final byte[] encoded = baos.toByteArray();
        output.close();
        return encoded;
    }

}
