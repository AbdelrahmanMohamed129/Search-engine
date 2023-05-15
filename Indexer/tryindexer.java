package indexer;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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
        myIndexer.startOver();
        String[] urls = {
            "http://web.simmons.edu/~grovesd/comm244/notes/week3/html-test-page.html",
            "https://ar.wikipedia.org/wiki/%D8%A8%D9%88%D8%A7%D8%A8%D8%A9:%D8%A7%D9%84%D9%84%D8%BA%D8%A9_%D8%A7%D9%84%D8%B9%D8%B1%D8%A8%D9%8A%D8%A9"
        };
        Document doc1 = Jsoup.connect(urls[0]).get();
        Document doc2 = Jsoup.connect(urls[1]).get();
        myIndexer.startIndexingURL(urls[0], doc1, new ArrayList<>());
        myIndexer.startIndexingURL(urls[1], doc2, null);
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
    // public static void main(String[] args) throws IOException {
    //     System.out.println(getStem("unhappily"));
    //     System.out.println(getStem("computing"));

    //     // Document doc = Jsoup.connect("https://codeforces.com/").get();
    //     // FileWriter myWriter = new FileWriter("theirway.txt");
    //     // dfs(doc.body(),myWriter);
    //     // doc.body().traverse(new NodeVisitor() {
    //     //     public void head(Node node, int depth) {
    //     //         if(node instanceof TextNode) {
    //     //                 TextNode textNode = (TextNode) node;
    //     //                 Node parentNode = node.parent();
    //     //                 if (parentNode instanceof Element) {
    //     //                     Element parentElement = (Element) parentNode;
    //     //                     if(parentElement.tagName() == "html") return;
    //     //                     try {
    //     //                         myWriter.write("Tag: " + parentElement.tagName() + "\n"+"Text: " + textNode.text().trim());
    //     //                     } catch (IOException e) {
    //     //                         // TODO Auto-generated catch block
    //     //                         e.printStackTrace();
    //     //                     }
    //     //                 }
    //     //         }
    //     //     }
    //     //     public void tail(Node node, int depth) {
    //     //         // 
    //     //     }
    //     // });
    //     // myWriter.close();
    // }
    // private static void dfs(Node cur, FileWriter myWriter) {
    //     // If its a text node then process its text
    //     if (cur instanceof TextNode) {
    //         TextNode node = (TextNode) cur;
    //         Node parentNode = node.parent();
    //                     if (parentNode instanceof Element) {
    //                         Element parentElement = (Element) parentNode;
    //                         if(parentElement.tagName() == "html") return;
    //                         try {
    //                             myWriter.write("Tag: " + parentElement.tagName() + "\n"+"Text: " + node.text().trim());
    //                         } catch (IOException e) {
    //                             // TODO Auto-generated catch block
    //                             e.printStackTrace();
    //                         }
    //                     }
    //         return;
    //     }

    //     // If it is an element node then recursively call the DFS function
    //     // with the children nodes of allowed tag
    //     if (cur instanceof Element) {
    //         Element element = (Element) cur;
    //         String tag = element.tagName();

    //         for (Node child : cur.childNodes()) {
    //             dfs(child,myWriter);
    //         }
    //     }
    // }


}
