package Utilities;

public class Environment {
    
    /************ Database variables ************/
    /* Configs */

    public static final String DATABASE_NAME = "bingo-search-engine";
    public static final String DATABASE_HOST = "localhost";
    public static final int DATABASE_PORT = 27017;

    /* Collections */
    public static final String COLLECTION_WEBPAGES = "webpages";
    public static final String COLLECTION_TERMS = "terms";

    /* Fields */
    public static final String FIELD_ID = "_id";
    public static final String FIELD_URL = "url";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_TOTAL_WORDS_COUNT = "words_count";
    public static final String FIELD_RANK = "rank";
    public static final String FIELD_OUTLINKS = "outlinks";
    public static final String FIELD_TERM = "term";
    public static final String FIELD_TERM_COUNT = "count";
    public static final String FIELD_TERM_SCORE = "score";
    public static final String FIELD_TERM_POSITIONS = "positions";

    /* Stop Words */
    public static final String[] STOP_WORDS = {
        "i", "a", "about", "an", "are", "as", "at", "be", "by", "com", "for",
        "from", "how", "in", "is", "it", "of", "on", "or", "that", "the",
        "this", "to", "was", "what", "when", "where", "who", "will",
        "with", "the", "www", "can", "and"};
}
