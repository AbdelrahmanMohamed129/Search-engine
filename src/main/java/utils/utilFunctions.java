package utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import indexer.Webpage;

import java.util.*;

public class utilFunctions {

    public static String processString(String string) {
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

    // Removes stop words from the given list of words
    public static List<String> removeStopWords(List<String> words) {
        List<String> ret = new ArrayList<>();
        for (String word : words) {
            if (word.length() <= 2 || env.STOP_WORDS_SET.contains(word)) {
                continue;
            }
            ret.add(word);
        }

        return ret;
    }

    public static String removeStopWordsOne(String words) {
        StringBuffer ret = new StringBuffer();
        for (String word : words.split(" ")) {
            if (word.length() <= 2 || env.STOP_WORDS_SET.contains(word)) {
                continue;
            }
            ret.append(word+" ");
        }

        return ret.toString();
    }

    // It takes an array of strings and returns array of stemmed words
    public static List<String> stemWords(List<String> words) {
        List<String> ret = new ArrayList<>();

        for (String word : words) {
            ret.add(stemWord(word));
        }

        return ret;
    }

    // Finding the base stem of a word, this may take multiple iterations
    public static String stemWord(String word) {
        SnowballStemmer stemmer = new englishStemmer();

        String lastWord = word;

        while (true) {
            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();

            if (word.equals(lastWord))
                break;

            lastWord = word;
        }

        return word;
    }


    // Returns the given word without any characters except letters or digits in prefix or postfix
    public static String removeSpecialCharsAroundWord(String word) {
        int firstLetterIdx = 0, lastLetterIdx = word.length() - 1;

        // Prefix chars
        for (int i = 0; i < word.length(); ++i) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                firstLetterIdx = i;
                break;
            }
        }

        // Postfix chars
        for (int i = word.length() - 1; i >= 0; --i) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                lastLetterIdx = i;
                break;
            }
        }

        return word.substring(firstLetterIdx, lastLetterIdx + 1);
    }


    public static List<Webpage> intersectWebpage(List<Webpage>firstPhrase,List<Webpage>secondPhrase) {
        if(firstPhrase.isEmpty() || secondPhrase.isEmpty()) return new ArrayList<>(); 

        HashMap<String,Webpage> webpageMap = new HashMap<>();

        List<Set<String>> urlSets = new ArrayList<>();
        Set<String> tempURL = new HashSet<>();
        for (Webpage wordDocument : firstPhrase) {
            tempURL.add(wordDocument.url);
            webpageMap.putIfAbsent(wordDocument.url, wordDocument);
        }
        urlSets.add(tempURL);
        Set<String> tempURL2 = new HashSet<>();
        
        for (Webpage wordDocument : secondPhrase) {
            tempURL2.add(wordDocument.url);
            webpageMap.putIfAbsent(wordDocument.url, wordDocument);
        }
        urlSets.add(tempURL2);

        // Perform intersection to keep only the URLs that contain both phrases
        Set<String> intersection = new HashSet<>(urlSets.get(0));
        for (int i = 1; i < urlSets.size(); i++) {
            intersection.retainAll(urlSets.get(i));
        }

        // Finding the documents of the urls that survived the intersection
        List<Webpage> webpages = new ArrayList<>();
        for(String url : intersection) {
            webpages.add(webpageMap.get(url));
        }

        return webpages;
    }

}
