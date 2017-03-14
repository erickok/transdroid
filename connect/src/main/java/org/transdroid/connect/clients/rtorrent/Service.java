package org.transdroid.connect.clients.rtorrent;

import io.reactivex.Flowable;
import nl.nl2312.xmlrpc.Nothing;
import nl.nl2312.xmlrpc.XmlRpc;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface Service {

	@XmlRpc("system.client_version")
	@POST("{endpoint}")
	Flowable<String> clientVersion(@Path("endpoint") String endpoint, @Body Nothing nothing);

	@XmlRpc("d.multicall2")
	@POST("{endpoint}")
	Flowable<TorrentSpec[]> torrents(@Path("endpoint") String endpoint, @Body String... args);

	@XmlRpc("d.start")
	@POST("{endpoint}")
	Flowable<Void> start(@Path("endpoint") String endpoint, @Body String hash);

	@XmlRpc("d.stop")
	@POST("{endpoint}")
	Flowable<Void> stop(@Path("endpoint") String endpoint, @Body String hash);

	@XmlRpc("load.start")
	@POST("{endpoint}")
	Flowable<Integer> loadStart(@Path("endpoint") String endpoint, @Body String... args);

}
