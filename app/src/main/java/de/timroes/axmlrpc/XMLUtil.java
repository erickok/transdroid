package de.timroes.axmlrpc;

import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 * This class provides some utility methods for the use with the Java DOM parser.
 *
 * @author Tim Roes
 */
public class XMLUtil {

	/**
	 * Creates an xml tag with a given type and content.
	 *
	 * @param type The type of the xml tag. What will be filled in the <..>.
	 * @param content The content of the tag.
	 * @return The xml tag with its content as a string.
	 */
	public static XmlElement makeXmlTag(String type, String content) {
		XmlElement xml = new XmlElement(type);
		xml.setContent(content);
		return xml;
	}

}