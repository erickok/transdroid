package org.transdroid.connect.clients;

import org.transdroid.connect.model.Torrent;

import io.reactivex.Flowable;

public interface ClientSpec {

	Flowable<String> clientVersion();

	Flowable<Torrent> torrents();

}
