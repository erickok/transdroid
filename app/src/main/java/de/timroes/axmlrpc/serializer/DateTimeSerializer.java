package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import java.text.SimpleDateFormat;

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
