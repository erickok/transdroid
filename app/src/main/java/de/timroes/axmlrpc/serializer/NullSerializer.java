package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 *
 * @author Tim Roes
 */
public class NullSerializer implements Serializer {

	public XmlElement serialize(Object object) {
		return new XmlElement(SerializerHandler.TYPE_NULL);
	}

}