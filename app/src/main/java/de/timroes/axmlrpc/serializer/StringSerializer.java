package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import org.w3c.dom.Element;

/**
 *
 * @author Tim Roes
 */
public class StringSerializer implements Serializer {

	private boolean encodeStrings;

	public StringSerializer(boolean encodeStrings) {
		this.encodeStrings = encodeStrings;
	}


	public XmlElement serialize(Object object) {
		String content = object.toString();
		if(encodeStrings) {
			content = content.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
		}
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_STRING, content);
	}

}