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
package org.transdroid.daemon.adapters.deluge;

import androidx.annotation.NonNull;

import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.util.TlsSniSocketFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import se.dimovski.rencode.Rencode;

import static org.transdroid.daemon.adapters.deluge.DelugeCommon.RPC_METHOD_DAEMON_LOGIN;
import static org.transdroid.daemon.adapters.deluge.DelugeCommon.RPC_METHOD_INFO;

/**
 * A Deluge RPC API Client.
 */
class DelugeRpcClient implements Closeable {

    private static final int RESPONSE_TYPE_INDEX = 0;
    private static final int RESPONSE_RETURN_VALUE_INDEX = 2;
    private static final int RPC_ERROR = 2;
    private static final byte V2_PROTOCOL_VERSION = 1;
    private static final int V2_HEADER_SIZE = 5;
    private static AtomicInteger requestId = new AtomicInteger();
    private final boolean isVersion2;
    private Socket socket;

    DelugeRpcClient(boolean isVersion2) {
        this.isVersion2 = isVersion2;
    }

    void connect(DaemonSettings settings) throws DaemonException {
        try {
            socket = openSocket(settings);
            if (isVersion2) {
                sendRequest(RPC_METHOD_INFO);
            }
            if (settings.shouldUseAuthentication()) {
                sendRequest(RPC_METHOD_DAEMON_LOGIN, settings.getUsername(), settings.getPassword());
            }
        } catch (UnknownHostException e) {
            throw new DaemonException(ExceptionType.AuthenticationFailure, "Failed to sign in: " + e.getMessage());
        } catch (IOException e) {
            throw new DaemonException(ExceptionType.ConnectionError, "Failed to open socket: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @NonNull
    Object sendRequest(String method, Object... args) throws DaemonException {
        final byte[] requestBytes;
        try {
            HashMap<Object, Object> kwargs = new HashMap<>();
            if (isVersion2 && RPC_METHOD_DAEMON_LOGIN.equals(method)) {
                kwargs.put("client_version", "" + V2_PROTOCOL_VERSION);
            }
            requestBytes = compress(Rencode.encode(new Object[]{new Object[]{requestId.getAndIncrement(), method, args, kwargs}}));
        } catch (IOException e) {
            throw new DaemonException(ExceptionType.ConnectionError, "Failed to encode request: " + e.getMessage());
        }
        try {
            if (isVersion2) {
                socket.getOutputStream().write(
                        ByteBuffer.allocate(V2_HEADER_SIZE + requestBytes.length)
                                .put(V2_PROTOCOL_VERSION)
                                .putInt(requestBytes.length)
                                .put(requestBytes)
                                .array()
                );
            } else {
                socket.getOutputStream().write(requestBytes);
            }
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
        final InputStream in = socket.getInputStream();
        final InflaterInputStream inflater = new InflaterInputStream(in);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final byte[] buffer;
        if (isVersion2) {
            final byte[] header = new byte[V2_HEADER_SIZE];
            in.read(header, 0, V2_HEADER_SIZE);
            if (header[0] != V2_PROTOCOL_VERSION) {
                throw new DaemonException(ExceptionType.ConnectionError, "Unexpected protocol version: " + header[0]);
            }
            buffer = new byte[ByteBuffer.wrap(header).getInt(1)];
        } else {
            buffer = new byte[1024];
        }

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
        final List<?> response = (List<?>) responseObject;

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
    private Socket openSocket(DaemonSettings settings) throws IOException, DaemonException {
        if (!settings.getSsl()) {
            // Non-ssl connections
            throw new DaemonException(ExceptionType.ConnectionError, "Deluge RPC Adapter must have SSL enabled");
        }
        final TlsSniSocketFactory socketFactory;
        if (settings.getSslTrustKey() != null && settings.getSslTrustKey().length() != 0) {
            socketFactory = new TlsSniSocketFactory(settings.getSslTrustKey());
        } else if (settings.getSslTrustAll()) {
            socketFactory = new TlsSniSocketFactory(true);
        } else {
            socketFactory = new TlsSniSocketFactory();
        }
        return socketFactory.createSocket(null, settings.getAddress(), settings.getPort(), false);
    }

}
