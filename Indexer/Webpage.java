package indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import utils.env;

public class Webpage {

    public ObjectId _id;
    public String url;
    public String title;
    public String pageData;
    public double rank = 1;
    public int totalWords = 0;

    public List<String> outlinks;

    public HashMap<String,List<Integer>> terms;
    public HashMap<String,Stem> stems;

    public Webpage(Document document) {
        if (document == null) return;

        _id = (ObjectId) document.getOrDefault(env.FIELD_ID, null);
        url = (String) document.getOrDefault(env.FIELD_URL, null);
        title = (String) document.getOrDefault(env.FIELD_TITLE, null);
        pageData = (String) document.getOrDefault(env.FIELD_PAGE_DATA, null);
        rank = (double) document.getOrDefault(env.FIELD_RANK, 1.0);
        totalWords = (int) document.getOrDefault(env.FIELD_TOTAL_WORDS, 0);


        outlinks = (List<String>) document.getOrDefault(env.FIELD_OUTLINKS, null);

        setTermIndex((List<Document>) document.getOrDefault(env.FIELD_TERM_INDEX, null));
        setStemIndex((List<Document>) document.getOrDefault(env.FIELD_STEM_INDEX, null));

    }

    public Webpage() {

    }

    public Document convertToDocument() {
        Document document = new Document();
        document.append(env.FIELD_URL, url);
        document.append(env.FIELD_TITLE, title);
        document.append(env.FIELD_PAGE_DATA, pageData);
        document.append(env.FIELD_TOTAL_WORDS, totalWords);
        document.append(env.FIELD_RANK, rank);
        document.append(env.FIELD_OUTLINKS, outlinks);
        document.append(env.FIELD_TERM_INDEX, createTermIndex());
        document.append(env.FIELD_STEM_INDEX, createStemIndex());
        return document;
    }

    public List<Document> createTermIndex() {
        if(terms==null) return null;
        List<Document> termIndex = new ArrayList<>();
        for (HashMap.Entry<String, List<Integer>> entry : terms.entrySet()) {
            Document currentDocument = new Document();
            currentDocument.append(env.FIELD_TERM, entry.getKey());
            currentDocument.append(env.FIELD_TERM_POSITIONS, entry.getValue());
            termIndex.add(currentDocument);
        }
        return termIndex;
    }

    public void setTermIndex(List<Document> termIndex) {
        if(termIndex==null) return;

        terms = new HashMap<>();
        for (Document document : termIndex) {
            String term = document.getString(env.FIELD_TERM);
            terms.put(term, (List<Integer>) document.get(env.FIELD_TERM_POSITIONS));
        }
    }

    public List<Document> createStemIndex() {
        if(stems==null) return null;
        List<Document> stemIndex = new ArrayList<>();
        for (HashMap.Entry<String, Stem> entry : stems.entrySet()) {
            Document currentDocument = new Document();
            currentDocument.append(env.FIELD_TERM, entry.getKey());
            currentDocument.append(env.FIELD_STEM_COUNT, entry.getValue().count);
            currentDocument.append(env.FIELD_STEM_SCORE, entry.getValue().score);
            stemIndex.add(currentDocument);
        }
        return stemIndex;
    }

    public void setStemIndex(List<Document> stemIndex) {
        if(stemIndex==null) return;

        stems = new HashMap<>();
        for (Document document : stemIndex) {
            String stem = document.getString(env.FIELD_TERM);
            Integer stemCount = document.getInteger(env.FIELD_STEM_COUNT);
            Integer stemScore = document.getInteger(env.FIELD_STEM_SCORE);
            stems.put( stem, new Stem(stemCount,stemScore));
        }
    }

}