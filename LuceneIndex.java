
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
	
	Indexer idx = new Indexer(dir);
	for (String file : files) {
	    String[] source_target = file.split(",");
	    if (source_target.length == 2) {
		idx.indexFiles(source_target[0],source_target[1]);
	    }
	    else {
		idx.indexFile(file);
	    }
	}
	idx.close();
    }

    private static void Exit(String e){
	System.err.println("error: "+e);
	System.err.println("usage: LuceneIndex -i DIR [-f FILE]+");
	System.err.println("  -i   DIR : Create index in DIR (removes previous index if exists)");
	System.err.println("  -f  FILE : Index sentence pairs in FILE (two sentences per line separated by TAB or use two parallel FILEs comma-separated)");
	System.exit(1);
    }

}

/***********************************************************************************/
/*** Indexer ***********************************************************************/
/***********************************************************************************/

public class Indexer {
    private IndexWriter writer;

    public Indexer(String indexDirectoryPath) throws IOException {
	System.err.println("LuceneIndex: Building index "+indexDirectoryPath);
	IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
	conf.setOpenMode(OpenMode.CREATE);
	File d=new File(indexDirectoryPath);
	Directory indexDirectory = FSDirectory.open(d.toPath());
	writer = new IndexWriter(indexDirectory, conf);
    }

    public void indexFile(String indexDataPath) throws IOException {
	System.err.println("LuceneIndex: Reading data from "+indexDataPath);
	long startTime = System.currentTimeMillis();
	File indexFile = new File(indexDataPath);
	BufferedReader reader = Files.newBufferedReader(indexFile.toPath());
	String line;
	int nline = 0;
	while ((line = reader.readLine()) != null) {
	    String[] source_target = line.split("\t");
	    if (source_target.length != 2) {
		System.err.println("LuceneIndex: Bad sentence pair at line "+nline+": "+line);
		System.exit(1);
	    }
	    writer.addDocument(buildDocument(source_target[0], source_target[1], ++nline));
	}
	writer.commit();
	long endTime = System.currentTimeMillis();
	System.err.println("LuceneIndex: Created index with "+writer.getDocStats().maxDoc+" sentences in "+(endTime-startTime)+" ms");
    }

    public void indexFiles(String indexDataPathSrc, String indexDataPathTgt) throws IOException {
       System.err.println("LuceneIndex: Reading data from "+indexDataPathSrc+" AND "+indexDataPathTgt);
       long startTime = System.currentTimeMillis();
       File indexFileSrc = new File(indexDataPathSrc);
       File indexFileTgt = new File(indexDataPathTgt);
       BufferedReader readerSrc = Files.newBufferedReader(indexFileSrc.toPath());
       BufferedReader readerTgt = Files.newBufferedReader(indexFileTgt.toPath());
       String lineSrc, lineTgt;
       int nline = 0;
       while ( ((lineSrc = readerSrc.readLine()) != null) && ((lineTgt = readerTgt.readLine()) != null) ) {
	   writer.addDocument(buildDocument(lineSrc, lineTgt, ++nline));
       }
       writer.commit();
       long endTime = System.currentTimeMillis();
       int nsents = writer.getDocStats().maxDoc;
       long msec = endTime-startTime;
       System.err.println("LuceneIndex: Created index with "+nsents+" sentences in "+msec+" ms ("+((float)1000*nsents/msec)+" sents/sec)");
    }
    
    private Document buildDocument(String lsrc, String ltgt, int nline) {
	Document doc = new Document();
	TextField source = new TextField("source", lsrc, Field.Store.YES);  //analyzed using StandardAnalyzer, stored for search
	StringField target = new StringField("target", ltgt, Field.Store.YES); //not analysed, stored for search
	StringField position = new StringField("position", String.valueOf(nline), Field.Store.YES); //not analysed, stored for search
	doc.add(source);
	doc.add(target);
	doc.add(position);
	return doc;
    }
    
    public void close() throws CorruptIndexException, IOException {
	writer.close();
    }
}
