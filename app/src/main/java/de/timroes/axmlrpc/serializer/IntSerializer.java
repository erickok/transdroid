package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 *
 * @author timroes
 */
public class IntSerializer implements Serializer {

	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_INT,
				object.toString());
	}

}
