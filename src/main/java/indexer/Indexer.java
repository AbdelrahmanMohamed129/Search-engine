package indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

import java.util.*;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import utils.env;

public class Indexer {
    private MongoCollection<Document> webpagesCollection;
    private MongoCollection<Document> wordsCollection;
    private MongoCollection<Document> stemsCollection;
    private MongoCollection<Document> suggestCollection;

    public static void startOver() {
        MongoClient mongoConnection = new MongoClient(env.DATABASE_HOST,env.DATABASE_PORT);

        MongoDatabase myDatabase = mongoConnection.getDatabase(env.DATABASE_NAME);
        
        /* Drop old webpages collection, since drop database is deprecated in mongo driver 3.12 */
        MongoCollection<Document> oldWebpageCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);
        oldWebpageCollection.drop();
        MongoCollection<Document> oldWordCollection = myDatabase.getCollection(env.COLLECTION_WORDS);
        oldWordCollection.drop();
        MongoCollection<Document> oldStemCollection = myDatabase.getCollection(env.COLLECTION_STEMS);
        oldStemCollection.drop();
        MongoCollection<Document> oldSuggestCollection = myDatabase.getCollection(env.COLLECTION_SUGGESTIONS);
        oldSuggestCollection.drop();

        /* Creating the new webpages collection, empty */
        MongoCollection<Document> webpageCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);

        /* Defining the index options, where each element in the index must be unique */
        IndexOptions indexOptions = new IndexOptions().unique(true);

        /* Creating the indexes on the fields in the webpages collection */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_URL), indexOptions);
        /* index on the words inside each document */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_TERM_INDEX + "." + env.FIELD_TERM));
        /* index on the stemmed words inside each document */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_STEM_INDEX + "." + env.FIELD_TERM));

        /* Creating the words collection */
        MongoCollection<Document> wordsCollection = myDatabase.getCollection(env.COLLECTION_WORDS);
        wordsCollection.createIndex(Indexes.ascending(env.FIELD_TERM), indexOptions);
        wordsCollection.createIndex(Indexes.ascending(env.FIELD_URLS + "." + env.FIELD_URL));
        /* Creating the stems collection */
        MongoCollection<Document> stemsCollection = myDatabase.getCollection(env.COLLECTION_STEMS);
        stemsCollection.createIndex(Indexes.ascending(env.FIELD_TERM), indexOptions);
        stemsCollection.createIndex(Indexes.ascending(env.FIELD_URLS + "." + env.FIELD_URL));
        /* Creating the suggestions collection */
        MongoCollection<Document> suggestCollection = myDatabase.getCollection(env.COLLECTION_STEMS);
        suggestCollection.createIndex(Indexes.ascending(env.FIELD_SUGGEST_QUERY), indexOptions);
        suggestCollection.createIndex(Indexes.ascending(env.FIELD_SUGGEST_QUERY));
        //mongoConnection.close();
    }

    public Indexer() {
        /* Create the connection */
        MongoClient mongoConnection = new MongoClient(env.DATABASE_HOST,env.DATABASE_PORT);
        /* Get the database */
        MongoDatabase myDatabase = mongoConnection.getDatabase(env.DATABASE_NAME);
        /* Get the webpages collection */
        webpagesCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);
        wordsCollection = myDatabase.getCollection(env.COLLECTION_WORDS);
        stemsCollection = myDatabase.getCollection(env.COLLECTION_STEMS);
        suggestCollection = myDatabase.getCollection(env.COLLECTION_SUGGESTIONS);
        /* Close the connection */
        //mongoConnection.close();
    }

    /* Indexing functionalities */
    public boolean startIndexingURL(String url, org.jsoup.nodes.Document document, ArrayList<String>outlinks) {
        WebpageProcessor processor = new WebpageProcessor(url, document);
        Webpage webpage = processor.webpage;

        /* If we could only process less than 75% of the document, then discard it */
        if(webpage.pageData.isEmpty() || 1.0*processor.processedDataSize/webpage.pageData.length() < 0.75) {
            System.out.println("Indexing: " + url + ". Fail :/");
            return false;
        }

        webpage.outlinks = outlinks;
        addWebpageToDB(webpage);
        System.out.println("Indexing: " + url + ". Success!");

        return true;
    }

    public void addWebpageToDB(Webpage webpage) {
        webpagesCollection.insertOne(webpage.convertToDocument());
        /* update words */
        updateWords(webpage.url, webpage.terms);
        /* updates stems */
        updateStems(webpage.url, webpage.stems);
    }

    public void updateWords(String url, HashMap<String,List<Integer>> terms) {
        List<WriteModel<Document>> updateModels = new ArrayList<>();
        UpdateOptions options = new UpdateOptions().upsert(true);

        for (Map.Entry<String, List<Integer>> entry: terms.entrySet()) {
            Document newURL = new Document(env.FIELD_URL, url).append(env.FIELD_TERM_POSITIONS, entry.getValue());
            updateModels.add(new UpdateOneModel<>(
                    Filters.eq(env.FIELD_TERM, entry.getKey()),
                    Updates.addToSet(env.FIELD_URLS, newURL),
                    options
            ));
        }

        if(!updateModels.isEmpty()) wordsCollection.bulkWrite(updateModels);
    }

    public void updateStems(String url, HashMap<String,Stem> stems) {
        List<WriteModel<Document>> updateModels = new ArrayList<>();
        UpdateOptions options = new UpdateOptions().upsert(true);

        for (Map.Entry<String,Stem> entry: stems.entrySet()) {
            Document newURL = new Document(env.FIELD_URL, url)
            .append(env.FIELD_STEM_COUNT, entry.getValue().count)
            .append(env.FIELD_STEM_SCORE, entry.getValue().score);
            updateModels.add(new UpdateOneModel<>(
                    Filters.eq(env.FIELD_TERM, entry.getKey()),
                    Updates.push(env.FIELD_URLS, newURL),
                    options
            ));
        }

        if(!updateModels.isEmpty()) stemsCollection.bulkWrite(updateModels);
    }

    /* Ranking helper functionalities */

    public void updateRank(Collection<Webpage>webpages) {
        List<UpdateOneModel<Document>> updateModels = new ArrayList<>();
        for (Webpage webpage : webpages) {
            updateModels.add(new UpdateOneModel<>(
                    Filters.eq(env.FIELD_ID, webpage._id),
                    Updates.set(env.FIELD_RANK, webpage.rank)
            ));
        }
        if(!updateModels.isEmpty()) webpagesCollection.bulkWrite(updateModels);
    }

    /* Get all documents with their outlinks for initial ranking */
    public HashMap<String, Webpage> getAllWebpagesForRanker() {
        /* only select outlinks for each url */
        List<Document> results = webpagesCollection.find()
                                                   .projection(Projections.include(env.FIELD_URL, env.FIELD_OUTLINKS))
                                                   .into(new ArrayList<>());
        HashMap<String, Webpage> webpages = new HashMap<>();
        for(Document webpage : results) {
            Webpage objectDocument = new Webpage(webpage);
            webpages.put(objectDocument.url, objectDocument);
        }
        return webpages;
    }

    /* Searching functionalities */

    public List<Webpage> searchIds(List<ObjectId>ids, List<String>fields) {
        List<Document> results = webpagesCollection.find(Filters.in("_id", ids))
                                .projection(Projections.include(fields))
                                .into(new ArrayList<>());
        
        return convertToWebpages(results);
    }

    public List<Webpage> searchWords(List<String>stems) {
        /* find the stems */
        Bson filter = Filters.in(env.FIELD_TERM, stems);
        FindIterable<Document> stemDocuments = stemsCollection.find(filter);
        Set<String> urls = new HashSet<>();

        /* retreive urls */
        for (Document wordDocument : stemDocuments) {
            List<Document> urlsList = wordDocument.getList(env.FIELD_URLS, Document.class);
            for (Document urlDocument : urlsList) {
                String url = urlDocument.getString(env.FIELD_URL);
                urls.add(url);
            }
        }

        /* find webpages from db corresponding to urls */
        List<Document> results = webpagesCollection
                                .find(Filters.in(env.FIELD_URL,urls))
                                .into(new ArrayList<>());
        
        return convertToWebpages(results);
    }


    public List<Webpage> searchPhrase(List<String>words) {
        if(words.isEmpty()) return new ArrayList<>(); 

        Bson filter = Filters.in(env.FIELD_TERM, words);

        // Retrieve documents of all words in the search query, from the inverted index
        FindIterable<Document> wordDocuments = wordsCollection.find(filter);

        List<Set<String>> urlSets = new ArrayList<>();
        for (Document wordDocument : wordDocuments) {
            List<Document> urlsList = wordDocument.getList(env.FIELD_URLS, Document.class);
            Set<String> urls = new HashSet<>();
            for (Document urlDocument : urlsList) {
                String url = urlDocument.getString(env.FIELD_URL);
                urls.add(url);
            }
            urlSets.add(urls);
        }

        // Perform intersection to keep only the URLs that contain all query words
        Set<String> intersection = new HashSet<>(urlSets.get(0));
        for (int i = 1; i < urlSets.size(); i++) {
            intersection.retainAll(urlSets.get(i));
        }

        // Finding the documents of the urls that survived the intersection
        List<Webpage> webpages = new ArrayList<>();
        for(String url : intersection) {
            webpages.add(findByURL(url));
        }

        String firstWord = words.get(0);
        List<Webpage> correctWebpages = new ArrayList<>();

        // binary searching on positions, to get query correctly
        for(Webpage webpage : webpages) {
            HashMap<String,List<Integer>> termsPositions = webpage.terms;
            boolean flag = true;
            for(Integer pos : termsPositions.get(firstWord)) {
                for (int i = 1; i < words.size(); i++) {
                    if (Collections.binarySearch(termsPositions.get(words.get(i)), pos + i) < 0) {
                        flag = false;
                        break;
                    }
                }
                if(flag == true) correctWebpages.add(webpage);
            }
        }

        return correctWebpages;
    }

    /* Suggestions Functionalities */
    public List<String> getSuggestions(String query) {
        List<String> matchedQueries = new ArrayList<>();
        Document filter = new Document("query", new Document("$regex", "^" + query));
        List<Document> matchingDocuments = suggestCollection.find(filter).into(new ArrayList<>());
        for (Document document : matchingDocuments) {
            matchedQueries.add(document.getString(env.FIELD_SUGGEST_QUERY));
        }
        return matchedQueries;
    }

    public void addSuggestion(String query) {
        Bson filter = Filters.eq(env.FIELD_SUGGEST_QUERY, query);
        Bson update = Updates.set(env.FIELD_SUGGEST_QUERY, query);
        suggestCollection.updateOne(filter,update,new UpdateOptions().upsert(true));
    }
    
    /* Indexer Utils */
    public List<Webpage> convertToWebpages(List<Document> documents) {
        List<Webpage> webpages = new ArrayList<>();
        for (Document document : documents) {
            webpages.add(new Webpage(document));
        }
        return webpages;
    }

    /* Indexer Utils for Ranker */
    public long documentsCount() {
        return webpagesCollection.countDocuments();
    }

    public long documentCountForStem(String word) {
        Document document = stemsCollection.find(Filters.eq(env.FIELD_TERM, word)).first();
        if(document != null) {
            List<Document> urls = document.get(env.FIELD_URLS, List.class);
            return (urls != null) ? urls.size() : 0;
        }
        return 0;
    }

    public long documentCountForWord(String word) {
        Document document = wordsCollection.find(Filters.eq(env.FIELD_TERM, word)).first();
        if(document != null) {
            List<Document> urls = document.get(env.FIELD_URLS, List.class);
            return (urls != null) ? urls.size() : 0;
        }
        return 0;
    }

    public Webpage findByURL(String url) {
        Document document = webpagesCollection.find(Filters.eq(env.FIELD_URL, url)).first();
        return new Webpage(document);
    }
    
    /* Unused functions */
    
    public List<Webpage> searchWordsNotInverted(List<String>stems) {
        List<Document> results = webpagesCollection
        .find(Filters.in(env.FIELD_STEM_INDEX + "." + env.FIELD_TERM,stems))
        .into(new ArrayList<>());

        return convertToWebpages(results);
    }

    public List<Webpage> searchPhraseNotInverted(List<String>words) {
        if(words.isEmpty()) return new ArrayList<>();
        List<Document> results = webpagesCollection
                                .find(Filters.all(env.FIELD_TERM_INDEX + "." + env.FIELD_TERM,words))
                                .into(new ArrayList<>());

        List<Webpage> webpages = convertToWebpages(results);

        String firstWord = words.get(0);
        List<Webpage> correctWebpages = new ArrayList<>();

        for(Webpage webpage : webpages) {
            HashMap<String,List<Integer>> termsPositions = webpage.terms;
            boolean flag = true;
            for(Integer pos : termsPositions.get(firstWord)) {
                for (int i = 1; i < words.size(); i++) {
                    if (Collections.binarySearch(termsPositions.get(words.get(i)), pos + i) < 0) {
                        flag = false;
                        break;
                    }
                }
                if(flag == true) correctWebpages.add(webpage);
            }
        }

        return correctWebpages;
    }
}