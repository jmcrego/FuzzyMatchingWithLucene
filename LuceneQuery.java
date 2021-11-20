
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.lang.*;
import java.util.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.*;
    
/***********************************************************************************/
/*** MAIN **************************************************************************/
/***********************************************************************************/

public class LuceneQuery {

    public static void main(String[] args) throws IOException, ParseException {
        String dir = "";
        String file = "";
	int n_best = 1;
	float mins = 0.0f;
	boolean fuzzymatch = false;
	boolean txt = false;
        int i = 0;
        while (i<args.length) {
            if (args[i].equals("-i") && i<args.length-1) {
                i++;
                dir = args[i];
                i++;
            }
            else if (args[i].equals("-f") && i<args.length-1) {
                i++;
                file = args[i];
                i++;
            }
            else if (args[i].equals("-n") && i<args.length-1) {
                i++;
                n_best = Integer.parseInt(args[i]);
                i++;
            }
            else if (args[i].equals("-mins") && i<args.length-1) {
                i++;
                mins = Float.parseFloat(args[i]);
                i++;
            }
            else if (args[i].equals("-fuzzymatch")) {
                i++;
		fuzzymatch = true;
	    }
            else if (args[i].equals("-txt")) {
                i++;
		txt = true;
	    }
            else {
                Exit("Bad option: "+args[0]+" Use -i OR -f");
            }
        }
        if (dir.equals(""))
            Exit("missing -i option");
        if (file.equals(""))
            Exit("missing -f option");

	Searcher idx = new Searcher(dir);
	idx.searchFile(n_best,fuzzymatch,mins,txt,file);
    }

    private static void Exit(String e){
        System.err.println("error: "+e);
        System.err.println("usage: LuceneQuery -i DIR -f FILE [-n INT] [-txt] [-fuzzymatch]");
        System.err.println("  -i      DIR : Read index in DIR");
        System.err.println("  -f     FILE : Find sentences indexed in DIR similar to sentences in FILE");
        System.err.println("  -n      INT : Returns up to INT-best similar sentences (default 1)");
        System.err.println("  -txt        : Output string of matched sentences (default false)");
        System.err.println("  -fuzzymatch : Sort using fuzzy match similarity score (default false)");
        System.err.println("  -mins FLOAT : Min score to consider a match (default 0.0)");
        System.exit(1);
    }
}

/***********************************************************************************/
/*** Searcher **********************************************************************/
/***********************************************************************************/

public class Searcher {
    IndexSearcher searcher;
    
    public Searcher(String indexDirectoryPath) throws IOException {
	System.err.println("LuceneQuery: Reading index "+indexDirectoryPath);
	QueryParser queryParser = new QueryParser("source", new StandardAnalyzer());
	File d=new File(indexDirectoryPath);
        Directory indexDirectory = FSDirectory.open(d.toPath());
	IndexReader reader = DirectoryReader.open(indexDirectory);
	searcher = new IndexSearcher(reader);
    }

    public void searchFile(int N_BEST, boolean fuzzymatch, float mins, boolean txt, String indexDataPath) throws IOException, ParseException {
	System.err.println("LuceneQuery: Searching data path " + indexDataPath );
	long startTime = System.currentTimeMillis();
        File indexFile = new File(indexDataPath);
        BufferedReader reader = Files.newBufferedReader(indexFile.toPath());
        String line;
	int nline = 0;
        while ((line = reader.readLine()) != null) {
	    BooleanQuery query = buildBooleanQuery(line);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(N_BEST, N_BEST);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    System.out.print("q="+(++nline));
	    if (txt)
		System.out.print("\t"+line);
	    //rescore by fuzzymatch score if needed
	    if (fuzzymatch) 
		hits = rescoreByFM(hits,line);
	    for (int i = 0; i < hits.length; ++i) {
		if (hits[i].score < mins)
		    break;
		int docId = hits[i].doc;
		System.out.print("\ti=" + (docId+1) + ",s=" + hits[i].score);
		if (txt) {
		    Document d = searcher.doc(docId);
		    System.out.print("\t" + d.get("source")  + "\t" + d.get("target"));
		}
	    }
	    System.out.print("\n");
	}
	long endTime = System.currentTimeMillis();
	System.err.println("LuceneQuery: Searched file with "+nline+" lines in "+(endTime-startTime)+" ms");
    }

    public ScoreDoc[] rescoreByFM(ScoreDoc[] hits, String line) throws IOException {
	String[] line_tokens = getAnalyzedTokens(line);	
	for (int i = 0; i < hits.length; ++i) {
	    Document d = searcher.doc(hits[i].doc);
	    String[] source_tokens = getAnalyzedTokens(d.get("source"));
	    float fm = 1 - ((float)editDistanceDP(line_tokens,source_tokens) / Math.max(line_tokens.length,source_tokens.length));
	    hits[i].score = fm; // replace score by fuzzymatch score
	}
	//sort again by score (fuzzy match score)
	Arrays.sort(hits, Comparator.comparing(sd -> sd.score));
	Collections.reverse(Arrays.asList(hits));
	return hits;
    }    

    public BooleanQuery buildBooleanQuery(String sentence) throws IOException {
	/* 
	   This functions returns a boolean query for the input sentence.
	   First applies the StandardAnalyzer over sentence, then each of the resulting tokens are added as terms of the query
	   Minimum number of term matches is 1
	 */
	BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
	StandardAnalyzer analyzer = new StandardAnalyzer();
	TokenStream ts= analyzer.tokenStream("source", sentence); //applies analyzer and split results into tokens
	OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
	CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
	try {
	    ts.reset();
	    while (ts.incrementToken()) {
		String term = charTermAttribute.toString();
		queryBuilder.add(new TermQuery(new Term("source", term)), BooleanClause.Occur.SHOULD);  
	    }
	    ts.end();
	} finally {
	    ts.close();
	}
	queryBuilder.setMinimumNumberShouldMatch(1);
	BooleanQuery query = queryBuilder.build();
	return query;
    }

    public String[] getAnalyzedTokens(String sentence) throws IOException {
	List<String> tokens = new ArrayList<String>();
	StandardAnalyzer analyzer = new StandardAnalyzer();
	TokenStream ts= analyzer.tokenStream("source", sentence); //applies analyzer and split results into tokens
	OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
        CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
        try {
            ts.reset();
            while (ts.incrementToken()) {
                tokens.add(charTermAttribute.toString());
            }
            ts.end();
        } finally {
            ts.close();
        }
	return tokens.toArray(new String[tokens.size()]); //convert List<String> to String[]
    }

    public int editDistanceDP(String[] s1, String[] s2) {
	/*
	System.err.println("s1: "+Arrays.toString(s1));
	System.err.println("s2: "+Arrays.toString(s2));
	*/
        int[][] solution = new int[s1.length + 1][s2.length + 1];
        for (int i = 0; i <= s2.length; i++) {
            solution[0][i] = i;
        }
        for (int i = 0; i <= s1.length; i++) {
            solution[i][0] = i;
        }
        int m = s1.length;
        int n = s2.length;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1[i - 1].equals(s2[j - 1]))
                    solution[i][j] = solution[i - 1][j - 1];
                else
                    solution[i][j] = 1 + Math.min(solution[i][j - 1], Math.min(solution[i - 1][j], solution[i - 1][j - 1]));
            }
        }
	//System.err.println("ed: "+solution[s1.length][s2.length]);
        return solution[s1.length][s2.length];
    }
}

