package de.timroes.axmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import de.timroes.axmlrpc.serializer.SerializerHandler;

/**
 * An XMLRPCClient is a client used to make XML-RPC (Extensible Markup Language
 * Remote Procedure Calls).
 * The specification of XMLRPC can be found at http://www.xmlrpc.com/spec.
 * You can use flags to extend the functionality of the client to some extras.
 * Further information on the flags can be found in the documentation of these.
 * For a documentation on how to use this class see also the README file delivered
 * with the source of this library.
 *
 * @author Tim Roes
 */
public class XMLRPCClient {

	/**
	 * Constants from the http protocol.
	 */
	static final String CONTENT_TYPE = "Content-Type";
	static final String TYPE_XML = "text/xml; charset=utf-8";
	static final String HOST = "Host";
	static final String CONTENT_LENGTH = "Content-Length";
	static final String HTTP_POST = "POST";

	/**
	 * XML elements to be used.
	 */
	static final String METHOD_RESPONSE = "methodResponse";
	static final String PARAMS = "params";
	static final String PARAM = "param";
	public static final String VALUE = "value";
	static final String FAULT = "fault";
	static final String METHOD_CALL = "methodCall";
	static final String METHOD_NAME = "methodName";
	static final String STRUCT_MEMBER = "member";

	/**
	 * No flags should be set.
	 */
	public static final int FLAGS_NONE = 0x0;

	/**
	 * The client should parse responses strict to specification.
	 * It will check if the given content-type is right.
	 * The method name in a call must only contain of A-Z, a-z, 0-9, _, ., :, /
	 * Normally this is not needed.
	 */
	public static final int FLAGS_STRICT = 0x01;

	/**
	 * The client will be able to handle 8 byte integer values (longs).
	 * The xml type tag &lt;i8&gt; will be used. This is not in the specification
	 * but some libraries and servers support this behaviour.
	 * If this isn't enabled you cannot recieve 8 byte integers and if you try to
	 * send a long the value must be within the 4byte integer range.
	 */
	public static final int FLAGS_8BYTE_INT = 0x02;

	/**
	 * The client will be able to send null values. A null value will be send
	 * as <nil/>. This extension is described under: http://ontosys.com/xml-rpc/extensions.php
	 */
	public static final int FLAGS_NIL = 0x08;

	/**
	 * With this flag enabled, the XML-RPC client will ignore the HTTP status
	 * code of the response from the server. According to specification the
	 * status code must be 200. This flag is only needed for the use with
	 * not standard compliant servers.
	 */
	public static final int FLAGS_IGNORE_STATUSCODE = 0x10;

	/**
	 * With this flag enabled, the client will forward the request, if
	 * the 301 or 302 HTTP status code has been received. If this flag has not
	 * been set, the client will throw an exception on these HTTP status codes.
	 */
	public static final int FLAGS_FORWARD = 0x20;

	/**
	 * With this flag enabled, a value with a missing type tag, will be parsed
	 * as a string element. This is just for incoming messages. Outgoing messages
	 * will still be generated according to specification.
	 */
	public static final int FLAGS_DEFAULT_TYPE_STRING = 0x100;

	/**
	 * With this flag enabled, the {@link XMLRPCClient} ignores all namespaces
	 * used within the response from the server.
	 */
	public static final int FLAGS_IGNORE_NAMESPACES = 0x200;

	/**
	 * With this flag enabled, the {@link XMLRPCClient} will use the system http
	 * proxy to connect to the XML-RPC server.
	 */
	public static final int FLAGS_USE_SYSTEM_PROXY = 0x400;

	/**
	 * This prevents the decoding of incoming strings, meaning &amp; and &lt;
	 * won't be decoded to the & sign and the "less then" sign. See
	 * {@link #FLAGS_NO_STRING_ENCODE} for the counterpart.
	 */
	public static final int FLAGS_NO_STRING_DECODE = 0x800;

	/**
	 * By default outgoing string values will be encoded according to specification.
	 * Meaning the & sign will be encoded to &amp; and the "less then" sign to &lt;.
	 * If you set this flag, the encoding won't be done for outgoing string values.
	 * See {@link #FLAGS_NO_STRING_ENCODE} for the counterpart.
	 */
	public static final int FLAGS_NO_STRING_ENCODE = 0x1000;

	/**
	 * This flag should be used if the server is an apache ws xmlrpc server.
	 * This will set some flags, so that the not standard conform behavior
	 * of the server will be ignored.
	 * This will enable the following flags: FLAGS_IGNORE_NAMESPACES, FLAGS_NIL,
	 * FLAGS_DEFAULT_TYPE_STRING
	 */
	public static final int FLAGS_APACHE_WS = FLAGS_IGNORE_NAMESPACES | FLAGS_NIL
			| FLAGS_DEFAULT_TYPE_STRING;

	private final int flags;

	private DefaultHttpClient httpclient;
	
	private String url;

	private Map<Long,Caller> backgroundCalls = new ConcurrentHashMap<Long, Caller>();

	private ResponseParser responseParser;

