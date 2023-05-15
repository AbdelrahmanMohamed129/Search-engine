import java.io.FileWriter;
import java.io.IOException;

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
        String now  = getStem("2001");
        System.out.println(now);
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
