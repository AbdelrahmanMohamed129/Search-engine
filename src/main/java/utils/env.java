package utils;
import java.util.*;

public class env {
    
    /************ Database variables ************/
    /* Configs */

    public static final String DATABASE_NAME = "bingo-search-engine";
    public static final String DATABASE_HOST = "localhost";
    public static final int DATABASE_PORT = 27017;

    /* Collection */
    /*  Note that: there's only one document that contains webpages "forward index" 
     *  However, there's a database index (B+ tree) on the words in each document
     *  instead of the inverted index, since we only crawl 6k pages
    */
    public static final String COLLECTION_WEBPAGES = "webpages";
    public static final String COLLECTION_WORDS = "words";
    public static final String COLLECTION_STEMS = "stems";
    public static final String COLLECTION_SUGGESTIONS = "suggestions";

    /* Fields */
    public static final String FIELD_ID = "_id";
    public static final String FIELD_URL = "url";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_PAGE_DATA = "pageData";
    public static final String FIELD_TOTAL_WORDS = "words_count";
    public static final String FIELD_RANK = "rank";
    public static final String FIELD_OUTLINKS = "outlinks";
    public static final String FIELD_TERM_INDEX = "term-index";
    public static final String FIELD_TERM = "term";
    public static final String FIELD_URLS = "urls";
    public static final String FIELD_TERM_POSITIONS = "positions";
    public static final String FIELD_STEM_INDEX = "stem-index";
    public static final String FIELD_STEM_COUNT = "count";
    public static final String FIELD_STEM_SCORE = "score";
    public static final String FIELD_SUGGEST_QUERY = "query";

    public static final List<String> FIELDS_FOR_SEARCH_RESULTS = Arrays.asList(
            FIELD_ID,
            FIELD_URL,
            FIELD_TITLE,
            FIELD_PAGE_DATA
    );
    
    /* Stop Words */
    public static final String[] STOP_WORDS = {
        "a", "about", "above", "across", "after", "against", "amid", "among", "an", "and", "around", "as", "at", "before",
        "behind", "below", "beneath", "beside", "between", "beyond", "but", "by", "despite", "down", "during", "except",
        "following", "for", "from", "in", "inside", "into", "like", "near", "not", "of", "off", "on", "onto", "or", "out",
        "outside", "over", "past", "regarding", "since", "than", "through", "throughout", "till", "to", "toward", "under",
        "underneath", "until", "unto", "up", "upon", "with", "within", "without", "was", "what", "when", "where", "who",
        "will", "the", "www"
    };
    public static final Set<String> STOP_WORDS_SET = new HashSet<>(Arrays.asList(STOP_WORDS));

    public static final String[] ALLOWED_TAGS = {
        "body", "div", "p", "main", "article", "pre",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "b", "i", "em", "blockquote", "strong",
        "a", "span", "ol", "ul", "li"
    };
    public static final Set<String> ALLOWED_TAGS_SET = new HashSet<>(Arrays.asList(ALLOWED_TAGS));
    
    public static final Map<String, Integer> TAG_SCORES = new HashMap<String, Integer>() {
        {
            put("title", 50);
            put("h1", 35);
            put("h2", 30);
            put("h3", 25);
            put("strong", 15);
            put("em", 15);
            put("b", 15);
            put("h4", 7);
            put("h5", 5);
            put("h6", 4);
            put("i", 5);
        }
    };
}
