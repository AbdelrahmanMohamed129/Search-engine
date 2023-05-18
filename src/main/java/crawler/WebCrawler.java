package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLHandshakeException;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import indexer.Indexer;

// Crawler class 
public class WebCrawler {

    // Data members
    private  Indexer index; // Object of indexer
    private Set<String> visitedUrls; // Set of strings holding urls previously visited
    private Queue<String> urlsToVisit; // List of strings containing urls to be visited
    private int maxPages;  // Maximum number of pages to output from crawler
    private int currentPageCount; // Count of currently visited urls
    private int numThreads; // Number of threads to perform crawling
    private ExecutorService executor; // Provides methods to manage threads terminations
    private final Lock lock = new ReentrantLock(); // Lock for synchronization

    // Crawler constructor
    public WebCrawler(ArrayList<String> seedUrls, int maxPages, int numThreads, Indexer index) {

        this.visitedUrls = ConcurrentHashMap.newKeySet(); // Thread safe map
        this.urlsToVisit = new ConcurrentLinkedQueue<>(); // Thread safe queue
        for (String seedUrl : seedUrls) {
            this.urlsToVisit.add(seedUrl);
        }
        this.maxPages = maxPages; 
        this.currentPageCount = 0;
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
        this.index = index;

        // Add shutdown hook to save urlsToVisit to a file
        // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        // try {
        //     PrintWriter writer = new PrintWriter("links.txt");
        //     for (String url : urlsToVisit) {
        //         writer.println(url);
        //     }
        //     writer.close();
        // } catch (FileNotFoundException e) {
        //     System.err.println("Error saving urlsToVisit: " + e.getMessage());
        // }
        // }));

        // try {
        //     if (Files.size(Paths.get("links.txt")) > 0) {
        //         try (Scanner scanner = new Scanner(new File("links.txt"))) {
        //             while (scanner.hasNextLine()) {
        //                 String url = scanner.nextLine();
        //                 urlsToVisit.add(url);
        //             }
        //         } catch (FileNotFoundException e) {
        //             System.err.println("Error loading URLs from file: " + e.getMessage());
        //         }
        //     } else {
        //         for (String seedUrl : seedUrls) {
        //             urlsToVisit.add(seedUrl);
        //         }
        //     }
        // } catch (IOException e) {
        // }
    }

