import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebCrawler {

    private Set<String> visitedUrls;
    private Queue<String> urlsToVisit;
    private int maxPages;
    private int currentPageCount;
    private ExecutorService executor;

    public WebCrawler(ArrayList<String> seedUrls, int maxPages, int numThreads) {
        this.visitedUrls = new HashSet<>();
        this.urlsToVisit = new LinkedList<>();
        for (String seedUrl : seedUrls) {
            this.urlsToVisit.add(seedUrl);
        }
        this.maxPages = maxPages;
        this.currentPageCount = 0;
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    public void crawl() throws IOException {
        while (!urlsToVisit.isEmpty() && currentPageCount < maxPages) {
            String url = urlsToVisit.poll();
            if (!visitedUrls.contains(url)) {
                try {
                    Document doc = Jsoup.connect(url).get();
                    visitedUrls.add(url);
                    currentPageCount++;
                    processPage(doc, url);
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String nextUrl = link.absUrl("href");
                        if (isValidUrl(nextUrl)) {
                            urlsToVisit.add(nextUrl);
                        }
                    }
                } catch (HttpStatusException e) {
                    // Ignore HTTP 404 errors and continue crawling
                    System.err.println("Error fetching URL: " + url);
                }
            }
        }
    }
    
    private void processPage(Document doc, String url) {
        // Implement logic to process page here
        System.out.println("Visited: " + url+ "     Number: " + visitedUrls.size());
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

    
    public void awaitTermination() throws InterruptedException {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }



public static void main(String[] args) throws IOException {
    int numThreads = 1000; // Read number of threads from command line
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
    WebCrawler crawler = new WebCrawler(seedUrls, 6000, numThreads);
    crawler.crawl();
    try {
        crawler.awaitTermination();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } // Wait for crawler to finish
    System.out.println("Crawling finished");
}

}