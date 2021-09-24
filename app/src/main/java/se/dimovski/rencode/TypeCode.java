package se.dimovski.rencode;

public class TypeCode
{
    // The bencode 'typecodes' such as i, d, etc have been
    // extended and relocated on the base-256 character set.
    public static final char LIST         = 59;
    public static final char DICTIONARY   = 60;
    public static final char NUMBER       = 61;
    public static final char BYTE         = 62;
    public static final char SHORT        = 63;
    public static final char INT          = 64;
    public static final char LONG         = 65;
    public static final char FLOAT        = 66;
    public static final char DOUBLE       = 44;
    public static final char TRUE         = 67;
    public static final char FALSE        = 68;
    public static final char NULL         = 69;
    public static final char END          = 127;
    public static final char LENGTH_DELIM = ':';

    /*
     * TypeCodes with embedded values/lengths
     */
    public static class EMBEDDED
    {
        // Positive integers
        public static final int INT_POS_START = 0;
        public static final int INT_POS_COUNT = 44;

        // Negative integers
        public static final int INT_NEG_START = 70;
        public static final int INT_NEG_COUNT = 32;

        // Dictionaries
        public static final int DICT_START    = 102;
        public static final int DICT_COUNT    = 25;

        // Strings
        public static final int STR_START     = 128;
        public static final int STR_COUNT     = 64;

        // Lists
        public static final int LIST_START    = STR_START + STR_COUNT;
        public static final int LIST_COUNT    = 64;
    }
}
