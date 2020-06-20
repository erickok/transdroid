package de.timroes.axmlrpc;

import de.timroes.axmlrpc.serializer.SerializerHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * The ResponseParser parses the response of an XMLRPC server to an object.
 *
 * @author Tim Roes
 */
public class ResponseParser {

	private static final String FAULT_CODE = "faultCode";
	private static final String FAULT_STRING = "faultString";


	/**
	 * Deallocate Http Entity and close streams
	 */
	private static void consumeHttpEntity(InputStream response, HttpEntity entity) {
		// Ideally we should use EntityUtils.consume(), introduced in apache http utils 4.1 - not available in
		// Android yet
		if (entity != null) {
			try {
				entity.consumeContent();
			} catch (IOException e) {
				// ignore exception (could happen if Content-Length is wrong)
			}
		}

		if (response != null) {
			try {
				response.close();
			} catch (Exception e) {
				// ignore exception
			}
		}
	}

	/**
	 * The given InputStream must contain the xml response from an xmlrpc server.
	 * This method extract the content of it as an object.
	 *
	 * @param response The InputStream of the server response.
	 * @return The returned object.
	 * @throws XMLRPCException Will be thrown whenever something fails.
	 * @throws XMLRPCServerException Will be thrown, if the server returns an error.
	 */
	public Object parse(InputStream response, HttpEntity entity) throws XMLRPCException {
		try {
			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			pullParser.setInput(response, "UTF-8");

			pullParser.nextTag();
			pullParser.require(XmlPullParser.START_TAG, null, XMLRPCClient.METHOD_RESPONSE);

			pullParser.nextTag(); // either TAG_PARAMS (<params>) or TAG_FAULT (<fault>)
			String tag = pullParser.getName();
			if (tag.equals(XMLRPCClient.PARAMS)) {
				// normal response
				pullParser.nextTag(); // TAG_PARAM (<param>)
				pullParser.require(XmlPullParser.START_TAG, null, XMLRPCClient.PARAM);
				pullParser.nextTag(); // TAG_VALUE (<value>)
				// no parser.require() here since its called in XMLRPCSerializer.deserialize() below
				// deserialize result
				Object obj = SerializerHandler.deserialize(pullParser);
				consumeHttpEntity(response, entity);
				return obj;
			} else if (tag.equals(XMLRPCClient.FAULT)) {
				// fault response
				pullParser.nextTag(); // TAG_VALUE (<value>)
				Map<String, Object> map = (Map<String, Object>) SerializerHandler.deserialize(pullParser);
				consumeHttpEntity(response, entity);

				//Check that required tags are in the response
				if (!map.containsKey(FAULT_STRING) || !map.containsKey(FAULT_CODE)) {
					throw new XMLRPCException("Bad XMLRPC Fault response received - <faultCode> and/or <faultString> missing!");
				}
				throw new XMLRPCServerException((String) map.get(FAULT_STRING), (Integer) map.get(FAULT_CODE));
			} else {
				throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
			}

		} catch (XmlPullParserException ex) {
			consumeHttpEntity(response, entity);
			throw new XMLRPCException("Error parsing response.", ex);
		} catch(XMLRPCServerException e) {
			throw e;
		} catch (Exception ex) {
			consumeHttpEntity(response, entity);
			throw new XMLRPCException("Error getting result from server.", ex);
		}
	}
}
