package org.transdroid.daemon.Deluge;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import deluge.impl.net.AcceptAllTrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import se.dimovski.rencode.Rencode;

/**
 * A Deluge RPC API Client.
 */
public class DelugeRpcClient {
  // TODO: Extract constants to a common file used by both Adapters.
  private static final String RPC_METHOD_LOGIN = "daemon.login";
  private static final int RPC_ERROR = 2;

  private final String address;
  private final int port;
  private final String username;
  private final String password;

  public DelugeRpcClient(String address, int port, String username, String password) {
    this.address = address;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  @NonNull
  Object sendRequest(String method, Object... args) throws DaemonException {
    final List<Object> results = sendRequests(new Request(method, args));
    return results.get(0);
  }

  @NonNull
  private List<Object> sendRequests(Request... requests) throws DaemonException {
    final List<Object> requestObjects = new ArrayList<>();

    int loginRequestId = -1;
    if (!TextUtils.isEmpty(username)) {
      final Request loginRequest = new Request(RPC_METHOD_LOGIN, username, password);
      requestObjects.add(loginRequest.toObject());
      loginRequestId = loginRequest.getId();
    }
    for (Request request : requests) {
      requestObjects.add(request.toObject());
    }
    final byte[] requestBytes;
    try {
      requestBytes = compress(Rencode.encode(requestObjects));
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to encode request: " + e.getMessage());
    }

    final Socket socket = openSocket();
    try {
      socket.getOutputStream().write(requestBytes);
      final SortedMap<Integer, Object> returnValuesMap = new TreeMap<>();
      for (int i = 0, n = requestObjects.size(); i < n; i++) {
        final Response response = readResponse(socket.getInputStream());
        final int responseId = response.getId();
        if (response.getType() == RPC_ERROR) {
          if (responseId == loginRequestId) {
            throw new DaemonException(ExceptionType.AuthenticationFailure, response.getReturnValue()
                .toString());
          } else {
            throw new DaemonException(ExceptionType.UnexpectedResponse, response.getReturnValue().toString());
          }
        }
        returnValuesMap.put(response.getId(), response.getReturnValue());
      }
      if (returnValuesMap.size() != requestObjects.size()) {
        throw new DaemonException(ExceptionType.UnexpectedResponse, returnValuesMap.toString());

      }
      final List<Object> returnValues = new ArrayList<>();
      for (Request request : requests) {
        final int requestId = request.getId();
        final Object returnValue = returnValuesMap.get(requestId);
        if (returnValue == null) {
          throw new DaemonException(ExceptionType.UnexpectedResponse, "No result for request id " + requestId);
        }
        returnValues.add(returnValue);
      }
      return returnValues;
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError, e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @NonNull
  private byte[] compress(byte[] bytes) throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try {
      DeflaterOutputStream deltaterOut = new DeflaterOutputStream(byteOut);
      try {
        deltaterOut.write(bytes);
        deltaterOut.finish();
        return byteOut.toByteArray();
      } finally {
        deltaterOut.close();
      }
    } finally {
      byteOut.close();
    }
  }

  @NonNull
  private Response readResponse(InputStream in) throws DaemonException, IOException {
    final InflaterInputStream inflater = new InflaterInputStream(in);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buffer = new byte[1024];
    while (inflater.available() > 0) {
      final int n = inflater.read(buffer);
      if (n > 0) {
        out.write(buffer, 0, n);
      }
    }
    final byte[] bytes = out.toByteArray();
    return new Response(Rencode.decode(bytes));
  }

  @NonNull
  private Socket openSocket() throws DaemonException {
    try {
      final TrustManager[] trustAllCerts = new TrustManager[]{new AcceptAllTrustManager()};
      final SSLContext sslContext = SSLContext.getInstance("TLSv1");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

      return sslContext.getSocketFactory().createSocket(address, port);
    } catch (NoSuchAlgorithmException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (UnknownHostException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (KeyManagementException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    }
  }

}
