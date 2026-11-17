package org.transdroid.daemon.adapters.transmission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.GetStatsTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Checks that the Transmission adapter does not send every request without credentials first (#105) while still
 * negotiating non-Basic schemes such as Digest (#499).
 */
public class TransmissionAdapterAuthTest {

    private static final String SESSION_HEADER = "X-Transmission-Session-Id";
    private static final String SESSION_GET =
            "{\"result\":\"success\",\"arguments\":{\"rpc-version\":17,\"alt-speed-enabled\":false," +
                    "\"download-dir-free-space\":1}}";

    private MockWebServer server;
    private final List<String> authHeaders = Collections.synchronizedList(new ArrayList<String>());
    private final List<Integer> statuses = Collections.synchronizedList(new ArrayList<Integer>());

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private void serve(final String challenge, final boolean sessionHandshake) throws Exception {
        server.setDispatcher(new Dispatcher() {
            private boolean handshakeDone = false;

            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String auth = request.getHeader("Authorization");
                authHeaders.add(auth);
                MockResponse response;
                if (auth == null || !auth.toLowerCase().startsWith(challenge.split(" ")[0].toLowerCase())) {
                    response = new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", challenge);
                } else if (sessionHandshake && !handshakeDone && request.getHeader(SESSION_HEADER) == null) {
                    handshakeDone = true;
                    response = new MockResponse().setResponseCode(409).addHeader(SESSION_HEADER, "abc");
                } else {
                    response = new MockResponse().setResponseCode(200).setBody(SESSION_GET);
                }
                statuses.add(response.getStatus().contains("401") ? 401 : response.getStatus().contains("409") ? 409 : 200);
                return response;
            }
        });
        server.start();
    }

    private DaemonTaskResult runStats(boolean useAuth) {
        DaemonSettings settings = new DaemonSettings("test", Daemon.Transmission, server.getHostName(),
                server.getPort(), false, false, null, null, useAuth, "user", "pass", null, null, OS.Linux, null,
                null, null, 5, false, false, null, false);
        TransmissionAdapter adapter = new TransmissionAdapter(settings);
        return adapter.executeTask(mock(Log.class), GetStatsTask.create(adapter));
    }

    private int count401() {
        int n = 0;
        for (int s : statuses) {
            if (s == 401) n++;
        }
        return n;
    }

    @Test
    public void basicAuthIsSentPreemptivelyAfterFirstChallenge() throws Exception {
        serve("Basic realm=\"Transmission\"", true);
        // Adapters are re-created often by the app, so use a fresh one for each run
        for (int i = 0; i < 4; i++) {
            assertTrue(runStats(true).wasSuccessful());
        }
        assertEquals("Only the very first request may be challenged", 1, count401());
        assertNull(authHeaders.get(0));
        for (int i = 1; i < authHeaders.size(); i++) {
            assertTrue(authHeaders.get(i).startsWith("Basic "));
        }
    }

    @Test
    public void digestIsStillNegotiatedAndNeverGetsBasic() throws Exception {
        serve("Digest realm=\"Transmission\", nonce=\"abc123\", qop=\"auth\"", false);
        for (int i = 0; i < 3; i++) {
            assertTrue(runStats(true).wasSuccessful());
        }
        for (String auth : authHeaders) {
            assertTrue("Basic must never be sent to a Digest server", auth == null || auth.startsWith("Digest "));
        }
        // Digest keeps plain challenge/response: one 401 for every RPC request (2 per run: version + stats)
        assertEquals(6, count401());
    }

    @Test
    public void noCredentialsWhenAuthenticationDisabled() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(SESSION_GET));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(SESSION_GET));
        server.start();
        assertTrue(runStats(false).wasSuccessful());
        assertNull(server.takeRequest().getHeader("Authorization"));
        assertNull(server.takeRequest().getHeader("Authorization"));
    }

    @Test
    public void wrongPasswordStillReportsAuthenticationFailure() throws Exception {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(401).addHeader("WWW-Authenticate", "Basic realm=\"x\"");
            }
        });
        server.start();
        assertTrue(!runStats(true).wasSuccessful());
        assertTrue(!runStats(true).wasSuccessful());
    }
}
