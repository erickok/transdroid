package org.transdroid.daemon.Utorrent.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by twig on 29/05/2016.
 */
public class UTorrentRssFeed implements Parcelable {
    /**
     * [
     *  1, // id?
     *  true, // enabled
     *  false, // false = use custom alias
     *  false, // ?
     *  false, // ?
     *  0, // ?
     *  "Castle|http:\/\/showrss.info\/show\/53.rss", // Custom alias|url or url
     *  1464520989, // las updated?
 *      [ // Files
     *      [
     *          "Castle 2009 Crossfire", // name?
     *          "Castle (2009) 8x22 Crossfire", // Title
     *          // link
     *          "magnet:?xt=urn:btih:58E032D0B2E595393828FAED47224B3A2C9CCFE6&dn=Castle+2009+S08E22+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",
     *          0,
     *          0,
     *          1463455205, // timestamp
     *          8, // season
     *          22, // episode
     *          0,
     *          1,
     *          false,
     *          false
     *      ],
     *      [
     *          "Castle 2009 Crossfire",
     *          "Castle (2009) 8x22 Crossfire 720p",
     *          "magnet:?xt=urn:btih:6E7764AA9CB23A84914FDB1206A2CD7887CCD622&dn=Castle+2009+S08E22+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",
     *          12,
     *          0,
     *          1463455205,
     *          8,
     *          22,
     *          0,
     *          1,
     *          false,
     *          false
     *      ],
     *      ["Castle 2009 Hell to Pay","Castle (2009) 8x21 Hell to Pay 720p","magnet:?xt=urn:btih:2436547E7A13201C9A3E9409C7D3112C8DF2560C&dn=Castle+2009+S08E21+720p+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1462762205,8,21,0,1,false,false],
     *      ["Castle 2009 Hell to Pay","Castle (2009) 8x21 Hell to Pay","magnet:?xt=urn:btih:329A154C7A4FA68F55409C9F0203C0B0F6FB5F43&dn=Castle+2009+S08E21+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1462762205,8,21,0,1,false,false],
     *      ["Castle 2009 Much Ado About Murder PROPER","Castle (2009) 8x20 Much Ado About Murder 720p PROPER","magnet:?xt=urn:btih:98A3CF3CC59EB71658964FCB709E572B1401E35E&dn=Castle+2009+S08E20+PROPER+720p+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1462189505,8,20,0,1,true,false],
     *      ["Castle 2009 Much Ado About Murder PROPER","Castle (2009) 8x20 Much Ado About Murder PROPER","magnet:?xt=urn:btih:C1E4F55526B298C9BC9F481F781777E1ACD88536&dn=Castle+2009+S08E20+PROPER+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1462189505,8,20,0,1,true,false],
     *      ["Castle 2009 Much Ado About Murder","Castle (2009) 8x20 Much Ado About Murder 720p","magnet:?xt=urn:btih:B80E9DA4DF7D88DC9BF62C8677233F6DB4E5E24E&dn=Castle+2009+S08E20+720p+HDTV+x264+FLEET&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1462164608,8,20,0,1,false,false],
     *      ["Castle 2009 Much Ado About Murder","Castle (2009) 8x20 Much Ado About Murder","magnet:?xt=urn:btih:C73F67FDFCDBCF36686F16C79EDDE663669C72CD&dn=Castle+2009+S08E20+HDTV+x264+FLEET&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1462164608,8,20,0,1,false,false],
     *      ["Castle 2009 Dead Again","Castle (2009) 8x19 Dead Again 720p","magnet:?xt=urn:btih:92FF43AF5BE73B8A79119C7C80CCCF3E5091D241&dn=Castle+2009+S08E19+720p+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1461552608,8,19,0,1,false,false],
     *      ["Castle 2009 Dead Again","Castle (2009) 8x19 Dead Again","magnet:?xt=urn:btih:EE3DB0A002EF01B655777D8E56AF7C6046B22455&dn=Castle+2009+S08E19+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1461552608,8,19,0,1,false,false],
     *      ["Castle 2009 Backstabber","Castle (2009) 8x18 Backstabber","magnet:?xt=urn:btih:C05B22BEE08D82C84B9A96728EE84FDB0E4286F5&dn=Castle+2009+S08E18+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1460951407,8,18,0,1,false,false],
     *      ["Castle 2009 Backstabber","Castle (2009) 8x18 Backstabber 720p","magnet:?xt=urn:btih:27FB8EC5F537267F8A5543D728150E278B8E4D35&dn=Castle+2009+S08E18+720p+HDTV+x264+2HD&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1460949608,8,18,0,1,false,false],
     *      ["Castle 2009 Death Wish","Castle (2009) 8x17 Death Wish","magnet:?xt=urn:btih:A772093AB91C9A87B1289DBFE7010B088A3FDCD4&dn=Castle+2009+S08E17+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1460438409,8,17,0,1,false,false],
     *      ["Castle 2009 Death Wish","Castle (2009) 8x17 Death Wish 720p","magnet:?xt=urn:btih:ECAE7F4DCCB015F3052907CB9EBF255B718B74B4&dn=Castle+2009+S08E17+720p+HDTV+x264+AVS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1460433007,8,17,0,1,false,false],
     *      ["Castle 2009 Heartbreaker","Castle (2009) 8x16 Heartbreaker","magnet:?xt=urn:btih:1523E75580E6D4EDB34EAE287D53018C365A25BD&dn=Castle+S08E16+WEB+DL+x264+RARBG&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1459863906,8,16,0,1,false,false],
     *      ["Castle 2009 Heartbreaker","Castle (2009) 8x16 Heartbreaker 720p","magnet:?xt=urn:btih:780628ABAADA1F7EEA98F4ED3FE971FCB2550D4E&dn=Castle+2009+S08E16+720p+HDTV+x264+SVA&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1459828206,8,16,0,1,false,false],
     *      ["Castle 2009 Fidelis Ad Mortem","Castle (2009) 8x15 Fidelis Ad Mortem","magnet:?xt=urn:btih:ECB8DF64844076F4EB18349569FEA48DE4B4F2F9&dn=Castle+2009+S08E15+WEB+DL+x264+FUM&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1458640207,8,15,0,1,false,false],
     *      ["Castle 2009 Fidelis Ad Mortem","Castle (2009) 8x15 Fidelis Ad Mortem 720p","magnet:?xt=urn:btih:829C270329F1527D5C1DF7AEE8D6E31028CB23A8&dn=Castle+2009+S08E15+720p+HDTV+x264+AVS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1458618610,8,15,0,1,false,false],
     *      ["Castle 2009 The G.D.S","Castle (2009) 8x14 The G.D.S.","magnet:?xt=urn:btih:4110431702205393AF8278E9018E4F2D9B3CB28E&dn=Castle+S08E14+WEB+DL+x264+RARBG&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1457459708,8,14,0,1,false,false],
     *      ["Castle 2009 The G.D.S","Castle (2009) 8x14 The G.D.S. 720p","magnet:?xt=urn:btih:B69272F3FC841B6BA37EAE3D2E7128B2ED846F50&dn=Castle+2009+S08E14+720p+HDTV+x264+AVS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1457412606,8,14,0,1,false,false],
     *      ["Castle 2009 And Justice For All","Castle (2009) 8x13 And Justice For All 720p","magnet:?xt=urn:btih:9715C7CE755DFE6D23633D5E8BA08A7FE2DE7347&dn=Castle+2009+S08E13+720p+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1456807809,8,13,0,1,false,false],
     *      ["Castle 2009 And Justice For All","Castle (2009) 8x13 And Justice For All","magnet:?xt=urn:btih:0638AC833ED9D2D72C880FF385651A6BA6CE28EE&dn=Castle+2009+S08E13+HDTV+x264+KILLERS&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1456807809,8,13,0,1,false,false],
     *      ["Castle 2009 The Blame Game","Castle (2009) 8x12 The Blame Game 720p","magnet:?xt=urn:btih:E49F11DC34E7ADC87BBF798C6BB45D49C8475466&dn=Castle+2009+S08E12+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1456195816,8,12,0,1,false,false],
     *      ["Castle 2009 The Blame Game","Castle (2009) 8x12 The Blame Game","magnet:?xt=urn:btih:A9417E1BDD7887C6DB18D06DB37DC48567766D04&dn=Castle+2009+S08E12+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1456194022,8,12,0,1,false,false],
     *      ["Castle 2009 Dead Red","Castle (2009) 8x11 Dead Red 720p","magnet:?xt=urn:btih:94DD4AC8826A59814887E780D38F6D0FCD8590E3&dn=Castle+2009+S08E11+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1455591012,8,11,0,1,false,false],
     *      ["Castle 2009 Dead Red","Castle (2009) 8x11 Dead Red","magnet:?xt=urn:btih:E44F825E3D3B6BE3471A833185B04DCA74A3674D&dn=Castle+2009+S08E11+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1455589221,8,11,0,1,false,false],
     *      ["Castle 2009 Witness for the Prosecution","Castle (2009) 8x10 Witness for the Prosecution 720p","magnet:?xt=urn:btih:6E0F4DA3529B63BE75AECDC9EA1248305A8AD9D7&dn=Castle+2009+S08E10+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1455515410,8,10,0,1,false,false],
     *      ["Castle 2009 Witness for the Prosecution","Castle (2009) 8x10 Witness for the Prosecution","magnet:?xt=urn:btih:8DD8E60B78C1A6703727FD54055C1DA4AD422222&dn=Castle+2009+S08E10+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1455513610,8,10,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x09","magnet:?xt=urn:btih:2F5BDC5958C2B7BCBC568D76F1487E68A95B74D3&dn=Castle+2009+S08E09+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1454991609,8,9,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x09 720p","magnet:?xt=urn:btih:E21DF496B8F838C37C2F191A623D6EA3315190BD&dn=Castle+2009+S08E09+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1454991609,8,9,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x08 720p","magnet:?xt=urn:btih:96A21B41A80043280AA2241C4530AF529655EB8D&dn=Castle+2009+S08E08+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1448337002,8,8,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x08","magnet:?xt=urn:btih:4E7BBE21D8B18DC09A9647038DC3254D7E85864C&dn=Castle+2009+S08E08+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1448334902,8,8,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x07","magnet:?xt=urn:btih:428208294D380ABDAA2CA9CD4DC0B1CC6CBD2CC8&dn=Castle+2009+S08E07+HDTV+x264+LOL&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",0,0,1447728601,8,7,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x07 720p","magnet:?xt=urn:btih:3CDBF2ED5C14F1E514772D6541EC9D087F0AE3F3&dn=Castle+2009+S08E07+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1447728601,8,7,0,1,false,false],
     *      ["Castle 2009","Castle (2009) 8x06 720p","magnet:?xt=urn:btih:BD99B76B9B3E9BC2401CBAB9100CB1A8CB0F9813&dn=Castle+2009+S08E06+720p+HDTV+X264+DIMENSION&tr=udp:\/\/tracker.coppersurfer.tk:6969\/announce&tr=udp:\/\/tracker.leechers-paradise.org:6969&tr=udp:\/\/open.demonii.com:1337",12,0,1447131002,8,6,0,1,false,false]
     *  ]
     * ]
     */


