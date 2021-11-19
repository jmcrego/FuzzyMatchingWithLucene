
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.Files;

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
	idx.searchFile(n_best,txt,file);
    }

    private static void Exit(String e){
        System.err.println("error: "+e);
        System.err.println("usage: LuceneQuery -i DIR -f FILE [-n INT] [-txt]");
        System.err.println("  -i  DIR : Read index in DIR");
        System.err.println("  -f FILE : Find similar sentences indexed in DIR for sentences in FILE");
        System.err.println("  -n  INT : Returns up to INT-best similar sentences (default 1)");
        System.err.println("  -txt    : Output matched sentences (default false)");
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

    public void searchFile(int N_BEST, boolean txt, String indexDataPath) throws IOException, ParseException {
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
	    for (int i = 0; i < hits.length; ++i) {
		int docId = hits[i].doc;
		Document d = searcher.doc(docId);
		System.out.print("\ti=" + (docId+1) + ",s=" + hits[i].score);
		if (txt)
		    System.out.print("\t" + d.get("source")  + "\t" + d.get("target"));
	    }
	    System.out.print("\n");
	}
	long endTime = System.currentTimeMillis();
	System.err.println("LuceneQuery: Searched file with "+nline+" lines in "+(endTime-startTime)+" ms");
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
}
