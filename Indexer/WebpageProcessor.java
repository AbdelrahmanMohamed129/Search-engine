package indexer;

import java.util.*;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import utils.env;

public class WebpageProcessor {
    public StringBuffer pageData;
    public Webpage webpage;
    public int processedDataSize;
    
    public WebpageProcessor(String url, Document document) {
        pageData = new StringBuffer();
        processedDataSize = 0;
        webpage =  new Webpage();
        webpage.terms = new HashMap<>();
        webpage.stems = new HashMap<>();
        webpage.url = url;
        webpage.title = document.title();
        processText(webpage.title,"title");

        traverseHTML(document);
        webpage.pageData = pageData.toString().trim();
    }

    public WebpageProcessor() {}

    private void traverseHTML(Document document) {
        document.body().traverse(new NodeVisitor() {
            public void head(Node node, int depth) {
                if(node instanceof TextNode) {
                    /* cast to text node */
                    TextNode textNode = (TextNode) node;
                    /* get parent, to get tag */
                    Node parentNode = node.parent();

                    if (parentNode instanceof Element) {
                        Element parentElement = (Element) parentNode;
                        String tag = parentElement.tagName();

                        /* if tag is irrelevant, don't do anything */
                        if (!env.ALLOWED_TAGS_SET.contains(tag)) return;
                        /* otherwise, process text inside */
                        processText(textNode.text().trim(), tag);
                    }
                }
            }
            public void tail(Node node, int depth) {}
        });
    }

    private void processText(String text, String tag) {
        if(text.isEmpty()) return;

        /* Add this sentence to the page content */
        pageData.append(text+" ");

        /* Process the string before adding to index */
        text = processString(text);

        processedDataSize += text.length();

        /* Add to the words and stem indexes */
        String [] words = text.split(" ");

        int tagScore = env.TAG_SCORES.getOrDefault(tag, 1);

        for(String currentWord : words) {
            if(currentWord.isEmpty()) continue;

            int currentWordPos = webpage.totalWords++;

            /* Normal term indexing */
            if (webpage.terms.containsKey(currentWord)) {
                // Term exists in the index
                webpage.terms.get(currentWord).add(currentWordPos);
            } else {
                // Term does not exist in the index
                List<Integer> positions = new ArrayList<>();
                positions.add(currentWordPos);
                webpage.terms.put(currentWord, positions);
            }

            /* Term stemming */
            if(checkStopWord(currentWord)) continue; // if stop word, don't stem

            String currentStem = getStem(currentWord);
            if (webpage.stems.containsKey(currentStem)) {
                // Stem exists in the index
                Stem stemElement = webpage.stems.get(currentStem);
                stemElement.count++;
                stemElement.score+=tagScore;
            } else {
                // Stem does not exist in the index
                webpage.stems.put(currentStem, new Stem(1,tagScore));
            }


        }
    }
    

    /* utils */

    public String processString(String string) {
        /* normalize text */
        string = string.toLowerCase();
        /* any special character, including punctuation is replaced by a space */
        string = string.replaceAll("[^a-zA-Z0-9\\s]", " ");
        // /* remove number only words */
        // string = string.replaceAll("\\b\\d+\\b", " ");
        /* replace multiple spaces with one space */
        string = string.replaceAll("\\s+", " ");
        return string.trim();
    }

    public boolean checkStopWord(String word) {
        if(word.length() <= 2 || env.STOP_WORDS_SET.contains(word)) return true;
        return false;
    }

    public String getStem(String word) {
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