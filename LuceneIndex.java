
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
	List<String> files = new ArrayList<String>();
	int i = 0;
	while (i<args.length) {
	    if (args[i].equals("-i") && i<args.length-1) {
		i++;
		dir = args[i];
		i++;
	    }
	    else if (args[i].equals("-f") && i<args.length-1) {
		i++;
		files.add(args[i]);
		i++;
	    }
	    else {
		Exit("Bad option: "+args[0]+" Use -i OR -f");
	    }
	}
	if (dir.equals(""))
	    Exit("missing -i option");
	if (files.size() == 0)
	    Exit("missing -f option/s");
	
	for (String file : files) {
	    String[] tm_source_targets = file.split(",");
	    if (tm_source_targets.length >= 2) {
		Indexer idx = new Indexer(dir);
		idx.indexFile(dir,tm_source_targets);
	    }
	    else {
		Exit("Bad -f option: use at least 2 fields");
	    }
	}
    }

    private static void Exit(String e){
	System.err.println("error: "+e);
	System.err.println("usage: LuceneIndex -i DIR [-f TM,FILE0[,FILE1]*]+");
	System.err.println("  -i             DIR : Create indexs in DIR");
	System.err.println("  -f  TM,FILE[,FPAR] : Build an index in DIR/TM (removes previous if exists) using FILE for indexing and storing additional FPAR's parallel files");
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

    public void indexFile(String indexDirectoryPath, String[] desc) throws IOException {
	//System.err.println("LuceneIndex: Building index "+indexDirectoryPath+"/"+desc[0]);
	IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
	conf.setOpenMode(OpenMode.CREATE);
	File d=new File(indexDirectoryPath+"/"+desc[0]);
	Directory indexDirectory = FSDirectory.open(d.toPath());
	IndexWriter writer = new IndexWriter(indexDirectory, conf);

	System.err.println("LuceneIndex: Reading "+desc[0]);
	long startTime = System.currentTimeMillis();
	BufferedReader[] readerFiles = new BufferedReader[desc.length-1];
	for (int i=0 ; i<desc.length-1; i++) {
	    System.err.print("LuceneIndex: Opening "+desc[i+1]);
	    File indexFile = new File(desc[i+1]);
	    readerFiles[i] = Files.newBufferedReader(indexFile.toPath());
	}
	int nline = 0;
	String[] lines = new String[desc.length-1];
	boolean end = false;
	while (!end) {
	    for (int i=0 ; i<desc.length-1 && !end; i++) {
		lines[i] = readerFiles[i].readLine();
		if (lines[i] == null)
		    end = true;
	    }
	    if (!end) {
		writer.addDocument(buildDocument(desc[0],lines,++nline));
		progress(nline);
	    }
	}
	writer.commit();
	long endTime = System.currentTimeMillis();
	long msec = endTime-startTime;
	System.err.println(" found "+nline+" sentences in "+String.format("%.2f",(float)msec/100)+" sec [index contains "+writer.getDocStats().maxDoc+" sentences]");
	writer.close();
    }

    private Document buildDocument(String desc, String[] lines, int nline) {
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
