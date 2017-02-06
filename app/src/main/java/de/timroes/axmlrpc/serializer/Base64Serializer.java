package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import de.timroes.base64.Base64;
import org.w3c.dom.Element;

/**
 *
 * @author Tim Roes
 */
public class Base64Serializer implements Serializer {

	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_BASE64,
				Base64.encode((Byte[])object));
	}

}