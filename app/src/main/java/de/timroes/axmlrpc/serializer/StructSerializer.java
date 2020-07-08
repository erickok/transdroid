package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCRuntimeException;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import java.util.Map;

/**
 *
 * @author Tim Roes
 */
public class StructSerializer implements Serializer {

	private static final String STRUCT_MEMBER = "member";
	private static final String STRUCT_NAME = "name";
	private static final String STRUCT_VALUE = "value";

	private final SerializerHandler serializerHandler;

	public StructSerializer(SerializerHandler serializerHandler) {
		this.serializerHandler = serializerHandler;
	}

	public XmlElement serialize(Object object) {

		XmlElement struct = new XmlElement(SerializerHandler.TYPE_STRUCT);

		try {

			XmlElement entry, name, value;

			// We can safely cast here, this Serializer should only be called when
			// the parameter is a map.
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)object;

			for(Map.Entry<String,Object> member : map.entrySet()) {
				entry = new XmlElement(STRUCT_MEMBER);
				name = new XmlElement(STRUCT_NAME);
				value = new XmlElement(STRUCT_VALUE);
				name.setContent(member.getKey());
				value.addChildren(serializerHandler.serialize(member.getValue()));
				entry.addChildren(name);
				entry.addChildren(value);
				struct.addChildren(entry);
			}

		} catch(XMLRPCException ex) {
			throw new XMLRPCRuntimeException(ex);
		}

		return struct;
	}

}
