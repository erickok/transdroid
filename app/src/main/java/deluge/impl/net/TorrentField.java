package deluge.impl.net;

public enum TorrentField
{
    ACTIVE_TIME("active_time"), ALL_TIME_DOWNLOAD("all_time_download"), COMPACT("compact"), DISTRIBUTED_COPIES(
            "distributed_copies"), DOWNLOAD_PAYLOAD_RATE("download_payload_rate"), FILE_PRIORITIES("file_priorities"), HASH(
            "hash"), IS_AUTO_MANAGED("is_auto_managed"), IS_FINISHED("is_finished"), MAX_CONNECTIONS("max_connections"), MAX_DOWNLOAD_SPEED(
            "max_download_speed"), MAX_UPLOAD_SLOTS("max_upload_slots"), MAX_UPLOAD_SPEED("max_upload_speed"), MESSAGE(
            "message"), MOVE_ON_COMPLETED_PATH("move_on_completed_path"), MOVE_ON_COMPLETED("move_on_completed"), MOVE_COMPLETED_PATH(
            "move_completed_path"), MOVE_COMPLETED("move_completed"), NEXT_ANNOUNCE("next_announce"), NUM_PEERS(
            "num_peers"), NUM_SEEDS("num_seeds"), PAUSED("paused"), PRIORITIZE_FIRST_LAST("prioritize_first_last"), PROGRESS(
            "progress"), REMOVE_AT_RATIO("remove_at_ratio"), SAVE_PATH("save_path"), SEEDING_TIME("seeding_time"), SEEDS_PEERS_RATIO(
            "seeds_peers_ratio"), SEED_RANK("seed_rank"), STATE("state"), STOP_AT_RATIO("stop_at_ratio"), STOP_RATIO(
            "stop_ratio"), TIME_ADDED("time_added"), TOTAL_DONE("total_done"), TOTAL_PAYLOAD_DOWNLOAD(
            "total_payload_download"), TOTAL_PAYLOAD_UPLOAD("total_payload_upload"), TOTAL_PEERS("total_peers"), TOTAL_SEEDS(
            "total_seeds"), TOTAL_UPLOADED("total_uploaded"), TOTAL_WANTED("total_wanted"), TRACKER("tracker"), TRACKERS(
            "trackers"), TRACKER_STATUS("tracker_status"), UPLOAD_PAYLOAD_RATE("upload_payload_rate"), COMMENT(
            "comment"), ETA("eta"), FILE_PROGRESS("file_progress"), FILES("files"), IS_SEED("is_seed"), NAME("name"), NUM_FILES(
            "num_files"), NUM_PIECES("num_pieces"), PEERS("peers"), PIECE_LENGTH("piece_length"), PRIVATE("private"), QUEUE(
            "queue"), RATIO("ratio"), TOTAL_SIZE("total_size"), TRACKER_HOST("tracker_host"), LABEL("label");

    private final String value;

    TorrentField(String str)
    {
        this.value = str;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}
