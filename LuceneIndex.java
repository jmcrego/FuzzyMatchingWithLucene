
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/***********************************************************************************/
/*** MAIN **************************************************************************/
/***********************************************************************************/

public class LuceneIndex {
    
    public static void main(String[] args) throws IOException {
	String dir = "";
	String file = "";
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
	    else {
		Exit("Bad option: "+args[0]+" Use -i OR -f");
	    }
	}
	if (dir.equals(""))
	    Exit("missing -i option");
	if (file.equals(""))
	    Exit("missing -f option");
	
	String[] source_targets = file.split(",");
	if (source_targets.length >= 1) {
	    Indexer idx = new Indexer(dir);
	    idx.indexFile(dir,source_targets);
	}
	else {
	    Exit("Bad -f option: use at least 2 fields");
	}
    }

    private static void Exit(String e){
	System.err.println("error: "+e);
	System.err.println("usage: LuceneIndex -i DIR -f FILE[,FPAR]*");
	System.err.println("  -i         DIR : Create indexs in DIR");
	System.err.println("  -f FILE[,FPAR] : Build an index in DIR using FILE for indexing and storing additional FPAR's parallel files (removes previous if exists)");
	System.exit(1);
    }
}

/***********************************************************************************/
/*** Indexer ***********************************************************************/
/***********************************************************************************/

public class Indexer {
    private String indexDirectoryPath;
    
    private void progress(int n){
	if (n%1000000 == 0)
	    System.err.print(n);
	else if (n%100000 == 0)
	    System.err.print(".");
    }

    public Indexer(String dir) {
	indexDirectoryPath = dir;
    }

    public void indexFile(String indexDirectoryPath, String[] src_tgts) throws IOException {
	IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
	conf.setOpenMode(OpenMode.CREATE);
	File d=new File(indexDirectoryPath);
	System.err.println("LuceneIndex: Creating "+d.getAbsolutePath());	
	Directory indexDirectory = FSDirectory.open(d.toPath());
	IndexWriter writer = new IndexWriter(indexDirectory, conf);

	long startTime = System.currentTimeMillis();
	BufferedReader[] readerFiles = new BufferedReader[src_tgts.length];
	for (int i=0 ; i<src_tgts.length; i++) {
	    File indexFile = new File(src_tgts[i]);
	    System.err.println("LuceneIndex: Opening (index="+(i==0)+") "+indexFile.getAbsolutePath());
	    readerFiles[i] = Files.newBufferedReader(indexFile.toPath());
	}
	int nline = 0;
	String[] lines = new String[readerFiles.length];
	boolean end = false;
	while (!end) {
	    for (int i=0 ; i<readerFiles.length && !end; i++) {
		lines[i] = readerFiles[i].readLine();
		if (lines[i] == null)
		    end = true;
	    }
	    if (!end) {
		writer.addDocument(buildDocument(lines,++nline));
		progress(nline);
	    }
	}
	writer.commit();
	long endTime = System.currentTimeMillis();
	long msec = endTime-startTime;
	System.err.println("\nLuceneIndex: indexed "+nline+" sentences in "+String.format("%.2f",(float)msec/1000)+" sec ("+String.format("%.2f",(float)1000*nline/msec)+" sents/sec)");
	writer.close();
    }

    private Document buildDocument(String[] lines, int nline) {
	Document doc = new Document();
	TextField source = new TextField("source", lines[0], Field.Store.YES);  //analyzed using StandardAnalyzer, indexed, stored for retrieval
	doc.add(source);
	StoredField position = new StoredField("position", String.valueOf(nline)); //not analysed, not indexed, only stored for retrieval
	doc.add(position);
	if (lines.length >= 2) {
	    String t = lines[1];
	    for (int i=2; i<lines.length; i++) {
		t += "\t" + lines[i];
	    }
	    StoredField target = new StoredField("target", t); //not analysed, not indexed, only stored for retrieval
	    doc.add(target);
	}
	return doc;
    }
}
