package utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;

public class utilFunctions {

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


}
