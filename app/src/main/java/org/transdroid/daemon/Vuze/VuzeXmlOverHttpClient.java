/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.daemon.Vuze;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.base64.android.Base64;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.util.TlsSniSocketFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/**
 * Implements an XML-RPC-like client that build and parses XML following 
 * Vuze's XML over HTTP plug-in (which unfortunately is incompatible with
 * the default XML-RPC protocol).
 * 
 * The documentation can be found at http://azureus.sourceforge.net/plugin_details.php?plugin=xml_http_if&docu=1#1
 * and some stuff is at http://wiki.vuze.com/index.php/XML_over_HTTP
 * 
 * A lot of it is copied from the org.xmlrpc.android library's XMLRPCClient
 * as can be found at http://code.google.com/p/android-xmlrpc
 * 
 * @author erickok
 *
 */
public class VuzeXmlOverHttpClient {

	private final static String TAG_REQUEST = "REQUEST";
	private final static String TAG_OBJECT = "OBJECT";
	private final static String TAG_OBJECT_ID = "_object_id";
	private final static String TAG_METHOD = "METHOD";
	private final static String TAG_PARAMS = "PARAMS";
	private final static String TAG_ENTRY = "ENTRY";
	private final static String TAG_INDEX = "index";
	private final static String TAG_CONNECTION_ID = "CONNECTION_ID";
	private final static String TAG_REQUEST_ID = "REQUEST_ID";
	private final static String TAG_RESPONSE = "RESPONSE";
	private final static String TAG_ERROR = "ERROR";
	private final static String TAG_TORRENT = "torrent";
	private final static String TAG_STATS = "stats";
	private final static String TAG_ANNOUNCE = "announce_result";
	private final static String TAG_SCRAPE = "scrape_result";
	private final static String TAG_CACHED_PROPERTY_NAMES = "cached_property_names";
	
	private DefaultHttpClient client;
	private HttpPost postMethod;
	private Random random;
	private String username;
	private String password;

