package org.transdroid.core.gui.remoterss.data;

import java.util.ArrayList;

/**
 * Interface for daemon adapters if they support remote RSS management.
 *
 * @author Twig
 */
public interface RemoteRssSupplier {
    ArrayList<RemoteRssChannel> getRemoteRssChannels();
}
