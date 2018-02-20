package org.transdroid.core.gui.remoterss.data;

import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.DaemonException;

import java.util.ArrayList;

/**
 * Interface for daemon adapters if they support remote RSS management.
 *
 * @author Twig
 */
public interface RemoteRssSupplier {
    ArrayList<RemoteRssChannel> getRemoteRssChannels(Log log) throws DaemonException;

    void downloadRemoteRssItem(Log log, RemoteRssItem rssItem, RemoteRssChannel rssChannel) throws DaemonException;
}