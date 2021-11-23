
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
	List<String> dirs = new ArrayList<String>();
        String file = "";
	int n_best = 1;
	float mins = 0.0f;
	boolean fuzzymatch = false;
	boolean query = false;
	boolean match = false;
	boolean noperfect = false;
	//	List<String> tms = new ArrayList<String>();
        int i = 0;
        while (i<args.length) {
            if (args[i].equals("-i") && i<args.length-1) {
                i++;
                dirs.add(args[i]);
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
            else if (args[i].equals("-query")) {
                i++;
		query = true;
	    }
            else if (args[i].equals("-match")) {
                i++;
		match = true;
	    }
            else if (args[i].equals("-noperfect")) {
                i++;
		noperfect = true;
	    }
            else {
                Exit("Bad option: "+args[0]+" Use -i OR -f");
            }
        }
        if (dirs.size() == 0)
            Exit("missing -i option");
        if (file.equals(""))
            Exit("missing -f option");

	Searchers idxs = new Searchers(dirs);
	idxs.searchFile(n_best,fuzzymatch,mins,noperfect,query,match,file);
    }

    private static void Exit(String e){
        System.err.println("error: "+e);
        System.err.println("usage: LuceneQuery [-i DIR]+ -f FILE [-n INT] [-query] [-match] [-fuzzymatch] [-noperfect] [-mins FLOAT]");
        System.err.println("  -i       DIR : Read TM index in DIR");
        System.err.println("  -f      FILE : Find sentences of TM indexed in DIR similar to sentences in FILE");
        System.err.println("  -n       INT : Returns up to INT-best similar sentences (default 1)");
        System.err.println("  -query       : Output string corresponding to queried sentences");
        System.err.println("  -match       : Output string corresponding to matched sentences in index");
        System.err.println("  -fuzzymatch  : Sort n-best list using fuzzy match similarity score (use with -n)");
        System.err.println("  -noperfect   : Do not consider perfect matches");
        System.err.println("  -mins  FLOAT : Min score to consider a match (default 0.0)");
        System.exit(1);
    }
}

/***********************************************************************************/
/*** Searcher **********************************************************************/
/***********************************************************************************/

public class Hit {
    String tm;
    String pos;
    String src;
    String tgt;
    float score;

    public Hit(String mytm, String mypos, String mysrc, String mytgt, float myscore) {
	tm = mytm;
	pos = mypos;
	src = mysrc;
	tgt = mytgt;
	score = myscore;
    }
}

public class Searchers {
    Map<String, IndexSearcher> searchers = new HashMap<String, IndexSearcher>();
    
    public Searchers(List<String> dirs) throws IOException {
	for (String dir : dirs) {
	    File d=new File(dir);
	    System.err.println("LuceneQuery: Opening TM "+d.getName()+" : "+dir);
	    Directory indexDirectory = FSDirectory.open(d.toPath());
	    IndexReader reader = DirectoryReader.open(indexDirectory);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    searchers.put(d.getName(),searcher);
	}
    }

    public void searchFile(int N_BEST, boolean fuzzymatch, float mins, boolean noperfect, boolean query,boolean match,String dataPath) throws IOException, ParseException {
	System.err.println("LuceneQuery: Searching file "+dataPath);
	long startTime = System.currentTimeMillis();
        File indexFile = new File(dataPath);
        BufferedReader reader = Files.newBufferedReader(indexFile.toPath());
        String line;
	int nline = 0;
        while ((line = reader.readLine()) != null) {
	    List<Hit> allhits = new ArrayList<Hit>();
	    for (var entry : searchers.entrySet()) {
		String tm = entry.getKey();
		IndexSearcher searcher = entry.getValue();
		BooleanQuery booleanQuery = buildBooleanQuery(line,tm);
		TopScoreDocCollector collector = TopScoreDocCollector.create(N_BEST, N_BEST);
		searcher.search(booleanQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; ++i) {
		    int docId = hits[i].doc;
		    Document d = searcher.doc(docId);
		    float score = fuzzymatch ? rescoreByFM(d.get("source"),line) : hits[i].score;
		    allhits.add(new Hit(tm,d.get("position"),d.get("source"),d.get("target"),score));
		}
	    }
	    //sorting allhits by score
	    Hit[] hits = allhits.toArray(new Hit[allhits.size()]); //to sort i need an array not a list
	    Arrays.sort(hits, Comparator.comparing(h -> h.score));
	    Collections.reverse(Arrays.asList(hits));	    
	    String out = "";
	    if (query)
		out = line;
	    int N = 0;
	    for (int i = 0; i < hits.length; ++i) {
		if (N >= N_BEST) //histogram pruning
		    break;
		if (hits[i].score < mins) //threshold pruning
		    break;
 		if (noperfect && line.equals(hits[i].src)) { //perfect match pruning
		    continue;
		}
		N++;
		if (!out.equals(""))
		    out += "\t";
		out += hits[i].tm + ":" + hits[i].pos + ":" + String.format("%.6f",hits[i].score);
		if (match) {
		    out += "\t" + hits[i].src;
		    if (hits[i].tgt != null) 
			out += "\t" + hits[i].tgt;
		}
	    }
	    System.out.println(out);
	    nline++;
	}
	long endTime = System.currentTimeMillis();
	long msec = endTime-startTime;
	System.err.println("LuceneQuery: Searched file with "+nline+" sentences in "+String.format("%.2f",(float)msec/1000)+" sec ("+String.format("%.2f",(float)1000*nline/msec)+" sents/sec)");
    }

    float rescoreByFM(String line, String source) throws IOException {
	String[] line_tokens = getAnalyzedTokens(line);	
	String[] source_tokens = getAnalyzedTokens(source);
	float fm = 1 - ((float)editDistanceDP(line_tokens,source_tokens) / Math.max(line_tokens.length,source_tokens.length));
	return fm;
    }    

    public BooleanQuery buildBooleanQuery(String sentence, String tm) throws IOException {
	/* 
	   This functions returns a boolean query for the input sentence.
	   First applies the StandardAnalyzer over sentence, then each of the resulting tokens are added as terms of the query
	   Minimum number of term matches is 1
	 */
	int minmatch = 1;
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
	queryBuilder.setMinimumNumberShouldMatch(minmatch);
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

