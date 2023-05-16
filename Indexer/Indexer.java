package indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

import java.util.*;

import org.bson.Document;
import utils.env;

public class Indexer {
    private MongoCollection<Document> webpagesCollection;

    public static void startOver() {
        MongoClient mongoConnection = new MongoClient(env.DATABASE_HOST,env.DATABASE_PORT);

        MongoDatabase myDatabase = mongoConnection.getDatabase(env.DATABASE_NAME);
        
        /* Drop old webpages collection, since drop database is deprecated in mongo driver 3.12 */
        MongoCollection<Document> oldWebpageCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);
        oldWebpageCollection.drop();

        /* Creating the new webpages collection, empty */
        MongoCollection<Document> webpageCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);

        /* Defining the index options, where each element in the index must be unique */
        IndexOptions indexOptions = new IndexOptions().unique(true);

        /* Creating the indexes on the fields in the webpages collection */
        /*  index on the actual urls, for fast retreival */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_URL), indexOptions);
        /* index on the words inside each document */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_TERM_INDEX + "." + env.FIELD_TERM));
        /* index on the stemmed words inside each document */
        webpageCollection.createIndex(Indexes.ascending(env.FIELD_STEM_INDEX + "." + env.FIELD_TERM));

        //mongoConnection.close();
    }

    public Indexer() {
        /* Create the connection */
        MongoClient mongoConnection = new MongoClient(env.DATABASE_HOST,env.DATABASE_PORT);
        /* Get the database */
        MongoDatabase myDatabase = mongoConnection.getDatabase(env.DATABASE_NAME);
        /* Get the webpages collection */
        webpagesCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);
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

    public List<Webpage> searchIds(List<String>ids, List<String>fields) {
        List<Document> results = webpagesCollection.find(Filters.in("_id", ids))
                                .projection(Projections.include(fields))
                                .into(new ArrayList<>());
        
        return convertToWebpages(results);
    }

    public List<Webpage> searchWords(List<String>stems) {
        List<Document> results = webpagesCollection
                                .find(Filters.in(env.FIELD_STEM_INDEX + "." + env.FIELD_TERM,stems))
                                .into(new ArrayList<>());
        
        return convertToWebpages(results);
    }

    public List<Webpage> searchPhrase(List<String>words) {
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

    /* Utils */
    public List<Webpage> convertToWebpages(List<Document> documents) {
        List<Webpage> webpages = new ArrayList<>();
        for (Document document : documents) {
            webpages.add(new Webpage(document));
        }
        return webpages;
    }
    
}