    // Crawl function that starts crawling using threads
    public void crawl() {

        long startTime = System.currentTimeMillis(); // Time analysis (for our own use)

        // Assign threads to start crawling
        for (int i = 0; i < numThreads; i++) {
            executor.execute(new CrawlerWorker());
        }
        // Termination of threads and printing total time
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Crawling elapsed time: " + 1.0*elapsedTime/60000 + " minutes");
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for threads to finish");
        }
    }

    // CrawlerWorker class that implements runnable and contains run function performed by threads
    private class CrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (currentPageCount < maxPages ) {

                String url = urlsToVisit.poll(); // Retrieves and removes the head of this queue, or returns null if this queue is empty
                if (url == null) {
                    break;
                }

                // Normalizing the url before saving it in visited urls
                URI uri_url;
                try {
                    uri_url = new URI(url).normalize();
                    url = uri_url.toString();
                    url=extraNormalize(url);
                } catch (URISyntaxException e) {
                    System.err.println("Error normalizing main URL: " + url);
                }


                // Checking that this url hasn't been crawled before
                if (!visitedUrls.contains(url) && currentPageCount < maxPages) {
                                     try {

                    // Download link
                    Document doc = Jsoup.connect(url).timeout(5000).get();
                    // Put all links found in this document in links
                    Elements links = doc.select("a[href]");
                                    
                    // Put these links in Array instead of elements to be able to send it to the indexer
                    // Also make sure they are all normalized before sending them to indexer
                    // The ranker is the one who's gonna use them later on
                    ArrayList<String> linksArray = new ArrayList<>();
                    for (Element link : doc.select("a[href]")) {
                    String nextUrl = link.attr("href");
                    try {
                        URI uri = new URI(nextUrl).normalize();
                        nextUrl = uri.toString();
                        nextUrl=extraNormalize(nextUrl);
                        linksArray.add(nextUrl);
                    } catch (URISyntaxException e) {
                        System.err.println("Error normalizing outlink URL: " + nextUrl);
                    }
                        }

                // Using synchronization with lock and not this to ensure that one thread enters this critical part at a time
                synchronized (lock) {
                    // Check again that this url has not been crawled before
                    if (!visitedUrls.contains(url) && currentPageCount < maxPages){
                    // Send it to indexer along with all the other urls found in it
                    if(index.startIndexingURL(url,doc,linksArray)){
                    // Add it to visited urls and increase current visited webpages count
                    visitedUrls.add(url);
                    currentPageCount = visitedUrls.size();
                    System.out.println("Visited: " + url+ "     Number: " + visitedUrls.size());  
                        } 
                    }
                }

                // Now i get links inside this link for my own use (crawler use)
                // this time i will limit the depth to 5 as i don't want my crawler to crawl down the tree vertically
                // BFS 

                int count = 0; 
                for (Element link : links) {
                 String nextUrl = link.attr("href");

                 // Normalize the outlinks found for the crawler use
                 try {
                    if (isValidUrl(nextUrl)) {
                    URI uri = new URI(nextUrl).normalize();
                    nextUrl = uri.toString();
                    nextUrl=extraNormalize(nextUrl);
                    
                        }
                    } catch (URISyntaxException e) {
                        System.err.println("Error normalizing URL: " + nextUrl);
                        continue;
                    }

                 // Synchronize so that no more than one thread adds to the urls to visit duplicated urls at the same time
                synchronized (this) {
                        // Check first if url is valid
                    if (isValidUrl(nextUrl)) {
                    // Check next if depth reached 5
                    if (count >= 5) {
                        break;
                    }
                    // If both conditions passed add to urls to visit
                        urlsToVisit.add(nextUrl);
                        count++;
                    }
                }

            }
                
                // Handling multiple possible errors due to connection
                } catch (HttpStatusException e) {
                    // Ignore HTTP 404 errors and continue crawling
                    System.err.println("Error fetching URL: " + url);
                } catch (SSLHandshakeException e) {
                    // Ignore SSL handshake errors and continue crawling
                    System.err.println("Error in SSL handshake with URL: " + url);
                } catch (SocketTimeoutException e) {
                    // Ignore socket timeout errors and continue crawling
                    System.err.println("Timeout fetching URL: " + url);
                } catch (IOException e) {
                    System.err.println("Error connecting to URL: " + url);
                }   
                }
            }

        }
    }

    // Boolean function isValidUrl returns true if we can crawl this url else returns false
    // Checks on start of url and type https (because jsoup library ignores it)
    // Normalizes the string to use then compact strings and make sure content is not duplicated in different urls
    // Checks if website is disallowed in robot.txt

    private boolean isValidUrl(String url) {

        // Implement logic to check if url is valid for crawling here
        if (!url.startsWith("http://www.") && !url.startsWith("https://www.")) {
            return false;
        }
    
        // Normalize the URL
        try {
            URI uri = new URI(url).normalize();
            String normalizedUrl = uri.toString();
    
            // Check if the normalized URL is referring to the same page using a compact string
            String compactString = Jsoup.parse(normalizedUrl).body().text();
            if (visitedUrls.contains(compactString)) {
                return false;
            }
    
        // Check if the website allows crawling based on robots.txt file
        String domain = uri.getHost();
        URL robotsUrl = new URL("http://" + domain + "/robots.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(robotsUrl.openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("User-agent: *")) {
                // Check if the website disallows crawling for all user agents
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Disallow: ")) {
                        String disallowedPath = line.substring("Disallow: ".length());
                        if (uri.getPath().startsWith(disallowedPath)) {
                            return false;
                        }
                    } else {
                        // Stop checking if we encounter a non-disallow line
                        break;
                    }
                }
                break;
            }
        }

            return true;
        } catch (URISyntaxException | IOException e) {
            return false;
        }
    }
    	
    public String extraNormalize(String webPageUrl) throws URISyntaxException {
		
		// case of no url
		if (webPageUrl == null) {
			return null;
		}
		
		if(webPageUrl != null) {
            webPageUrl = webPageUrl.replaceAll("//*/", "/");
            webPageUrl = webPageUrl.replaceFirst("^(http:/)", "http://");
            webPageUrl = webPageUrl.replaceFirst("^(https:/)", "http://");
			if(webPageUrl.length() > 0 && webPageUrl.charAt(webPageUrl.length() - 1) == '/') {
				webPageUrl = webPageUrl.substring(0, webPageUrl.length() - 1);
			}
		}		
		return webPageUrl;
	}
    
    public static void main(String[] args) throws IOException {
        ArrayList<String> seedUrls = new ArrayList<>();
        try {
            File file = new File("seedList.txt");
            Scanner scanner = new Scanner(file);
            
            while (scanner.hasNextLine()) {
                String url = scanner.nextLine();
                seedUrls.add(url);
            }
            
            scanner.close(); 
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        }

        long startTime = System.currentTimeMillis();

        Indexer newIndexer = new Indexer();

        Indexer.startOver();

        WebCrawler crawler = new WebCrawler(seedUrls, 6000, 100, newIndexer);
        crawler.crawl();

        // End recording the time
        long endTime = System.currentTimeMillis();

        // Calculate the elapsed time
        long elapsedTime = endTime - startTime;

        // Print the elapsed time
        System.out.println("Elapsed time: " + 1.0*elapsedTime/60000 + " minutes");

        // Wait for crawler to finish
        System.out.println("Crawling finished");
    }
}     