package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCRuntimeException;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import java.util.Arrays;

/**
 *
 * @author Tim Roes
 */
public class ArraySerializer implements Serializer {

	private static final String ARRAY_DATA = "data";
	private static final String ARRAY_VALUE = "value";
	private final SerializerHandler serializerHandler;

	public ArraySerializer(SerializerHandler serializerHandler){
		this.serializerHandler = serializerHandler;
	}

	public XmlElement serialize(Object object) {

		Iterable<?> iter;
		if ( object instanceof Iterable<?>){
			iter = (Iterable<?>)object;
		} else {
			iter = Arrays.asList((Object[]) object);
		}
		XmlElement array = new XmlElement(SerializerHandler.TYPE_ARRAY);
		XmlElement data = new XmlElement(ARRAY_DATA);
		array.addChildren(data);

		try {

			XmlElement e;
			for(Object obj : iter) {
				e = new XmlElement(ARRAY_VALUE);
				e.addChildren(serializerHandler.serialize(obj));
				data.addChildren(e);
			}

		} catch(XMLRPCException ex) {
			throw new XMLRPCRuntimeException(ex);
		}

		return array;

	}

}
