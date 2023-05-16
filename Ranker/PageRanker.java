/**
 * PageRanker
 */
package ranker;

import indexer.Indexer;
import indexer.Webpage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;


public class PageRanker {
    
    // ################## MEMBER VARIABLES ################## //
    Indexer mIndexer;
    Map<String, Webpage> graphNodes;
    Map<String, String> hostGraph;

    private Integer pagesCount;
    private HashMap<Integer, ArrayList<Integer>> adjList = new HashMap<>();     // The adjacency list used to represent the graph used in the ranking opeation
    private ArrayList<Integer> outDegrees;                                      // The number of the out degrees for each page
    private Integer nextWebpageID;                                              // The ID that is set to each Webpage
    private Map<String, Integer> pagesIDS;                                      // Map between the URL and its ID
    private ArrayList<Double> pagesRank;                                        // The rank set to each page


    private static final Double ALPHA = 0.85;                                   // Dumping factor

    private static final Integer MAX_ITERATIONS = 100;


    // ################## CONSTRUCTOR ################## //

    public PageRanker(Indexer index) {
        mIndexer = index;
    }
    
    
    // ################## PRIVATE METHODS ################## //

    // initialize all the lists and varibales

    private void init() {
        nextWebpageID = 0;

        outDegrees = new ArrayList<>();
        pagesRank = new ArrayList<>();
        pagesIDS = new HashMap<>();
        hostGraph = new HashMap<>();
    }

    // initialize all the variables used for the ranking algorithm

    private void initializePageRankLists() {
        for (int i = 0; i < pagesCount; i++) {

            // Create a new list for each page
            adjList.put(i, new ArrayList<>());

            outDegrees.add(0);
            pagesRank.add(1.0 / pagesCount); // Initialize at first with 1/n prob
        }
    }
    
    // Function used in building the mGraph and set the outDegrees

    private void addArc(int from, int to) {
        adjList.get(to).add(from);

        outDegrees.set(from, outDegrees.get(from) + 1);
    }


     // Function that updates pages ranks in the database

     private void updateRankerPagesRanks() {
        // Loop over the Map and update
        for (String WebpageURL : hostGraph.keySet()) {
            // Get its host URl.
            String WebpageHostURL = hostGraph.get(WebpageURL);

            graphNodes.get(WebpageURL).rank = pagesRank.get(pagesIDS.get(WebpageHostURL));
        }
        
        mIndexer.updatePageRanks(graphNodes.values());

    }


    // ################## PUBLIC METHODS ################## //

    // Starting page ranking 
    public void startPageRanker() throws Exception {
        System.out.println("Page ranking started !!!");

        // Get the graph and save it
        getGraph();
        saveGraph();

        rank();
        updateRankerPagesRanks();

        System.out.println("Page ranking finished !!!");
    }

    // Ranking algorithm 
    public void rank() {
        Double danglingSum, pagesRankSum = 1.0;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            danglingSum = 0.0;

            // Normalize the PR(i) needed for the power method calculations
            if (iteration > 0) {
                for (int page = 0; page < pagesCount; page++) {
                    Double rank = pagesRank.get(page) * 1.0 / pagesRankSum;
                    pagesRank.set(page, rank);
                    if (outDegrees.get(page) == 0) {
                        danglingSum += rank;
                    }
                }
            }

            pagesRankSum = 0.0;

            Double aPage = ALPHA * danglingSum * (1.0 / pagesCount); // Same for all pages
            Double oneProb = (1.0 - ALPHA) * (1.0 / pagesCount) * 1.0; // Same for all pages

            // Loop over all pages
            ArrayList<Double> newPagesRank = new ArrayList<>();
            for (int page = 0; page < pagesCount; page++) {

                Double hPage = 0.0;

                if (adjList.containsKey(page)) {
                    for (Integer from : adjList.get(page)) {
                        hPage += (1.0 * pagesRank.get(from) / (1.0 * outDegrees.get(from)));
                    }
                    hPage *= ALPHA; // Multiply by dumping factor.
                }

                newPagesRank.add(hPage + aPage + oneProb);
            }

            // Update new ranks
            for (int page = 0; page < pagesCount; page++) {
                pagesRank.set(page, newPagesRank.get(page));
                pagesRankSum += newPagesRank.get(page);
            }
        }
    }

    // Function that builds
    public void getGraph() {
        // Get the web pages in the graph (all nodes)
        graphNodes = mIndexer.getWebGraph();

        this.pagesCount = 0;

        init();

        // calculate pages count and get unique host pages ids.
        for (Map.Entry<String, Webpage> WebpageNode : graphNodes.entrySet()) {
            // Get the host web page url.
            String WebpageURL = getHostName(WebpageNode.getKey());
            // Map this url to its host url.
            hostGraph.put(WebpageNode.getKey(), WebpageURL);
            
            // if the URL is not repeated then give it a new pageID
            if (!pagesIDS.containsKey(WebpageURL)) {
                this.pagesCount++;
                pagesIDS.put(WebpageURL, nextWebpageID++);
            }

            // looping on the outlinks of the current WebpageNode
            for (String to : WebpageNode.getValue().outLinks) {
                String toHostURL = getHostName(to);

                if (graphNodes.containsKey(to)) { // Check if this out link page is currently indexed in the database.
                    // Map this url to its host url.
                    hostGraph.put(to, toHostURL);
                    
                    // if the URL is not repeated then give it a new pageID
                    if (!pagesIDS.containsKey(toHostURL)) {
                        this.pagesCount++;
                        pagesIDS.put(toHostURL, nextWebpageID++);
                    }
                }
            }
        }

        // Initialize pageRank lists after getting pages count this is why it is not in the same init function
        initializePageRankLists();

        // Building the graph
        for (Map.Entry<String, Webpage> WebpageNode : graphNodes.entrySet()) {
            // Get the host web page url.
            String WebpageHostURL = getHostName(WebpageNode.getKey());

            // Loop over all links and write arcs
            for (String to : WebpageNode.getValue().outLinks) {
                String toHostURL = getHostName(to);

                if (graphNodes.containsKey(to)) { // Check if this out link page is currently indexed in the database.
                    this.addArc(pagesIDS.get(WebpageHostURL), pagesIDS.get(toHostURL));
                }
            }
        }
    }
    
    // Function that saves the graph into a file (to remove later)
    private void saveGraph() throws Exception {
        // Write to the edges file
        PrintWriter pw = new PrintWriter("files/test/graph.txt");
        // Write the number of nodes
        pw.println(this.pagesCount);

        // Write arcs
        for (int to = 0; to < pagesCount; to++) {
            if (adjList.containsKey(to)) {
                for (int from : adjList.get(to)) {
                    pw.println(from + " " + to);
                }
            }
        }
        pw.close();
    }

    // ################## UTILITY METHODS ################## //
    public static String getHostName(String urlStr) throws URISyntaxException {
        String ret = null;
        
        URI url = new URI(urlStr);
        ret = url.getHost();
    
        return ret;
    }
}