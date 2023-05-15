package indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

import java.util.ArrayList;

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

        mongoConnection.close();
    }

    public Indexer() {
        /* Create the connection */
        MongoClient mongoConnection = new MongoClient(env.DATABASE_HOST,env.DATABASE_PORT);
        /* Get the database */
        MongoDatabase myDatabase = mongoConnection.getDatabase(env.DATABASE_NAME);
        /* Get the webpages collection */
        webpagesCollection = myDatabase.getCollection(env.COLLECTION_WEBPAGES);
        /* Close the connection */
        mongoConnection.close();
    }

    public boolean startIndexingURL(String url, org.jsoup.nodes.Document document, ArrayList<String>outlinks) {
        WebpageProcessor processor = new WebpageProcessor(url, document);
        Webpage webpage = processor.webpage;

        /* If we could process less than 75% of the document, then discard it */
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
}