package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.xmlcreator.XmlElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Base64;
import android.util.Log;

/**
 * The serializer handler serializes and deserialized objects.
 * It takes an object, determine its type and let the responsible handler serialize it.
 * For deserialization it looks at the xml tag around the element.
 * The class is designed as a kind of singleton, so it can be accessed from anywhere in
 * the library.
 *
 * @author Tim Roes
 */
public class SerializerHandler {
	private static final String LOG_NAME = "SerializerHandler";

	public static final String TAG_NAME = "name";
	public static final String TAG_MEMBER = "member";
	public static final String TAG_VALUE = "value";
	public static final String TAG_DATA = "data";


	public static final String TYPE_DATE_TIME_ISO8601 = "dateTime.iso8601";

	static SimpleDateFormat dateFormat = DateTimeSerializer.DATE_FORMATER;
	static Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));

	public static final String TYPE_STRING = "string";
	public static final String TYPE_BOOLEAN = "boolean";
	public static final String TYPE_INT = "int";
	public static final String TYPE_INT2 = "i4";
	public static final String TYPE_LONG = "i8";
	public static final String TYPE_DOUBLE = "double";
	public static final String TYPE_DATETIME = "dateTime.iso8601";
	public static final String TYPE_STRUCT = "struct";
	public static final String TYPE_ARRAY = "array";
	public static final String TYPE_BASE64 = "base64";
	public static final String TYPE_NULL = "nil";

	private StringSerializer string;
	private BooleanSerializer bool = new BooleanSerializer();
	private IntSerializer integer = new IntSerializer();
	private LongSerializer long8 = new LongSerializer();
	private StructSerializer struct;
	private DoubleSerializer floating = new DoubleSerializer();
	private DateTimeSerializer datetime;
	public static boolean accepts_null_input;
	public SerializerHandler(boolean accepts_null_input) {
		SerializerHandler.accepts_null_input = accepts_null_input;
	}
	private ArraySerializer array;
	private Base64Serializer base64 = new Base64Serializer();
	private NullSerializer nil = new NullSerializer();

	private int flags;

	public SerializerHandler(int flags) {
		this.flags = flags;
		string = new StringSerializer((flags & XMLRPCClient.FLAGS_NO_STRING_ENCODE) == 0);
		struct = new StructSerializer(this);
		array = new ArraySerializer(this);
		datetime = new DateTimeSerializer((flags & XMLRPCClient.FLAGS_ACCEPT_NULL_DATES) != 0);
	}

	/**
	 * Deserialize an incoming xml to a java object.
	 * The type of the returning object depends on the type tag.
	 *
	 * @param parser Initialized parser.
	 * @return The deserialized object.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static Object deserialize(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
		parser.require(XmlPullParser.START_TAG, null, TAG_VALUE);

		parser.nextTag();
		String typeNodeName = parser.getName();

		Object obj;
		if (typeNodeName.equals(TYPE_INT) || typeNodeName.equals(TYPE_INT2)) {
			String value = parser.nextText();
			try {
				obj = Integer.parseInt(value);
			} catch (NumberFormatException nfe) {
				Log.w(LOG_NAME, "Server replied with an invalid 4 bytes int value, trying to parse it as 8 bytes long.");
				obj = Long.parseLong(value);
			}
		} else
		if (typeNodeName.equals(TYPE_LONG)) {
			String value = parser.nextText();
			obj = Long.parseLong(value);
		} else
		if (typeNodeName.equals(TYPE_DOUBLE)) {
			String value = parser.nextText();
			obj = Double.parseDouble(value);
		} else
		if (typeNodeName.equals(TYPE_BOOLEAN)) {
			String value = parser.nextText();
			obj = value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
		} else
		if (typeNodeName.equals(TYPE_STRING)) {
			obj = parser.nextText();
		} else
		if (typeNodeName.equals(TYPE_DATE_TIME_ISO8601)) {

			dateFormat.setCalendar(cal);
			String value = parser.nextText();
			if (accepts_null_input && (value==null || value.trim().length()==0)) {
				return null;
			}
			try {
				obj = dateFormat.parseObject(value);
			} catch (ParseException e) {
				Log.e(LOG_NAME,  "Error parsing date, using non-parsed string.");
				obj = value;
			}
		} else
		if (typeNodeName.equals(TYPE_BASE64)) {
			String value = parser.nextText();
			BufferedReader reader = new BufferedReader(new StringReader(value));
			String line;
			StringBuffer sb = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			obj = Base64.decode(sb.toString(), Base64.DEFAULT);
		} else
		if (typeNodeName.equals(TYPE_ARRAY)) {
			parser.nextTag(); // TAG_DATA (<data>)
			parser.require(XmlPullParser.START_TAG, null, TAG_DATA);

			parser.nextTag();
			List<Object> list = new ArrayList<Object>();
			while (parser.getName().equals(TAG_VALUE)) {
				list.add(deserialize(parser));
				parser.nextTag();
			}
			parser.require(XmlPullParser.END_TAG, null, TAG_DATA);
			parser.nextTag(); // TAG_ARRAY (</array>)
			parser.require(XmlPullParser.END_TAG, null, TYPE_ARRAY);
			obj = list.toArray();
		} else
		if (typeNodeName.equals(TYPE_STRUCT)) {
			parser.nextTag();
			Map<String, Object> map = new HashMap<String, Object>();
			while (parser.getName().equals(TAG_MEMBER)) {
				String memberName = null;
				Object memberValue = null;
				while (true) {
					parser.nextTag();
					String name = parser.getName();
					if (name.equals(TAG_NAME)) {
						memberName = parser.nextText();
					} else
					if (name.equals(TAG_VALUE)) {
						memberValue = deserialize(parser);
					} else {
						break;
					}
				}
				if (memberName != null && memberValue != null) {
					map.put(memberName, memberValue);
				}
				parser.require(XmlPullParser.END_TAG, null, TAG_MEMBER);
				parser.nextTag();
			}
			parser.require(XmlPullParser.END_TAG, null, TYPE_STRUCT);
			obj = map;
		} else {
			throw new IOException("Cannot deserialize " + parser.getName());
		}
		parser.nextTag(); // TAG_VALUE (</value>)
		parser.require(XmlPullParser.END_TAG, null, TAG_VALUE);
		return obj;
	}


	/**
	 * Serialize an object to its representation as an xml element.
	 * The xml element will be the type element for the use within a value tag.
	 *
	 * @param object The object that should be serialized.
	 * @return The xml representation of this object.
	 * @throws XMLRPCException Will be thrown, if an error occurs (e.g. the object
	 * 		cannot be serialized to an xml element.
	 */
	public XmlElement serialize(Object object) throws XMLRPCException {

		Serializer s;

		if((flags & XMLRPCClient.FLAGS_NIL) != 0 && object == null) {
			s = nil;
		} else if(object instanceof String) {
			s = string;
		} else if(object instanceof Boolean) {
			s = bool;
		} else if(object instanceof Double || object instanceof Float
				|| object instanceof BigDecimal) {
			s = floating;
		} else if (object instanceof Integer || object instanceof Short
				|| object instanceof Byte) {
			s = integer;
		} else if(object instanceof Long) {
			// Check whether the 8 byte integer flag was set.
			if((flags & XMLRPCClient.FLAGS_8BYTE_INT) != 0) {
				s = long8;
			} else {
				// Allow long values as long as their fit within the 4 byte integer range.
				long l = (Long)object;
				if(l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
					throw new XMLRPCException("FLAGS_8BYTE_INT must be set, if values "
							+ "outside the 4 byte integer range should be transfered.");
				} else {
					s = integer;
				}
			}
		} else if(object instanceof Date) {
			s = datetime;
		} else if(object instanceof Calendar) {
			object = ((Calendar)object).getTime();
			s = datetime;
		} else if (object instanceof Map) {
			s = struct;
		} else if(object instanceof byte[]) {
			byte[] old = (byte[])object;
			Byte[] boxed = new Byte[old.length];
			for(int i = 0; i < boxed.length; i++) {
				boxed[i] = new Byte(old[i]);
			}
			object = boxed;
			s = base64;
		} else if(object instanceof Byte[]) {
			s = base64;
		} else if(object instanceof Iterable<?> || object instanceof Object[]) {
			s = array;
		} else {
			throw new XMLRPCException("No serializer found for type '"
					+ object.getClass().getName() + "'.");
		}

		return s.serialize(object);

	}

}
