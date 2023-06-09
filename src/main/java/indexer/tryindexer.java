package indexer;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class tryindexer {
    public static void main(String[] args) throws IOException, URISyntaxException  {
        String url = "https://www.edx.org";
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
        // WebpageProcessor me  = new WebpageProcessor("http://web.simmons.edu/~grovesd/comm244/notes/week3/html-test-page.html",doc);
        // Webpage me1 = me.webpage;



        // Indexer myIndexer = new Indexer();
        // Indexer.startOver();
        // myIndexer.startIndexingURL(url, doc, new ArrayList<>());
        // String[] searchwords = {"i","am"};
        // List<String> stemWords = new ArrayList<>();
        // for(String word : searchwords) {
        //     stemWords.add(getStem(word));
        // }
        // // List<Webpage> webpages = myIndexer.searchWords(Arrays.asList(searchwords));
        // // List<Webpage> webpages = myIndexer.searchPhrase(Arrays.asList(searchwords));
        // long startTime = System.currentTimeMillis();
        // // List<Webpage> webpages = myIndexer.searchPhrase(stemWords);
        // System.out.println(myIndexer.documentCountForWord("damn"));
        // System.out.println(myIndexer.documentCountForStem("sad"));
        // long endTime = System.currentTimeMillis();
        // long elapsedTime = endTime - startTime;
        // // for(Webpage webpage : webpages) {
        // //     System.out.println(webpage.url);
        // // }
        // //System.out.println(webpages.size());


        // Print the elapsed time
        //System.out.println("Elapsed time: " + 1.0*elapsedTime/60000 + " minutes");
    }

    public static String getStem(String word) {
        SnowballStemmer stemmer = new englishStemmer();
        String previousWord = word;
        stemmer.setCurrent(word);
        stemmer.stem();
        String now = stemmer.getCurrent();
        while(!now.equals(previousWord)) {
            previousWord = now;
            stemmer.setCurrent(now);
            stemmer.stem();
            now = stemmer.getCurrent();
        }
        return now;
    }

}