	/**
	 * XMLRPCClient constructor. Creates new instance based on server URI
	 * @param settings The server connection settings
	 * @param uri The URI of the XML RPC to connect to
	 * @throws DaemonException Thrown when settings are missing or conflicting
	 */
	public VuzeXmlOverHttpClient(DaemonSettings settings, URI uri) throws DaemonException {
		postMethod = new HttpPost(uri);
		postMethod.addHeader("Content-Type", "text/xml");

        // WARNING
        // I had to disable "Expect: 100-Continue" header since I had 
        // two second delay between sending http POST request and POST body 
        HttpParams httpParams = postMethod.getParams();
        HttpProtocolParams.setUseExpectContinue(httpParams, false);

        HttpConnectionParams.setConnectionTimeout(httpParams, settings.getTimeoutInMilliseconds());
        HttpConnectionParams.setSoTimeout(httpParams, settings.getTimeoutInMilliseconds()); 
        
        SchemeRegistry registry = new SchemeRegistry();
		SocketFactory httpsSocketFactory;
		if (settings.getSslTrustKey() != null) {
			httpsSocketFactory = new TlsSniSocketFactory(settings.getSslTrustKey());
		} else if (settings.getSslTrustAll()) {
			httpsSocketFactory = new TlsSniSocketFactory(true);
		} else {
			httpsSocketFactory = new TlsSniSocketFactory();
		}
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		registry.register(new Scheme("https", httpsSocketFactory, 443));
		
        client = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, registry), httpParams);
        if (settings.shouldUseAuthentication()) {
            if (settings.getUsername() == null || settings.getPassword() == null) {
                    throw new DaemonException(DaemonException.ExceptionType.AuthenticationFailure, "No username or password set, while authentication was enabled.");
            } else {
            	username = settings.getUsername();
            	password = settings.getPassword();
                client.getCredentialsProvider().setCredentials(
                        new AuthScope(postMethod.getURI().getHost(), postMethod.getURI().getPort(), AuthScope.ANY_REALM),
                        new UsernamePasswordCredentials(username, password));
            }
        }
        
		random = new Random();
		random.nextInt();
	}
	
	/**
	 * Convenience constructor. Creates new instance based on server String address
	 * @param settings The server connection settings
	 * @param url The URL of the XML RPC to connect to
	 * @throws DaemonException Thrown when settings are missing or conflicting
	 */
	public VuzeXmlOverHttpClient(DaemonSettings settings, String url) throws DaemonException {
		this(settings, URI.create(url));
	}
	
	protected Map<String, Object> callXMLRPC(Long object, String method, Object[] params, Long connectionID, boolean paramsAreVuzeObjects) throws DaemonException {
		try {
			
			// prepare POST body
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter bodyWriter = new StringWriter();
			serializer.setOutput(bodyWriter);
			serializer.startDocument(null, null);
			serializer.startTag(null, TAG_REQUEST);
			
			// set object
			if (object != null) {
				serializer.startTag(null, TAG_OBJECT).startTag(null, TAG_OBJECT_ID)
					.text(object.toString()).endTag(null, TAG_OBJECT_ID).endTag(null, TAG_OBJECT);
			}
			
			// set method
			serializer.startTag(null, TAG_METHOD).text(method).endTag(null, TAG_METHOD);
			if (params != null && params.length != 0) {
				// set method params
				serializer.startTag(null, TAG_PARAMS);
				Integer entryIndex = 0;
				for (Object param : params) {
					serializer.startTag(null, TAG_ENTRY).attribute(null, TAG_INDEX, entryIndex.toString());
					if (paramsAreVuzeObjects) {
						serializer.startTag(null, TAG_OBJECT).startTag(null, TAG_OBJECT_ID);
					}
					serializer.text(serialize(param));
					if (paramsAreVuzeObjects) {
						serializer.endTag(null, TAG_OBJECT_ID).endTag(null, TAG_OBJECT);
					}
					serializer.endTag(null, TAG_ENTRY);
					entryIndex++;
				}
				serializer.endTag(null, TAG_PARAMS);
			}
			
			// set connection id
			if (connectionID != null) {
				serializer.startTag(null, TAG_CONNECTION_ID).text(connectionID.toString()).endTag(null, TAG_CONNECTION_ID);
			}
			// set request id, which for this purpose is always a random number
			Integer randomRequestID = new Integer(random.nextInt());
			serializer.startTag(null, TAG_REQUEST_ID).text(randomRequestID.toString()).endTag(null, TAG_REQUEST_ID);

			serializer.endTag(null, TAG_REQUEST);
			serializer.endDocument();
			
			// set POST body
			HttpEntity entity = new StringEntity(bodyWriter.toString());
			postMethod.setEntity(entity);
			
			// Force preemptive authentication
			// This makes sure there is an 'Authentication: ' header being send before trying and failing and retrying 
			// by the basic authentication mechanism of DefaultHttpClient
			postMethod.addHeader("Authorization", "Basic " + Base64.encodeBytes((username + ":" + password).getBytes()));
			
			// execute HTTP POST request
			HttpResponse response = client.execute(postMethod);

			// check status code
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
				throw new DaemonException(ExceptionType.AuthenticationFailure, "HTTP " + HttpStatus.SC_UNAUTHORIZED + " response (so no user or password or incorrect ones)");
			} else if (statusCode != HttpStatus.SC_OK) {
				throw new DaemonException(ExceptionType.ConnectionError, "HTTP status code: " + statusCode + " != " + HttpStatus.SC_OK);
			}

			// parse response stuff
			//
			// setup pull parser
			XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
			entity = response.getEntity();
			//String temp = HttpHelper.ConvertStreamToString(entity.getContent());			
			//Reader reader = new StringReader(temp);
			Reader reader = new InputStreamReader(entity.getContent());
			pullParser.setInput(reader);
			
			// lets start pulling...
			pullParser.nextTag();
			pullParser.require(XmlPullParser.START_TAG, null, TAG_RESPONSE);
			
			// build list of returned values
			int next = pullParser.nextTag(); // skip to first start tag in list
			String name = pullParser.getName(); // get name of the first start tag
			
			// Empty response?
			if (next == XmlPullParser.END_TAG && name.equals(TAG_RESPONSE)) {
				
				return null;
				
			} else if (name.equals(TAG_ERROR)) {
				
				// Error
				String errorText = pullParser.nextText(); // the value of the ERROR
				entity.consumeContent();
				throw new DaemonException(ExceptionType.ConnectionError, errorText);
				
			} else {
				
				// Consume a list of ENTRYs?
				if (name.equals(TAG_ENTRY)) {

					Map<String, Object> entries = new HashMap<String, Object>();
					for (int i = 0; name.equals(TAG_ENTRY); i++) {
						entries.put(TAG_ENTRY + i, consumeEntry(pullParser));
						name = pullParser.getName();
					}
					entity.consumeContent();
					return entries;
					
				} else {
					
					// Only a single object was returned, not an entry listing
					return consumeObject(pullParser);
				}
			
			}
			
		} catch (IOException e) {
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		} catch (XmlPullParserException e) {
			throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
		}
	}

	private Map<String, Object> consumeEntry(XmlPullParser pullParser) throws XmlPullParserException, IOException {

		int next = pullParser.nextTag();
		String name = pullParser.getName();
		
		// Consume the ENTRY objects
		Map<String, Object> returnValues = new HashMap<String, Object>();
		while (next == XmlPullParser.START_TAG) {
			
			if (name.equals(TAG_TORRENT) || name.equals(TAG_ANNOUNCE) || name.equals(TAG_SCRAPE) || name.equals(TAG_STATS)) {
				// One of the objects contained inside an entry
				pullParser.nextTag();
				returnValues.put(name, consumeObject(pullParser));
			} else {
				// An object text inside this entry (such as _connectionid)
				returnValues.put(name, deserialize(pullParser.nextText()));
			}
			next = pullParser.nextTag(); // skip to next start tag
			name = pullParser.getName(); // get name of the new start tag
			
		}
		
		// Consume ENTRY ending
		pullParser.nextTag();
		
		return returnValues;
		
	}

	private Map<String, Object> consumeObject(XmlPullParser pullParser) throws XmlPullParserException, IOException {

		int next = XmlPullParser.START_TAG;
		String name = pullParser.getName();
		
		// Consume bottom-level (contains no objects of its own) object
		Map<String, Object> returnValues = new HashMap<String, Object>();
		while (next == XmlPullParser.START_TAG && !(name.equals(TAG_CACHED_PROPERTY_NAMES))) {

			if (name.equals(TAG_TORRENT) || name.equals(TAG_ANNOUNCE) || name.equals(TAG_SCRAPE) || name.equals(TAG_STATS)) {
				// One of the objects contained inside an object
				pullParser.nextTag();
				returnValues.put(name, consumeObject(pullParser));
			} else {
				// An object text inside this entry (such as _connectionid)
				returnValues.put(name, deserialize(pullParser.nextText()));
			}
			next = pullParser.nextTag(); // skip to next start tag
			name = pullParser.getName(); // get name of the new start tag
			
		}
		
		return returnValues;
		
	}

	private String serialize(Object value) {
		return value.toString();
	}

	static Object deserialize(String rawText) {

		// Null?
		if (rawText == null || rawText.equals("null")) {
			return null;
		}

		/* For now cast all integers as Long; this prevents casting problems later on when 
		 * we know it's a long but the value was small so it is casted to an Integer here
		// Integer?
		try {
			Integer integernum = Integer.parseInt(rawText);
			return integernum;
		} catch (NumberFormatException e) {
			// Just continue trying the next type
		}*/
		
		// Long?
		try {
			Long longnum = Long.parseLong(rawText);
			return longnum;
		} catch (NumberFormatException e) {
			// Just continue trying the next type
		}
		
		// Double?
		try {
			Double doublenum = Double.parseDouble(rawText);
			return doublenum;
		} catch (NumberFormatException e) {
			// Just continue trying the next type
		}
		
		// String otherwise
		return rawText;
	}
	
}
