package integration;

import crawler.WebCrawler;
import indexer.Indexer;
import ranker.PageRanker;
import server.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Integration {
    private static BufferedReader bf;

    public static void main(String[] args) throws Exception {
        bf = new BufferedReader(new InputStreamReader(System.in));

        int choice = -1;

        while (choice == -1) {
            System.out.println("################ BINGO SEARCH ENGINE ################");
            System.out.println("Choose one of the following functions");
            System.out.println("1. Crawl");
            System.out.println("2. Search");
            System.out.println("3. Start over");
            System.out.println("4. Exit");
            
            choice = Integer.parseInt(bf.readLine());
            System.out.println("######################################################");
            
            long startTime = System.nanoTime();

            switch (choice) {
                case 1:
                    startCrawler();
                    break;
                case 2:
                    startServer();
                    break;
                case 3:
                    startOver();
                    break;
                case 4:
                    System.out.println("GoodBye !!!");
                    break;
                default:
                    choice = -1;
                    break;
            }

            long endTime = System.nanoTime();
            long millis = (endTime - startTime) / (long) 1e6;
            long secs = millis / 1000;

            System.out.printf("Elapsed Time: %d min : %d sec : %d ms\n", secs / 60, secs % 60, millis % 1000);
        }

        bf.close();
    }

    private static void startCrawler() throws Exception {
        System.out.println("Please enter the number of crawler threads: ");
        int threadsNo = Integer.parseInt(bf.readLine());
        BufferedReader seedsFile = new BufferedReader(new FileReader("seedList.txt"));
        
        ArrayList<String> URLs = new ArrayList<>();
        String line= null;

        // Looping on all the lines in the text file to get all available doctors
        while((line = seedsFile.readLine()) != null){
            URLs.add(line);
        }
        
        Indexer indexer = new Indexer();
        WebCrawler crawler = new WebCrawler(URLs,500,threadsNo,indexer);
        Indexer.startOver();
        crawler.crawl();

        PageRanker pageRanker = new PageRanker(indexer);
        pageRanker.startPageRanker();
        seedsFile.close();
    }

    private static void startOver() throws Exception {
        System.out.println("Deleting all DB content");
        Indexer.startOver();
        
    }

    private static void startServer() {
        // Server
        server.startServer();
        System.out.println("listening on port 8000");
    }

}