	/**
	 * Create a new XMLRPC client for the given URL.
	 * 
	 * @param httpclient The already-initialized Apache HttpClient to use for connection.
	 * @param url The URL to send the requests to.
	 * @param flags A combination of flags to be set.
	 */
	public XMLRPCClient(DefaultHttpClient httpclient, String url, int flags) {

		SerializerHandler.initialize(flags);

		this.httpclient = httpclient;
		this.url = url;
		this.flags = flags;
		
		// Create a parser for the http responses.
		responseParser = new ResponseParser();

	}

	/**
	 * Create a new XMLRPC client for the given url.
	 * No flags will be used.
	 *
	 * @param httpclient The already-initialized Apache HttpClient to use for connection.
	 * @param url The url to send the requests to.
	 */
	public XMLRPCClient(DefaultHttpClient httpclient, String url) {
		this(httpclient, url, FLAGS_NONE);
	}

	/**
	 * Call a remote procedure on the server. The method must be described by
	 * a method name. If the method requires parameters, this must be set.
	 * The type of the return object depends on the server. You should consult
	 * the server documentation and then cast the return value according to that.
	 * This method will block until the server returned a result (or an error occurred).
	 * Read the README file delivered with the source code of this library for more
	 * information.
	 *
	 * @param method A method name to call.
	 * @param params An array of parameters for the method.
	 * @return The result of the server.
	 * @throws XMLRPCException Will be thrown if an error occurred during the call.
	 */
	public Object call(String method, Object... params) throws XMLRPCException {
		try {
			return new Caller().call(method, params);
		} catch (CancelException e) {
			// Should not happen as this is not an async call
			throw new XMLRPCException("Background thread was explicitly cancelled, but not started asynchronously.");
		}
	}

	/**
	 * Asynchronously call a remote procedure on the server. The method must be
	 * described by a method  name. If the method requires parameters, this must
	 * be set. When the server returns a response the onResponse method is called
	 * on the listener. If the server returns an error the onServerError method
	 * is called on the listener. The onError method is called whenever something
	 * fails. This method returns immediately and returns an identifier for the
	 * request. All listener methods get this id as a parameter to distinguish between
	 * multiple requests.
	 *
	 * @param listener A listener, which will be notified about the server response or errors.
	 * @param methodName A method name to call on the server.
	 * @param params An array of parameters for the method.
	 * @return The id of the current request.
	 */
	public long callAsync(XMLRPCCallback listener, String methodName, Object... params) {
		long id = System.currentTimeMillis();
		new Caller(listener, id, methodName, params).start();
		return id;
	}

	/**
	 * Cancel a specific asynchronous call.
	 *
	 * @param id The id of the call as returned by the callAsync method.
	 */
	public void cancel(long id) {

		// Lookup the background call for the given id.
		Caller cancel = backgroundCalls.get(id);
		if(cancel == null) {
			return;
		}

		// Cancel the thread
		cancel.cancel();

		try {
			// Wait for the thread
			cancel.join();
		} catch (InterruptedException ex) {
			// Ignore this
		}

	}

	/**
	 * Create a call object from a given method string and parameters.
	 *
	 * @param method The method that should be called.
	 * @param params An array of parameters or null if no parameters needed.
	 * @return A call object.
	 */
	private Call createCall(String method, Object[] params) {

		if(isFlagSet(FLAGS_STRICT) && !method.matches("^[A-Za-z0-9\\._:/]*$")) {
			throw new XMLRPCRuntimeException("Method name must only contain A-Z a-z . : _ / ");
		}

		return new Call(method, params);

	}

