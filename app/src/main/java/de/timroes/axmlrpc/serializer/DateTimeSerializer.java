package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.w3c.dom.Element;

/**
 *
 * @author timroes
 */
public class DateTimeSerializer implements Serializer {

	public static final String DATETIME_FORMAT = "yyyyMMdd'T'HH:mm:ss";
	public static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat(DATETIME_FORMAT);

	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_DATETIME,
				DATE_FORMATER.format(object));
	}

}