    public int feedID;
    boolean enabled;
    boolean isCustomAlias;
    public String feedAlias;
    public String feedURL;
    public long lastUpdated;
    public List<RemoteRssFile> files;

    public UTorrentRssFeed(JSONArray json) throws JSONException {
        Log.e("UTorrentRssFeedItem", "input");

        feedID = json.getInt(0);
        enabled = json.getBoolean(1);
        isCustomAlias = !json.getBoolean(2);
        feedURL = json.getString(6);
        lastUpdated = json.getLong(7);

        if (isCustomAlias) {
            feedAlias = feedURL.split("\\|")[0];
            feedURL = feedURL.split("\\|")[1];
        }
        else {
            feedAlias = feedURL;
        }

        files = new ArrayList<>();

        JSONArray filesJson = json.getJSONArray(8);
        RemoteRssFile file;

        for (int i = 0; i < filesJson.length(); i++) {
            file = new RemoteRssFile(filesJson.getJSONArray(i));
            file.feedLabel = feedAlias;
            files.add(file);
        }
    }

    public UTorrentRssFeed(Parcel in) {
        feedID = in.readInt();
        enabled = (in.readByte() == 1);
        isCustomAlias = (in.readByte() == 1);
        feedAlias = in.readString();
        feedURL = in.readString();
        lastUpdated = in.readLong();

        files = new ArrayList<>();
        in.readList(files, RemoteRssFile.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(feedID);
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeByte((byte) (isCustomAlias ? 1 : 0));
        dest.writeString(feedAlias);
        dest.writeString(feedURL);
        dest.writeLong(lastUpdated);
        dest.writeList(files);
    }

    public static final Parcelable.Creator<UTorrentRssFeed> CREATOR = new Parcelable.Creator<UTorrentRssFeed>() {
   		public UTorrentRssFeed createFromParcel(Parcel in) {
            return new UTorrentRssFeed(in);
        }

   		public UTorrentRssFeed[] newArray(int size) {
   			return new UTorrentRssFeed[size];
   		}
   	};
}
