package indexer;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class tryindexer {
    public static void main(String[] args) throws IOException  {
        // Document doc = Jsoup.connect("http://web.simmons.edu/~grovesd/comm244/notes/week3/html-test-page.html").get();
        // WebpageProcessor me  = new WebpageProcessor("http://web.simmons.edu/~grovesd/comm244/notes/week3/html-test-page.html",doc);
        // Webpage me1 = me.webpage;

        Indexer myIndexer = new Indexer();
        String[] searchwords = {"i","am"};
        List<String> stemWords = new ArrayList<>();
        for(String word : searchwords) {
            stemWords.add(getStem(word));
        }
        // List<Webpage> webpages = myIndexer.searchWords(Arrays.asList(searchwords));
        // List<Webpage> webpages = myIndexer.searchPhrase(Arrays.asList(searchwords));
        long startTime = System.currentTimeMillis();
        // List<Webpage> webpages = myIndexer.searchPhrase(stemWords);
        System.out.println(myIndexer.documentCountForWord("damn"));
        System.out.println(myIndexer.documentCountForStem("sad"));
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        // for(Webpage webpage : webpages) {
        //     System.out.println(webpage.url);
        // }
        //System.out.println(webpages.size());


        // Print the elapsed time
        System.out.println("Elapsed time: " + 1.0*elapsedTime/60000 + " minutes");
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
