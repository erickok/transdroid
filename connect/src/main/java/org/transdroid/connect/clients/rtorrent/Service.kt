package org.transdroid.connect.clients.rtorrent

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import nl.nl2312.xmlrpc.Nothing
import nl.nl2312.xmlrpc.XmlRpc
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface Service {

    @XmlRpc("system.client_version")
    @POST("{endpoint}")
    fun clientVersion(@Path("endpoint") endpoint: String?, @Body nothing: Nothing = Nothing.NOTHING): Single<String>

    @XmlRpc("d.multicall2")
    @POST("{endpoint}")
    fun torrents(@Path("endpoint") endpoint: String?, @Body vararg args: String): Flowable<Array<TorrentSpec>>

    @XmlRpc("d.start")
    @POST("{endpoint}")
    fun start(@Path("endpoint") endpoint: String?, @Body hash: String): Completable

    @XmlRpc("d.stop")
    @POST("{endpoint}")
    fun stop(@Path("endpoint") endpoint: String?, @Body hash: String): Completable

    @XmlRpc("load.start")
    @POST("{endpoint}")
    fun loadStart(@Path("endpoint") endpoint: String?, @Body vararg args: String): Completable

    @XmlRpc("load.raw_start")
    @POST("{endpoint}")
    fun loadRawStart(@Path("endpoint") endpoint: String?, @Body vararg args: Any): Completable

    @XmlRpc("network.xmlrpc.size_limit.set")
    @POST("{endpoint}")
    fun networkSizeLimitSet(@Path("endpoint") endpoint: String?, @Body vararg args: Any): Single<Int>

}
