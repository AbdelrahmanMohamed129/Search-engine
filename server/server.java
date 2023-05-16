package server;

import indexer.Indexer;
import utils.env;
import utils.utilFunctions;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.port;

public class server {
    private static Indexer sIndexer = new Indexer();

    
    public static void startServer() {
        // Setting the front end files
        externalStaticFileLocation(System.getProperty("user.dir") + "/client");

        // Setting the port number to listen on localhost:5000
        port(5000);


        // Seraching route get request
        get("/search", (Request req, Response res) -> {
            String res = "";
            try {
                // initializing the QueryProcessor to process the search query
                QueryProcessor processor = new QueryProcessor(
                        sIndexer,
                        req.queryParams("q"),
                        req.queryParams("page")
                );
                res = processor.getJsonResult();
            } catch (Exception e) {
                e.printStackTrace();
                res = e.getMessage();
            }
            System.out.println("...............................................");
            System.out.println(res);
            return  res ;
        });

        get("/suggest", (Request req, Response res) -> {
            // Parse query string
            String queryString = Utilities.processString(req.queryParams("q"));

            // Get suggestions from the indexer
            List<String> suggestions = sIndexer.getSuggestions(queryString);

            return suggestions.toString();
        });
    }
}
