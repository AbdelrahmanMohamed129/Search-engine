package server;

import indexer.Indexer;
import indexer.WebpageProcessor;
import spark.Request;
import spark.Response;
import utils.utilFunctions;

import java.util.List;
import static spark.Spark.*;
import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.port;

public class server {
    private static Indexer sIndexer = new Indexer();
    private WebpageProcessor webProcessor;
    
    public static void startServer() {
        // Setting the front end files
        externalStaticFileLocation(System.getProperty("user.dir") + "/client");

        // Setting the port number to listen on localhost:8000
        port(8000);

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
        });

        // Seraching route get request
        get("/search", (Request req, Response res) -> {
            String response = "";
            try {
                // initializing the QueryProcessor to process the search query
                QueryProcessor processor = new QueryProcessor(
                        sIndexer,
                        req.queryParams("q"),
                        req.queryParams("page")
                );
                response = processor.getJsonResult();
            } catch (Exception e) {
                e.printStackTrace();
                response = e.getMessage();
            }
            System.out.println("...............................................");
            System.out.println(response);
            return  response ;
        });

        get("/suggest", (Request req, Response res) -> {
            // Parse query string
            String queryString = utilFunctions.processString(req.queryParams("q"));

            // Get suggestions from the indexer
            List<String> suggestions = sIndexer.getSuggestions(queryString);

            return suggestions.toString();
        });
    }
}
