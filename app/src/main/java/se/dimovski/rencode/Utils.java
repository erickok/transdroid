package se.dimovski.rencode;

public class Utils
{
    // Character Encodings
    public final static String UTF_8          = "UTF-8";
    public final static String ISO_8859       = "ISO-8859-1";

    // Byte-lengths for types
    public static final int    SHORT_BYTES    = Short.SIZE / Byte.SIZE;
    public static final int    INTEGER_BYTES  = Integer.SIZE / Byte.SIZE;
    public static final int    LONG_BYTES     = Long.SIZE / Byte.SIZE;
    public static final int    FLOAT_BYTES    = Float.SIZE / Byte.SIZE;
    public static final int    DOUBLE_BYTES   = Double.SIZE / Byte.SIZE;

    // Maximum length of integer when written as base 10 string.
    public static final int    MAX_INT_LENGTH = 64;

    private static boolean tokenInRange(int token, int start, int count)
    {
        return start <= token && token < (start + count);
    }

    public static boolean isNumber(int token)
    {
        switch (token)
        {
            case TypeCode.NUMBER:
            case TypeCode.BYTE:
            case TypeCode.SHORT:
            case TypeCode.INT:
            case TypeCode.LONG:
            case TypeCode.FLOAT:
            case TypeCode.DOUBLE:
                return true;
        }
        return isFixedNumber(token);
    }

    public static boolean isFixedNumber(int token)
    {
        return isPositiveFixedNumber(token) || isNegativeFixedNumber(token);
    }

    public static boolean isPositiveFixedNumber(int token)
    {
        return tokenInRange(token, TypeCode.EMBEDDED.INT_POS_START, TypeCode.EMBEDDED.INT_POS_COUNT);
    }

    public static boolean isNegativeFixedNumber(int token)
    {
        return tokenInRange(token, TypeCode.EMBEDDED.INT_NEG_START, TypeCode.EMBEDDED.INT_NEG_COUNT);
    }

    public static boolean isFixedList(int token)
    {
        return tokenInRange(token, TypeCode.EMBEDDED.LIST_START, TypeCode.EMBEDDED.LIST_COUNT);
    }

    public static boolean isFixedDictionary(int token)
    {
        return tokenInRange(token, TypeCode.EMBEDDED.DICT_START, TypeCode.EMBEDDED.DICT_COUNT);
    }

    public static boolean isFixedString(int token)
    {
        return tokenInRange(token, TypeCode.EMBEDDED.STR_START, TypeCode.EMBEDDED.STR_COUNT);
    }

    public static boolean isDigit(int token)
    {
        return '0' <= token && token <= '9';
    }
}
