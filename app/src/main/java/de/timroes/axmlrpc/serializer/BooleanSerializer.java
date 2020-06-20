package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 *
 * @author Tim Roes
 */
public class BooleanSerializer implements Serializer {

	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_BOOLEAN,
				(Boolean) object ? "1" : "0");
	}

}
