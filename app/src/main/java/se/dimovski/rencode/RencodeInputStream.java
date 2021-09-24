package se.dimovski.rencode;

import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RencodeInputStream extends FilterInputStream implements DataInput {
    /**
     * The charset that is being used for {@link String}s.
     */
    private final String charset;

    /**
     * Whether or not all byte-Arrays should be decoded as {@link String}s.
     */
    private final boolean decodeAsString;

    /**
     * Creates a {@link RencodeInputStream} with the default encoding.
     */
    public RencodeInputStream(InputStream in) {
        this(in, Utils.UTF_8, false);
    }

    /**
     * Creates a {@link RencodeInputStream} with the given encoding.
     */
    public RencodeInputStream(InputStream in, String charset) {
        this(in, charset, false);
    }

    /**
     * Creates a {@link RencodeInputStream} with the default encoding.
     */
    public RencodeInputStream(InputStream in, boolean decodeAsString) {
        this(in, Utils.UTF_8, decodeAsString);
    }

    /**
     * Creates a {@link RencodeInputStream} with the given encoding.
     */
    public RencodeInputStream(InputStream in, String charset, boolean decodeAsString) {
        super(in);

        if (charset == null) {
            throw new IllegalArgumentException("charset is null");
        }

        this.charset = charset;
        this.decodeAsString = decodeAsString;
    }

    /**
     * Returns the charset that is used to decode {@link String}s. The default
     * value is UTF-8.
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Returns true if all byte-Arrays are being turned into {@link String}s.
     */
    public boolean isDecodeAsString() {
        return decodeAsString;
    }

    /**
     * Reads and returns an {@link Object}.
     */
    public Object readObject() throws IOException {
        int token = readToken();

        return readObject(token);
    }

    /**
     * Reads and returns an {@link Object}.
     */
    protected Object readObject(int token) throws IOException {
        if (token == TypeCode.DICTIONARY) {
            return readMap0(Object.class);
        } else if (Utils.isFixedDictionary(token)) {
            return readMap0(Object.class, token);
        } else if (token == TypeCode.LIST) {
            return readList0(Object.class);
        } else if (Utils.isFixedList(token)) {
            return readList0(Object.class, token);
        } else if (Utils.isNumber(token)) {
            return readNumber0(token);
        } else if (token == TypeCode.FALSE || token == TypeCode.TRUE) {
            return readBoolean0(token);
        } else if (token == TypeCode.NULL) {
            return null;
        } else if (Utils.isDigit(token) || Utils.isFixedString(token)) {
            return readString(token, charset);
        }

        throw new IOException("Not implemented: " + token);
    }

    /**
     * Reads and returns a {@link Map}.
     */
    public Map<String, ?> readMap() throws IOException {
        return readMap(Object.class);
    }

    /**
     * Reads and returns a {@link Map}.
     */
    public <T> Map<String, T> readMap(Class<T> clazz) throws IOException {
        int token = readToken();

        if (token != TypeCode.DICTIONARY) {
            throw new IOException();
        }

        return readMap0(clazz);
    }

    private <T> Map<String, T> readMap0(Class<T> clazz) throws IOException {
        Map<String, T> map = new TreeMap<String, T>();
        int token = -1;
        while ((token = readToken()) != TypeCode.END) {
            readMapItem(clazz, token, map);
        }

        return map;
    }

    private <T> Map<String, T> readMap0(Class<T> clazz, int token) throws IOException {
        Map<String, T> map = new TreeMap<String, T>();

        int count = token - TypeCode.EMBEDDED.DICT_START;
        for (int i = 0; i < count; i++) {
            readMapItem(clazz, readToken(), map);
        }

        return map;
    }

    private <T> void readMapItem(Class<T> clazz, int token, Map<String, T> map) throws UnsupportedEncodingException,
            IOException {
        String key = readString(token, charset);
        T value = clazz.cast(readObject());

        map.put(key, value);
    }

    public int readToken() throws IOException {
        int token = super.read();
        if (token == -1) {
            throw new EOFException();
        }
        return token;
    }

    /**
     * Reads and returns a {@link List}.
     */
    public List<?> readList() throws IOException {
        return readList(Object.class);
    }

    /**
     * Reads and returns a {@link List}.
     */
    public <T> List<T> readList(Class<T> clazz) throws IOException {
        int token = readToken();

        if (token != TypeCode.LIST) {
            throw new IOException();
        }

        return readList0(clazz);
    }

    private <T> List<T> readList0(Class<T> clazz) throws IOException {
        List<T> list = new ArrayList<T>();
        int token = -1;
        while ((token = readToken()) != TypeCode.END) {
            list.add(clazz.cast(readObject(token)));
        }
        return list;
    }

    private <T> List<T> readList0(Class<T> clazz, int token) throws IOException {
        List<T> list = new ArrayList<T>();
        int length = token - TypeCode.EMBEDDED.LIST_START;
        for (int i = 0; i < length; i++) {
            list.add(clazz.cast(readObject()));
        }
        return list;
    }

    public boolean readBoolean() throws IOException {
        return readBoolean0(readToken());
    }

    public boolean readBoolean0(int token) throws IOException {
        if (token == TypeCode.FALSE) {
            return false;
        } else if (token == TypeCode.TRUE) {
            return true;
        }

        throw new IOException();
    }

    public byte readByte() throws IOException {
        return (byte) readToken();
    }

    public char readChar() throws IOException {
        return (char) readToken();
    }

    public double readDouble() throws IOException {
        return readNumber().doubleValue();
    }

    public float readFloat() throws IOException {
        return readNumber().floatValue();
    }

    public void readFully(byte[] dst) throws IOException {
        readFully(dst, 0, dst.length);
    }

    public void readFully(byte[] dst, int off, int len) throws IOException {
        int total = 0;

        while (total < len) {
            int r = read(dst, total, len - total);
            if (r == -1) {
                throw new EOFException();
            }

            total += r;
        }
    }

    public int readInt() throws IOException {
        return readNumber().intValue();
    }

    public String readLine() throws IOException {
        return readString();
    }

    public long readLong() throws IOException {
        return readNumber().longValue();
    }

    public short readShort() throws IOException {
        return readNumber().shortValue();
    }

    public String readUTF() throws IOException {
        return readString(Utils.UTF_8);
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads and returns a {@link Number}.
     */
    public Number readNumber() throws IOException {
        int token = readToken();

        if (!Utils.isNumber(token)) {
            throw new IOException();
        }

        return readNumber0(token);
    }

    private Number readNumber0(int token) throws IOException {
        switch (token) {
            case TypeCode.BYTE:
                return (int) readToBuffer(1).get();
            case TypeCode.SHORT:
                return (int) readToBuffer(2).getShort();
            case TypeCode.INT:
                return readToBuffer(4).getInt();
            case TypeCode.LONG:
                return readToBuffer(8).getLong();
            case TypeCode.FLOAT:
                return readToBuffer(4).getFloat();
            case TypeCode.DOUBLE:
                return readToBuffer(8).getDouble();

            case TypeCode.NUMBER:
                return readNumber0();
        }
        if (Utils.isNegativeFixedNumber(token)) {
            return TypeCode.EMBEDDED.INT_NEG_START - 1 - token;
        } else if (Utils.isPositiveFixedNumber(token)) {
            return TypeCode.EMBEDDED.INT_POS_START + token;
        }

        throw new IOException("Unknown number. TypeCode: " + token);
    }

    private ByteBuffer readToBuffer(int count) throws IOException {
        return ByteBuffer.wrap(readBytesFixed(count));
    }

    private Number readNumber0() throws IOException {
        StringBuilder buffer = new StringBuilder();

        boolean decimal = false;

        int token = -1;
        while ((token = readToken()) != TypeCode.END) {
            if (token == '.') {
                decimal = true;
            }

            buffer.append((char) token);
        }

        try {
            if (decimal) {
                return new BigDecimal(buffer.toString());
            } else {
                return new BigInteger(buffer.toString());
            }
        } catch (NumberFormatException err) {
            throw new IOException("NumberFormatException", err);
        }
    }

    public int skipBytes(int n) throws IOException {
        return (int) skip(n);
    }

    /**
     * Reads and returns a byte-Array.
     */
    public byte[] readBytes() throws IOException {
        int token = readToken();

        return readBytes(token);
    }

    /**
     * Reads and returns a {@link String}.
     */
    public String readString() throws IOException {
        return readString(charset);
    }

    private String readString(String encoding) throws IOException {
        return readString(readToken(), encoding);
    }

    private String readString(int token, String charset) throws IOException {
        if (Utils.isFixedString(token)) {
            int length = token - TypeCode.EMBEDDED.STR_START;
            return new String(readBytesFixed(length), charset);
        }
        return new String(readBytes(token), charset);
    }

    private byte[] readBytes(int token) throws IOException {
        int length = readLength(token);
        return readBytesFixed(length);
    }

    private byte[] readBytesFixed(int count) throws IOException {
        byte[] data = new byte[count];
        readFully(data);
        return data;
    }

    private int readLength(int token) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append((char) token);

        while ((token = readToken()) != TypeCode.LENGTH_DELIM) {

            buffer.append((char) token);
        }

        return Integer.parseInt(buffer.toString());
    }
}
