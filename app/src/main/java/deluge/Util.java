package deluge;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util
{

    public static void close(Closeable closeable) throws IOException
    {
        if (closeable == null)
        {
            return;
        }
        closeable.close();
    }

    public static long copy(InputStream from, OutputStream to) throws IOException
    {
        try
        {
            try
            {
                byte[] buf = new byte[4000];
                long total = 0;
                while (true)
                {
                    int r = from.read(buf);
                    if (r == -1)
                    {
                        break;
                    }
                    to.write(buf, 0, r);
                    total += r;
                }
                return total;
            }
            finally
            {
                Util.close(to);
            }
        }
        finally
        {
            Util.close(from);
        }
    }
}
