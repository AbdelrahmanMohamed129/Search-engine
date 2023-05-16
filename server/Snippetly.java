package server;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import utils.env;
import utils.utilFunctions;

public class Snippetly {

    // ################## HELPER CLASS ################## //
    private class Snippet {
        // StringBuilder is used to allow usage of append instead of making new variable every time we need a concatenation
        StringBuilder str = new StringBuilder();
        int L, R;
    }

    // ################## MEMBER VARIABLES ################## //

    private ArrayList<Snippet> nominatedSnippets;
    private List<Snippet> selectedSnippets;
    private List<String> originalQueryStems;
    private String[] pageContentArray;
    private String content;
    private String originalQuery;

    // ################## PUBLIC METHODS ################## //

    /**
     * Extracts the important snippets from the given web page content.
     * The general steps to extract page snippet:
     *      1. Iterate over the content and get all possible snippets (stems matching)
     *      2. Sort these nominated snippets by their length descending
     *      3. Select top "10" snippets
     *      4. Sort the selected snippets by their left index ascending (to display the overall snippet with the same order as they appear in the document)
     *      5. If the page snippet is shorter than "320 characters", fill more chars after the end of the last selected snippet
     */
    public String extractWebPageSnippet(String content, String originalQuery) {
        this.content = content;
        this.originalQuery = originalQuery;

        // Process the original query string, for snippet extraction purposes
        inititalize();

        // Extract all possible snippets from the document
        getNominatedSnippets();

        // Select top snippets w.r.t. size
        getSelectedSnippets();

        // Concatenate small snippets
        StringBuilder pageSnippet = concatenateSnippets();

        // Fill the snippet in case it was so short
        return completeSnippetFilling(pageSnippet);
    }

    
    // Removes the special characters from the original query
    // also stems the query words
    private void inititalize() {
        originalQuery = utilFunctions.removeSpecialCharsAroundWord(originalQuery);

        originalQuery = originalQuery
                // .substring(0, Math.min(originalQuery.length(), Constants.QUERY_MAX_LENGTH))
                .toLowerCase();

        originalQueryStems = new ArrayList<>();

        String queryWords[] = originalQuery.split(" ");

        for (String word : queryWords) {
            originalQueryStems.add(utilFunctions.stemWord(utilFunctions.removeSpecialCharsAroundWord(word)));
        }
    }


    /**
     * If the matched word is separated by at least two words from the previously matched word, 
     * a new Snippet object is created with a left index L of the current word's index minus 2 and a right index R of the current word's index plus 2 
     * (or the end of the pageContentArray, whichever comes first). 
     * The snippetStringStartIdx variable is set to the L index of the new Snippet object. The Snippet object is then added to the nominatedSnippets list.
     * 
     * If the matched word is not separated by at least two words from the previously matched word, 
     * the Snippet object that contains the previously matched word is extended to include the current matched word. 
     * The snippetStringStartIdx variable is set to the index of the first newly added word. 
     * The Snippet object is not added to the nominatedSnippets list because it was already added previously.
     * 
     * Then "fillSnippetStr" is used to fill the Str of the used snippet and also bolds out the exact query word if found
     */
    private ArrayList<Snippet> getNominatedSnippets() {
        nominatedSnippets = new ArrayList<>();

        this.pageContentArray = content.split(" ");

        int pageContentSize = pageContentArray.length;

        int lastKeywordIdx = -3;

        for (int idx = 0; idx < pageContentSize; ++idx) {
            String word = prepareWordForSnippet(pageContentArray[idx]);

            if (originalQueryStems.indexOf(word) == -1) continue;

            Snippet snippet = new Snippet();
            Snippet lastSnippet = null;
            Integer snippetStringStartIdx;

            if (!nominatedSnippets.isEmpty())
                lastSnippet = nominatedSnippets.get(nominatedSnippets.size() - 1);

            // New snippet
            if (idx - lastKeywordIdx > 2) {
                snippet.L = Math.max(idx - 2, (lastSnippet != null ? lastSnippet.R + 1 : 0));
                snippet.R = Math.min(pageContentSize - 1, idx + 2);

                snippetStringStartIdx = snippet.L;

                nominatedSnippets.add(snippet);
            }
            // Merge snippets
            else {
                int oldLastSnippetR = lastSnippet.R = Math.min(pageContentSize - 1, idx + 2);

                snippet = lastSnippet;

                // Separate newly added words from the previous snippet.str words
                snippet.str.append(" ");

                snippetStringStartIdx = oldLastSnippetR + 1;
            }

            // Add/Update snippet words
            fillSnippetStr(snippet, snippetStringStartIdx);

            lastKeywordIdx = idx;
        }

        // Sort by snippet length desc
        nominatedSnippets.sort(Comparator.comparingInt(s -> (s.L - s.R)));

        return nominatedSnippets;
    }

    
    // Fill the Str of the used snippet and also bolds out the exact query word if found
    
    private void fillSnippetStr(Snippet snippet, int snippetStringStartIdx) {
        for (int i = snippetStringStartIdx; i <= snippet.R; ++i) {
            String tmpWord = prepareWordForSnippet(pageContentArray[i]);

            boolean isKeyword = (originalQueryStems.indexOf(tmpWord) > -1);

            snippet.str.append((isKeyword) ? "<b>" : "");
            snippet.str.append(pageContentArray[i]);
            snippet.str.append((isKeyword) ? "</b>" : "");
            snippet.str.append((i < snippet.R) ? " " : "");
        }
    }

    // Selects the top 10 nominatedSnippets and sorts them ascendingly accroding to the L to appear with the same 
    // order as they appeared in the document
    private List<Snippet> getSelectedSnippets() {
        selectedSnippets = nominatedSnippets.subList(0,
                Math.min(10, nominatedSnippets.size())
        );

        // Sort again by L to print them in order
        selectedSnippets.sort(Comparator.comparingInt(s -> s.L));

        return selectedSnippets;
    }

    // Concatenates all the selected snippets together to be in one string
    private StringBuilder concatenateSnippets() {
        StringBuilder snippet = new StringBuilder("...");

        // Concatenate to get page snippet
        for (int i = 0; i < selectedSnippets.size(); ++i) {
            snippet.append(selectedSnippets.get(i).str);
        }

        return snippet;
    }


    // If the snippet is not big enough then add some charachters at the end of the found snippet
    // also if the snippet is empty returns the first 320 character from the content
    private String completeSnippetFilling(StringBuilder snippet) {
        // If no selected snippet
        if (selectedSnippets.size() == 0) {
            int len = Math.min(content.length(), 320);
            return content.substring(0, len);
        }

        // Fill more to show full-like snippet
        int index = selectedSnippets.get(selectedSnippets.size() - 1).R + 1;

        while (snippet.length() < 320 && index < pageContentArray.length) {
            snippet.append(" ").append(pageContentArray[index++]);
        }

        return snippet.toString();
    }

    
    // ################## UTILITY METHODS ################## //
    
    // All special characters are removed, lowercased and stemmed
    private String prepareWordForSnippet(String word) {
        return utilFunctions.stemWord(utilFunctions.removeSpecialCharsAroundWord(word)).toLowerCase();
    }

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
