package crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
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

public class WebCrawler {

    private  Indexer index;
    private Set<String> visitedUrls;
    private Queue<String> urlsToVisit;
    private int maxPages;
    private int currentPageCount;
    private int numThreads;
    private ExecutorService executor;
    private final Lock lock = new ReentrantLock();

    public WebCrawler(ArrayList<String> seedUrls, int maxPages, int numThreads, Indexer index) {
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.urlsToVisit = new ConcurrentLinkedQueue<>();
        for (String seedUrl : seedUrls) {
            this.urlsToVisit.add(seedUrl);
        }
        this.maxPages = maxPages;
        this.currentPageCount = 0;
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
        this.index = index;
    }

    public void crawl() {
        for (int i = 0; i < numThreads; i++) {
            executor.execute(new CrawlerWorker());
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for threads to finish");
        }
    }

    private class CrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (!urlsToVisit.isEmpty() && currentPageCount < maxPages) {
                String url = urlsToVisit.poll();
                if (url == null) {
                    break;
                }
                URI uri_url;
                try {
                    uri_url = new URI(url).normalize();
                    url = uri_url.toString();
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    System.err.println("Error normalizing main URL: " + url);
                }
                if (!visitedUrls.contains(url)) {
                    try {
                        Document doc = Jsoup.connect(url).get();
                        Elements links = doc.select("a[href]");

                        ArrayList<String> linksArray = new ArrayList<>();
                        for (Element link : doc.select("a[href]")) {
                            String nextUrl = link.attr("href");
                            try {
                                URI uri = new URI(nextUrl).normalize();
                                nextUrl = uri.toString();
                                linksArray.add(nextUrl);
                            } catch (URISyntaxException e) {
                                System.err.println("Error normalizing outlink URL: " + nextUrl);
                            }
                        }


                        synchronized (lock) {
                            if (!visitedUrls.contains(url) && currentPageCount < maxPages){
                            // send to farah
                                if(index.startIndexingURL(url,doc,linksArray)){
                                    visitedUrls.add(url);
                                    currentPageCount = visitedUrls.size();
                                    System.out.println("Visited: " + url+ "     Number: " + visitedUrls.size());  
                                } 
                            }
                        }
                        
                        int count = 0;
                        for (Element link : links) {
                            String nextUrl = link.attr("href");

                            // Normalize the URL
                            try {
                                URI uri = new URI(nextUrl).normalize();
                                nextUrl = uri.toString();
                            } catch (URISyntaxException e) {
                                System.err.println("Error normalizing URL: " + nextUrl);
                                continue;
                            }

                            synchronized (this) {
                                if (isValidUrl(nextUrl)) {
                                if (count >= 5) {
                                    break;
                                }
                                urlsToVisit.add(nextUrl);
                                count++;
                                }
                            }
                            
                        }

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

        
        private boolean isValidUrl(String url) {
             // Implement logic to check if url is valid for crawling here
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Normalize the URL
        try {
            URI uri = new URI(url).normalize();
            String normalizedUrl = uri.toString();

            // Check if the normalized URL is referring to the same page using a compact string
            String compactString = Jsoup.parse(normalizedUrl).body().text();
            return !visitedUrls.contains(compactString);
        } catch (URISyntaxException e) {
            return false;
        }
        }
    
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

        newIndexer.startOver();

        WebCrawler crawler = new WebCrawler(seedUrls, 50, 10, newIndexer);
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