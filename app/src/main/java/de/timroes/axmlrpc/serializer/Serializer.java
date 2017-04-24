package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 * A Serializer is responsible to serialize a specific type of data to
 * an xml tag and deserialize the content of this xml tag back to an object.
 * 
 * @author Tim Roes
 */
public interface Serializer {

	/**
	 * This method takes an object and returns a representation as a string
	 * containing the right xml type tag. The returning string must be useable
	 * within a value tag.
	 *
	 * @param object The object that should be serialized.
	 * @return An XmlElement representation of the object.
	 */
	public XmlElement serialize(Object object);

}