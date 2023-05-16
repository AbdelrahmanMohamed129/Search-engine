package ranker;

import org.bson.types.ObjectId;
import indexer.Indexer;
import indexer.Stem;
import indexer.Webpage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URISyntaxException;

public class Ranker {

    // ################## MEMBER VARIABLES ################## //

    Indexer mIndexer;

    List<Webpage> mWebpages;
    List<String> queryWords;
    List<String> queryStems;

    long totalDocsCount;
    long wordsDocsCount[];
    long stemsDocsCount[];


    // ################## CONSTRUCTOR ################## //
     
    public Ranker(Indexer indexer, List<Webpage> Webpages, List<String> queryWords, List<String> queryStems) {
        mIndexer = indexer;

        mWebpages = Webpages;
        this.queryWords = queryWords;
        this.queryStems = queryStems;

        retrieveDocumentsCount();
    }


    // ################## PUBLIC METHODS ################## //

    
    // Ranks the web pages based on the search query and returns a paginated results

    public List<String> startRanking(int pageNumber) throws URISyntaxException {
        // For each page calculate its TF-IDF score
        for (Webpage Webpage : mWebpages) {
            Webpage.rank = calculatePageScore(Webpage);
        }

        // Sort Webpages
        mWebpages.sort((p1, p2) -> Double.compare(p2.rank, p1.rank));

        List<ObjectId> ret = new ArrayList<>();

        int idx = 10 * (pageNumber - 1);
        int cnt = Math.min(mWebpages.size() - idx, 10);

        while (cnt-- > 0) {
            ret.add(mWebpages.get(idx++)._id);
        }
        List<String> list = ret.stream()
            .map(ObjectId::toString)
            .collect(Collectors.toList());
        return list;
    }


    // ################## PRIVATE METHODS ################## //

    
    // Retrieves the all the web pages count containing the query word
    // also the total number of documents in the database.
     
    private void retrieveDocumentsCount() {
        // Get the total number of documents in the database
        totalDocsCount = mIndexer.documentsCount();

        // Get the total number of documents containing each of the search query words
        wordsDocsCount = new long[queryWords.size()];
        stemsDocsCount = new long[queryWords.size()];

        for (int i = 0; i < queryWords.size(); ++i) {
            wordsDocsCount[i] = mIndexer.documentCountForWord(queryWords.get(i));
            stemsDocsCount[i] = mIndexer.documentCountForStem(queryStems.get(i));
        }
    }

    
    // Calculates the given web page score rank based on the search query
    // The score is calculated as the sum of product of the web page TF and IDF
     
    private double calculatePageScore(Webpage Webpage) throws URISyntaxException {
        String hostURL = getHostName(Webpage.url);
        double levenshtein = 0.0;
        double isHostWebpage = (cleanURL(Webpage.url).equals(hostURL)) ? 1.0 : 0.0;

        double pageScore = 0.0; // TF-IDF score
        int foundWordsCount = 0;

        // For each word in the query filter words
        for (int i = 0; i < queryWords.size(); ++i) {
            String word = queryWords.get(i);
            String stem = queryStems.get(i);

            List<Integer> positions = Webpage.terms.get(word);
            Stem stemInfo = Webpage.stems.getOrDefault(stem, new Stem(0, 0));

            int wordCnt = (positions == null ? 0 : positions.size());
            int stemCnt = stemInfo.count;
            double TF, IDF, score = 0, wordScore = 0;

            // Exact word
            if (wordCnt > 0) {
                TF = wordCnt / (double) Webpage.totalWords;
                IDF = Math.log((double) totalDocsCount / wordsDocsCount[i]);

                score += TF * IDF;

                foundWordsCount++;
            }

            // Synonymous words
            if (stemCnt > 0) {
                TF = stemCnt / (double) Webpage.totalWords;
                IDF = Math.log((double) totalDocsCount / stemsDocsCount[i]);

                score += (TF * IDF) * 0.5;

                wordScore = (double) stemInfo.score / stemCnt;
            }

            // Add the effect of the normalized score of the word
            // The word score is related to its occurrences in the HTML
            pageScore += score * wordScore;

            // Levenshtein query word, url score
            levenshtein += (1.0 * hostURL.length() - compute_Levenshtein_distanceDP(word, hostURL)) / hostURL.length();
        }

        double res = (0.2 * pageScore * levenshtein * foundWordsCount) + (Webpage.rank * 100 * levenshtein) + (isHostWebpage) + foundWordsCount;
        return res;
    }


    // ################## UTILITY METHODS ################## //

    public static String getHostName(String urlStr) throws URISyntaxException {
        String ret = null;
        
        URI url = new URI(urlStr);
        ret = url.getHost();
    
        return ret;
    }

    // Removes the http or https or // from the url
    public static String cleanURL(String urlStr) {
        String ret = urlStr;
        ret = ret.replace("http://", "");
        ret = ret.replace("https://", "");
        ret = ret.replace("/", "");

        return  ret;
    }

    public static int compute_Levenshtein_distanceDP(String str1, String str2)
    {
        // A 2-D matrix to store previously calculated
        // answers of subproblems in order
        // to obtain the final
 
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
 
        for (int i = 0; i <= str1.length(); i++)
        {
            for (int j = 0; j <= str2.length(); j++) {
 
                // If str1 is empty, all characters of
                // str2 are inserted into str1, which is of
                // the only possible method of conversion
                // with minimum operations.
                if (i == 0) {
                    dp[i][j] = j;
                }
 
                // If str2 is empty, all characters of str1
                // are removed, which is the only possible
                //  method of conversion with minimum
                //  operations.
                else if (j == 0) {
                    dp[i][j] = i;
                }
                    // If last characters are same, ignore last char
                    // and recur for remaining string
                else if (str1.charAt(i - 1) == str2.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1];

                    // If last character are different, consider all
                    // possibilities and find minimum
                else {
                    dp[i][j] = 1 + min(dp[i][j - 1],  // Insert
                            dp[i - 1][j],  // Remove
                            dp[i - 1][j - 1]); // Replace
                }
            }
        }
 
        return dp[str1.length()][str2.length()];
    }

    public static int min(int x, int y, int z) {
        if (x <= y && x <= z) return x;
        if (y <= x && y <= z) return y;
        else return z;
    }

}