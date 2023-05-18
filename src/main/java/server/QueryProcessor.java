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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tukaani.xz.rangecoder.RangeEncoder;

import ch.qos.logback.classic.spi.PackagingDataCalculator;

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
    private static Ranker ranker;

    private int type1 = 0;  // 0 normal phrase,  1 AND  , 2 OR
    private int type2 = 0;  // 0 normal phrase,  1 AND  , 2 OR
    private List<String> firstOp; 
    private List<String> secondOp; 
    private List<String> thirdOp; 
    


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

        for (ObjectId id : rankedIds) {
            for (Webpage Webpage : results) {
                if (!Webpage._id.equals(id)) continue;
                
                String snippet = mSnippetly.extractWebPageSnippet(Webpage.pageData, utilFunctions.removeStopWordsOne(originalQuery.toLowerCase()));
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
        
        if(isPhraseSearch) {
            // regex for pattern ""anything" (AND|OR)"anything""
            
            String regex = "\"(.*?)\"\\s*(AND|OR)\\s*\"(.*?)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(query);

            String regex2 = "\"(.*?)\"\\s*(AND|OR)\\s*\"(.*?)\"\\s*(AND|OR)\\s*\"(.*?)\"";
            Pattern pattern2 = Pattern.compile(regex2);
            Matcher matcher2 = pattern2.matcher(query);

            if(matcher2.matches()){
                String firstAnything = matcher2.group(1);
                String firstOperator = matcher2.group(2);
                String secondAnything = matcher2.group(3);
                String secondOperator = matcher2.group(4);
                String thirdAnything = matcher2.group(5);

                firstOp = Arrays.asList(firstAnything.split(" "));
                secondOp = Arrays.asList(secondAnything.split(" "));
                thirdOp = Arrays.asList(thirdAnything.split(" "));
                // Process the matched values
                if(firstOperator.equals("AND")) type1 = 1;
                else if(firstOperator.equals("OR")) type1 = 2;
                if(secondOperator.equals("AND")) type2 = 1;
                else if(secondOperator.equals("OR")) type2 = 2;

            }
            else if (matcher.matches()) {
                // Input string exactly matches the regex pattern
                String firstAnything = matcher.group(1);
                String operator = matcher.group(2);
                String secondAnything = matcher.group(3);
                
                firstOp = Arrays.asList(firstAnything.split(" "));
                secondOp = Arrays.asList(secondAnything.split(" "));
                // Process the matched values
                if(operator.equals("AND")) type1 = 1;
                else if(operator.equals("OR")) type1 = 2;
                
            }
            else{
                type1 = 0;
                type2 = 0;
            }
        }
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
        if(paginationNo == 1) {
            List<Webpage> matchingResults = new ArrayList<>();

            if (isPhraseSearch) {
                // matchingResults = mIndexer.searchPhrase(queryWords);
                if(type1 == 0) matchingResults = mIndexer.searchPhrase(queryWords);
                else if(type1 == 1 && type2 == 0) matchingResults = mIndexer.searchANDPhrases(firstOp, secondOp);
                else if(type1 == 2 && type2 == 0) { 
                    List<Webpage> matchingResults1 = mIndexer.searchPhrase(firstOp);
                    List<Webpage> matchingResults2 = mIndexer.searchPhrase(secondOp);
                    Set<Webpage> mergedSet = new HashSet<>(matchingResults1);
                    mergedSet.addAll(matchingResults2);

                    // Create a new list from the merged set
                    matchingResults = new ArrayList<>(mergedSet);
                }
                // AND AND
                else if (type1 == 1 && type2 == 1) {
                    List<Webpage> matchingResults1 = mIndexer.searchANDPhrases(firstOp, secondOp);
                    List<Webpage> matchingResults2 = mIndexer.searchPhrase(thirdOp);
                    matchingResults = utilFunctions.intersectWebpage(matchingResults1, matchingResults2);
                }
                // AND OR
                else if (type1 == 1 && type2 == 2) {
                    List<Webpage> matchingResults1 = mIndexer.searchANDPhrases(firstOp, secondOp);
                    List<Webpage> matchingResults2 = mIndexer.searchPhrase(thirdOp);
                    Set<Webpage> mergedSet = new HashSet<>(matchingResults1);
                    mergedSet.addAll(matchingResults2);

                    // Create a new list from the merged set
                    matchingResults = new ArrayList<>(mergedSet);
                }
                // OR AND
                else if (type1 == 2 && type2 == 1) {
                    List<Webpage> matchingResults1 = mIndexer.searchPhrase(firstOp);
                    List<Webpage> matchingResults2 = mIndexer.searchPhrase(secondOp);
                    Set<Webpage> mergedSet = new HashSet<>(matchingResults1);
                    mergedSet.addAll(matchingResults2);

                    // Create a new list from the merged set
                    List<Webpage> matchingResults3 = new ArrayList<>(mergedSet);
                    List<Webpage> matchingResults4 = mIndexer.searchPhrase(thirdOp);
                    matchingResults = utilFunctions.intersectWebpage(matchingResults3, matchingResults4);
                }
                // OR OR
                else if (type1 == 2 && type2 == 2) {
                    List<Webpage> matchingResults1 = mIndexer.searchPhrase(firstOp);
                    List<Webpage> matchingResults2 = mIndexer.searchPhrase(secondOp);
                    List<Webpage> matchingResults3 = mIndexer.searchPhrase(thirdOp);
                    Set<Webpage> mergedSet = new HashSet<>(matchingResults1);
                    mergedSet.addAll(matchingResults2);
                    mergedSet.addAll(matchingResults3);

                    // Create a new list from the merged set
                    matchingResults = new ArrayList<>(mergedSet);
                }
            } else {
                matchingResults = mIndexer.searchWords(queryStems);
            }

            
            now = System.nanoTime();
            System.out.printf("Search time:\t %.04f sec\n", (now - startTime) / 1e9);
            startTime = now;

            //totalResultsCount = matchingResults.size();
            totalResultsCount = Math.min(150, matchingResults.size());
            

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
            ranker = new Ranker(mIndexer, matchingResults, queryWords, queryStems);

            ranker.startRanking();
        }

        rankedIds = ranker.paginateResults(paginationNo);
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