	/**
	 * Checks whether a specific flag has been set.
	 *
	 * @param flag The flag to check for.
	 * @return Whether the flag has been set.
	 */
	private boolean isFlagSet(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	 * The Caller class is used to make asynchronous calls to the server.
	 * For synchronous calls the Thread function of this class isn't used.
	 */
	private class Caller extends Thread {

		private XMLRPCCallback listener;
		private long threadId;
		private String methodName;
		private Object[] params;

		HttpPost post = null;
		private volatile boolean canceled;

		/**
		 * Create a new Caller for asynchronous use.
		 *
		 * @param listener The listener to notice about the response or an error.
		 * @param threadId An id that will be send to the listener.
		 * @param methodName The method name to call.
		 * @param params The parameters of the call or null.
		 */
		public Caller(XMLRPCCallback listener, long threadId, String methodName, Object[] params) {
			this.listener = listener;
			this.threadId = threadId;
			this.methodName = methodName;
			this.params = params;
		}

		/**
		 * Create a new Caller for synchronous use.
		 * If the caller has been created with this constructor you cannot use the
		 * start method to start it as a thread. But you can call the call method
		 * on it for synchronous use.
		 */
		public Caller() { }

		/**
		 * The run method is invoked when the thread gets started.
		 * This will only work, if the Caller has been created with parameters.
		 * It execute the call method and notify the listener about the result.
		 */
		@Override
		public void run() {

			if(listener == null)
				return;

			try {
				backgroundCalls.put(threadId, this);
				Object o = this.call(methodName, params);
				listener.onResponse(threadId, o);
			} catch(XMLRPCServerException ex) {
				listener.onServerError(threadId, ex);
			} catch (XMLRPCException ex) {
				listener.onError(threadId, ex);
			} catch (CancelException e) {
			} finally {
				backgroundCalls.remove(threadId);
			}

		}

		/**
		 * Cancel this call. This will abort the network communication.
		 */
		public void cancel() {
			// Set the flag, that this thread has been canceled
			canceled = true;
			// Disconnect the connection to the server
			if (post != null)
				post.abort();
		}

		/**
		 * Call a remote procedure on the server. The method must be described by
		 * a method name. If the method requires parameters, this must be set.
		 * The type of the return object depends on the server. You should consult
		 * the server documentation and then cast the return value according to that.
		 * This method will block until the server returned a result (or an error occurred).
		 * Read the README file delivered with the source code of this library for more
		 * information.
		 *
		 * @param method A method name to call.
		 * @param params An array of parameters for the method.
		 * @return The result of the server.
		 * @throws XMLRPCException Will be thrown if an error occurred during the call.
		 * @throws CancelException WIll be thrown if the async execution is explicitly cancelled.
		 */
		public Object call(String methodName, Object[] params) throws XMLRPCException, CancelException {

			try {

				Call c = createCall(methodName, params);
				
				// Prepare POST request
				HttpPost post = new HttpPost(url);
				post.getParams().setParameter("http.protocol.handle-redirects",	false);
				post.setHeader(CONTENT_TYPE, TYPE_XML);
				StringEntity entity = new StringEntity(c.getXML(), HTTP.UTF_8);
				entity.setContentType(TYPE_XML);
				post.setEntity(entity);
				
				HttpResponse response = httpclient.execute(post);
				int statusCode = response.getStatusLine().getStatusCode();
				
				InputStream istream;

				// If status code was 401 or 403 throw exception or if appropriate
				// flag is set, ignore error code.
				if(statusCode == HttpURLConnection.HTTP_FORBIDDEN
						|| statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {

					if(isFlagSet(FLAGS_IGNORE_STATUSCODE)) {
						// getInputStream will fail if server returned above
						// error code, use getErrorStream instead
						istream = response.getEntity().getContent();
					} else {
						throw new XMLRPCException("Invalid status code '"
								+ statusCode + "' returned from server.", new UnauthorizdException(statusCode));
					}

				} else {
					istream = response.getEntity().getContent();
				}

				// If status code is 301 Moved Permanently or 302 Found ...
				if(statusCode == HttpURLConnection.HTTP_MOVED_PERM
						|| statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
					// ... do either a foward
					if(isFlagSet(FLAGS_FORWARD)) {
						boolean temporaryForward = (statusCode == HttpURLConnection.HTTP_MOVED_TEMP);

						// Get new location from header field.
						String newLocation = response.getFirstHeader("Location").getValue();
						// Try getting header in lower case, if no header has been found
						if(newLocation == null || newLocation.length() <= 0)
							newLocation = response.getFirstHeader("location").getValue();

						// Set new location, disconnect current connection and request to new location.
						String oldURL = url;
						url = newLocation;
						Object forwardedResult = call(methodName, params);

						// In case of temporary forward, restore original URL again for next call.
						if(temporaryForward) {
							url = oldURL;
						}

						return forwardedResult;

					} else {
						// ... or throw an exception
						throw new XMLRPCException("The server responded with a http 301 or 302 status "
								+ "code, but forwarding has not been enabled (FLAGS_FORWARD).");

					}
				}

				if(!isFlagSet(FLAGS_IGNORE_STATUSCODE)
					&& statusCode != HttpURLConnection.HTTP_OK) {
					throw new XMLRPCException("The status code of the http response must be 200.");
				}

				// Check for strict parameters
				if(isFlagSet(FLAGS_STRICT)) {
					if(!response.getFirstHeader("Content-Type").getValue().startsWith(TYPE_XML)) {
						throw new XMLRPCException("The Content-Type of the response must be text/xml.");
					}
				}

				return responseParser.parse(istream);

			} catch(SocketTimeoutException ex) {
				throw new XMLRPCTimeoutException("The XMLRPC call timed out.");
			} catch (IOException ex) {
				// If the thread has been canceled this exception will be thrown.
				// So only throw an exception if the thread hasnt been canceled
				// or if the thred has not been started in background.
				if(!canceled || threadId <= 0) {
					throw new XMLRPCException(ex);
				} else {
					throw new CancelException();
				}
			}

		}
		
	}
	
	public static class CancelException extends Exception {
		private static final long serialVersionUID = 9125122307255855136L;
	}

	public static class UnauthorizdException extends Exception {
		private static final long serialVersionUID = -3331056540713825039L;
		private int statusCode;
		public UnauthorizdException(int statusCode) { this.statusCode = statusCode; }
		public int getStatusCode() { return statusCode; }
	}
	
}
