package server;

import org.bson.Document;
import org.bson.types.ObjectId;
import indexer.Indexer;
import indexer.Webpage;
import indexer.WebpageProcessor;
import ranker.Ranker;
import utils.env;
import utils.utilFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class QueryProcessor {

    // ################## MEMBER VARIABLES ################## //
    private Indexer mIndexer;

    private int paginationNo;
    private boolean isPhraseSearch;
    private String originalQuery;
    private String query;
    private List<String> queryWords;
    private List<String> queryStems;

    private int totalResultsCount;
    private List<ObjectId> rankedIds;
    private List<Webpage> results;
    private WebpageProcessor webProcessor;


    // ################## CONSTRUCTOR ################## //
    public QueryProcessor(Indexer indexer, String query, String pageNumber) throws Exception {
        mIndexer = indexer;
        parseQuery(query, pageNumber);
        rankResults();
    }


    // ################## PUBLIC METHODS ################## //

    // Returns the results in JSON format
    public String getJsonResult() {
        List<Document> pagesDocuments = new ArrayList<>();

        Snippetly mSnippetly = new Snippetly();
        // System.out.println("HERE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        // System.out.println(rankedIds);
        // System.out.println(results);
        for (ObjectId id : rankedIds) {
            for (Webpage Webpage : results) {
                if (!Webpage._id.equals(id)) continue;

                String snippet = mSnippetly.extractWebPageSnippet(Webpage.pageData, originalQuery);
                // System.out.println("HERE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                // System.out.println(Webpage.title);
                // System.out.println(Webpage.url);
                // System.out.println(snippet);
                Document doc = new Document()
                        .append("title", Webpage.title)
                        .append("url", Webpage.url)
                        .append("snippet", snippet);

                pagesDocuments.add(doc);

                break;
            }
        }

        int pagesCount = ((totalResultsCount + 10 - 1) / 10);
        
        // System.out.println(pagesDocuments);
        Document paginationDocument = new Document()
                .append("pages_count", pagesCount)
                .append("current_page", paginationNo);

        Document WebpagesResponse = new Document()
                .append("pages", pagesDocuments)
                .append("pagination", paginationDocument);

        return WebpagesResponse.toJson();
    }

    // ################## PRIVATE METHODS ################## //
  
    // Parses the search query and generate a stemmed version of it
    private void parseQuery(String rawQuery, String number) throws Exception {
        originalQuery = (rawQuery == null ? "" : rawQuery.trim());
        query = (rawQuery == null ? "" : rawQuery.trim());

        // Check if the query is too short
        if (query.length() <= 3) {
            throw new Exception("Please enter a valid search query!");
        }

        // Process the query string
        isPhraseSearch = (query.startsWith("\"") && query.endsWith("\""));
        // query = query.substring(0, Math.min(query.length(), Constants.QUERY_MAX_LENGTH)); // CHECK
        query = utilFunctions.processString(query);
        queryWords = Arrays.asList(query.split(" "));

        // Remove stop words from search query in normal search mode
        if (!isPhraseSearch) {
            queryWords = utilFunctions.removeStopWords(queryWords);
        }

        queryStems = utilFunctions.stemWords(queryWords);

        // Check if no words are left after processing
        if (queryWords.isEmpty()) {
            throw new Exception("Please enter a valid search query!");
        }

        
        System.out.print("Search query:\t ");
        for (String word : queryWords) {
            System.out.print(word + " ");
        }
        System.out.println();

        
        System.out.print("Stems of the query words:\t ");
        for (String stem : queryStems) {
            System.out.print(stem + " ");
        }
        System.out.println();

        // if the number is not given we do try/catch block to avoid errors
        try {
            paginationNo = Integer.parseInt(number);
        } catch (Exception e) {
            paginationNo = 1;
        }
    }

   
    // Searches for web pages matching the search query and ranks the results.
    private void rankResults() throws Exception {
        
        long now, startTime = System.nanoTime();

        List<Webpage> matchingResults;

        if (isPhraseSearch) {
            matchingResults = mIndexer.searchPhrase(queryWords);
        } else {
            matchingResults = mIndexer.searchWords(queryStems);
        }

        
        now = System.nanoTime();
        System.out.printf("Search time:\t %.04f sec\n", (now - startTime) / 1e9);
        startTime = now;

        totalResultsCount = matchingResults.size();

        if (matchingResults.isEmpty()) {
            throw new Exception("Nothing was found!! Search one more time ... NOOB =) ");
        }

        //
        // Save search query for later suggestions
        //
        mIndexer.addSuggestion(query);

        //
        // Rank matching results
        //
        Ranker ranker = new Ranker(mIndexer, matchingResults, queryWords, queryStems);
        rankedIds = ranker.startRanking(paginationNo);
        // System.out.println("Hereee 2.0 !!!!!!!!!!!!!");
        // System.out.println(rankedIds);
        results = mIndexer.searchIds(rankedIds, env.FIELDS_FOR_SEARCH_RESULTS);

        //
        now = System.nanoTime();
        System.out.printf("Ranking time:\t %.04f sec\n", (now - startTime) / 1e9);

        //
        System.out.printf("Total results:\t %d\n", totalResultsCount);
    }

}   
