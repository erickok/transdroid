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
package org.transdroid.daemon.Deluge;

import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_DAEMON_LOGIN;

import android.support.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.util.IgnoreSSLTrustManager;
import se.dimovski.rencode.Rencode;

/**
 * A Deluge RPC API Client.
 */
class DelugeRpcClient implements Closeable {

  private static final int RESPONSE_TYPE_INDEX = 0;
  private static final int RESPONSE_RETURN_VALUE_INDEX = 2;
  private static final int RPC_ERROR = 2;

  private Socket socket;
  private static AtomicInteger requestId = new AtomicInteger();

  void connect(String address, int port, String username, String paswword) throws DaemonException {
    try {
      socket = openSocket(address, port);
      if (username != null) {
        sendRequest(RPC_METHOD_DAEMON_LOGIN, username, paswword);
      }
    } catch (NoSuchAlgorithmException e) {
      throw new DaemonException(ExceptionType.ConnectionError, "Failed to open socket: " + e.getMessage());
    } catch (UnknownHostException e) {
      throw new DaemonException(ExceptionType.AuthenticationFailure, "Failed to sign in: " + e.getMessage());
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError, "Failed to open socket: " + e.getMessage());
    } catch (KeyManagementException e) {
      throw new DaemonException(ExceptionType.ConnectionError, "Failed to open socket: " + e.getMessage());
    }
  }

  public void close() {
    try {
      socket.close();
    } catch (IOException e) {
      // ignore
    }
  }

  @NonNull
  Object sendRequest(String method, Object... args) throws DaemonException {
    final byte[] requestBytes;
    try {
      requestBytes = compress(
          Rencode.encode(new Object[]{new Object[]{requestId.getAndIncrement(), method, args, new HashMap<>()}}));
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError, "Failed to encode request: " + e.getMessage());
    }
    try {
      socket.getOutputStream().write(requestBytes);
      return readResponse();
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError, e.getMessage());
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
  private Object readResponse() throws DaemonException, IOException {
    final InflaterInputStream inflater = new InflaterInputStream(socket.getInputStream());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buffer = new byte[1024];
    while (inflater.available() > 0) {
      final int n = inflater.read(buffer);
      if (n > 0) {
        out.write(buffer, 0, n);
      }
    }
    final byte[] bytes = out.toByteArray();
    final Object responseObject = Rencode.decode(bytes);

    if (!(responseObject instanceof List)) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }
    final List response = (List) responseObject;

    if (response.size() < RESPONSE_RETURN_VALUE_INDEX + 1) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }

    if (!(response.get(RESPONSE_TYPE_INDEX) instanceof Number)) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }
    final int type = ((Number) (response.get(RESPONSE_TYPE_INDEX))).intValue();

    if (type == RPC_ERROR) {
      throw new DaemonException(ExceptionType.UnexpectedResponse, responseObject.toString());
    }

    return response.get(2);
  }

  @NonNull
  private Socket openSocket(String address, int port)
      throws NoSuchAlgorithmException, KeyManagementException, IOException {
    final TrustManager[] trustAllCerts = new TrustManager[]{new IgnoreSSLTrustManager()};
    final SSLContext sslContext = SSLContext.getInstance("TLSv1");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    return sslContext.getSocketFactory().createSocket(address, port);
  }

}
