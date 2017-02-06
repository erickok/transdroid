package de.timroes.axmlrpc.serializer;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCRuntimeException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Tim Roes
 */
public class ArraySerializer implements Serializer {

	private static final String ARRAY_DATA = "data";
	private static final String ARRAY_VALUE = "value";

	public XmlElement serialize(Object object) {

		Iterable<?> iter = (Iterable<?>)object;
		XmlElement array = new XmlElement(SerializerHandler.TYPE_ARRAY);
		XmlElement data = new XmlElement(ARRAY_DATA);
		array.addChildren(data);

		try {

			XmlElement e;
			for(Object obj : iter) {
				e = new XmlElement(ARRAY_VALUE);
				e.addChildren(SerializerHandler.getDefault().serialize(obj));
				data.addChildren(e);
			}

		} catch(XMLRPCException ex) {
			throw new XMLRPCRuntimeException(ex);
		}

		return array;

	}

}