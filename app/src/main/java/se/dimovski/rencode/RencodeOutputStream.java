package se.dimovski.rencode;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class RencodeOutputStream extends FilterOutputStream implements DataOutput
{

    /**
     * The {@link String} charset.
     */
    private final String charset;

    /**
     * Creates a {@link RencodeOutputStream} with the default charset.
     */
    public RencodeOutputStream(OutputStream out)
    {
        this(out, Utils.UTF_8);
    }

    /**
     * Creates a {@link RencodeOutputStream} with the given encoding.
     */
    public RencodeOutputStream(OutputStream out, String charset)
    {
        super(out);

        if (charset == null)
        {
            throw new NullPointerException("charset");
        }

        this.charset = charset;
    }

    /**
     * Returns the charset that is used to encode {@link String}s. The default
     * value is UTF-8.
     */
    public String getCharset()
    {
        return charset;
    }

    /**
     * Writes an {@link Object}.
     */
    public void writeObject(Object value) throws IOException
    {
        if (value == null)
        {
            writeNull();
        }
        else if (value instanceof byte[])
        {
            writeBytes((byte[]) value);
        }
        else if (value instanceof Boolean)
        {
            writeBoolean((Boolean) value);

        }
        else if (value instanceof Character)
        {
            writeChar((Character) value);

        }
        else if (value instanceof Number)
        {
            writeNumber((Number) value);

        }
        else if (value instanceof String)
        {
            writeString((String) value);

        }
        else if (value instanceof Collection<?>)
        {
            writeCollection((Collection<?>) value);

        }
        else if (value instanceof Map<?, ?>)
        {
            writeMap((Map<?, ?>) value);

        }
        else if (value instanceof Enum<?>)
        {
            writeEnum((Enum<?>) value);

        }
        else if (value.getClass().isArray())
        {
            writeArray(value);

        }
        else
        {
            writeCustom(value);
        }
    }

    /**
     * Writes a null value
     */
    public void writeNull() throws IOException
    {
        write(TypeCode.NULL);
    }

    /**
     * Overwrite this method to write custom objects. The default implementation
     * throws an {@link IOException}.
     */
    protected void writeCustom(Object value) throws IOException
    {
        throw new IOException("Cannot encode " + value);
    }

    /**
     * Writes the given byte-Array
     */
    public void writeBytes(byte[] value) throws IOException
    {
        writeBytes(value, 0, value.length);
    }

    /**
     * Writes the given byte-Array
     */
    public void writeBytes(byte[] value, int offset, int length) throws IOException
    {
        write(value, offset, length);
    }

    /**
     * Writes a boolean
     */
    public void writeBoolean(boolean value) throws IOException
    {
        write(value ? TypeCode.TRUE : TypeCode.FALSE);
    }

    /**
     * Writes a char
     */
    public void writeChar(int value) throws IOException
    {
        writeByte(value);
    }

    /**
     * Writes a byte
     */
    public void writeByte(int value) throws IOException
    {
        write(TypeCode.BYTE);
        write(value);
    }

    /**
     * Writes a short
     */
    public void writeShort(int value) throws IOException
    {
        write(TypeCode.SHORT);
        ByteBuffer buffer = ByteBuffer.allocate(Utils.SHORT_BYTES).putShort((short) value);
        write(buffer.array());
    }

    /**
     * Writes an int
     */
    public void writeInt(int value) throws IOException
    {
        write(TypeCode.INT);
        ByteBuffer buffer = ByteBuffer.allocate(Utils.INTEGER_BYTES).putInt(value);
        write(buffer.array());
    }

    /**
     * Writes a long
     */
    public void writeLong(long value) throws IOException
    {
        write(TypeCode.LONG);
        ByteBuffer buffer = ByteBuffer.allocate(Utils.LONG_BYTES).putLong(value);
        write(buffer.array());
    }

    /**
     * Writes a float
     */
    public void writeFloat(float value) throws IOException
    {
        write(TypeCode.FLOAT);
        ByteBuffer buffer = ByteBuffer.allocate(Utils.FLOAT_BYTES).putFloat(value);
        write(buffer.array());
    }

    /**
     * Writes a double
     */
    public void writeDouble(double value) throws IOException
    {
        write(TypeCode.DOUBLE);
        ByteBuffer buffer = ByteBuffer.allocate(Utils.DOUBLE_BYTES).putDouble(value);
        write(buffer.array());
    }

    /**
     * Writes a {@link Number}
     */
    public void writeNumber(Number num) throws IOException
    {
        if (num instanceof Float)
        {
            writeFloat(num.floatValue());
        }
        else if (num instanceof Double)
        {
            writeDouble(num.doubleValue());
        }
        if (0 <= num.intValue() && num.intValue() < TypeCode.EMBEDDED.INT_POS_COUNT)
        {
            write(TypeCode.EMBEDDED.INT_POS_START + num.intValue());
        }
        else if (-TypeCode.EMBEDDED.INT_NEG_COUNT <= num.intValue() && num.intValue() < 0)
        {
            write(TypeCode.EMBEDDED.INT_NEG_START - 1 - num.intValue());
        }
        else if (Byte.MIN_VALUE <= num.intValue() && num.intValue() < Byte.MAX_VALUE)
        {
            writeByte(num.byteValue());
        }
        else if (Short.MIN_VALUE <= num.intValue() && num.intValue() < Short.MAX_VALUE)
        {
            writeShort(num.shortValue());
        }
        else if (Integer.MIN_VALUE <= num.longValue() && num.longValue() < Integer.MAX_VALUE)
        {
            writeInt(num.intValue());
        }
        else if (Long.MIN_VALUE <= num.longValue() && num.longValue() < Long.MAX_VALUE)
        {
            writeLong(num.longValue());
        }
        else
        {
            String number = num.toString();
            write(TypeCode.NUMBER);
            write(number.getBytes(charset));
            write(TypeCode.END);
        }
    }

    /**
     * Writes a {@link String}
     */
    public void writeString(String value) throws IOException
    {
        int len = value.length();
        if (len < TypeCode.EMBEDDED.STR_COUNT)
        {
            write(TypeCode.EMBEDDED.STR_START + len);
        }
        else
        {
            String lenString = Integer.toString(len);
            writeBytes(lenString.getBytes(charset));
            write(TypeCode.LENGTH_DELIM);
        }

        writeBytes(value.getBytes(charset));
    }

    /**
     * Writes a {@link Collection}.
     */
    public void writeCollection(Collection<?> value) throws IOException
    {
        boolean useEndToken = value.size() >= TypeCode.EMBEDDED.LIST_COUNT;
        if (useEndToken)
        {
            write(TypeCode.LIST);
        }
        else
        {
            write(TypeCode.EMBEDDED.LIST_START + value.size());
        }

        for (Object element : value)
        {
            writeObject(element);
        }

        if (useEndToken)
        {
            write(TypeCode.END);
        }
    }

    /**
     * Writes a {@link Map}.
     */
    public void writeMap(Map<?, ?> map) throws IOException
    {
        if (!(map instanceof SortedMap<?, ?>))
        {
            map = new TreeMap<Object, Object>(map);
        }

        boolean untilEnd = map.size() >= TypeCode.EMBEDDED.DICT_COUNT;

        if (untilEnd)
        {
            write(TypeCode.DICTIONARY);
        }
        else
        {
            write(TypeCode.EMBEDDED.DICT_START + map.size());
        }

        for (Map.Entry<?, ?> entry : map.entrySet())
        {
            writeObject(entry.getKey());
            writeObject(entry.getValue());
        }

        if (untilEnd)
        {
            write(TypeCode.END);
        }
    }

    /**
     * Writes an {@link Enum}.
     */
    public void writeEnum(Enum<?> value) throws IOException
    {
        writeString(value.name());
    }

    /**
     * Writes an array
     */
    public void writeArray(Object value) throws IOException
    {
        int length = Array.getLength(value);
        boolean useEndToken = length >= TypeCode.EMBEDDED.LIST_COUNT;
        if (useEndToken)
        {
            write(TypeCode.LIST);
        }
        else
        {
            write(TypeCode.EMBEDDED.LIST_START + length);
        }

        for (int i = 0; i < length; i++)
        {
            writeObject(Array.get(value, i));
        }

        if (useEndToken)
        {
            write(TypeCode.END);
        }
    }

    /**
     * Writes the given {@link String}
     */
    public void writeBytes(String value) throws IOException
    {
        writeString(value);
    }

    /**
     * Writes the given {@link String}
     */
    public void writeChars(String value) throws IOException
    {
        writeString(value);
    }

    /**
     * Writes an UTF encoded {@link String}
     */
    public void writeUTF(String value) throws IOException
    {
        writeBytes(value.getBytes(Utils.UTF_8));
    }
}
