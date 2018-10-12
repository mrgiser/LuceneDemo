package demo.search;

/**
 * 描述: DirectoryReader.openIfChanged刷新
 * NearRealTimeSearchDemo3
 *
 * @Author HeFeng
 * @Create 2018-10-12 10:33
 */
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

public class NearRealTimeSearchDemo3 {
    public static void main(String[] args) throws IOException {
        RAMDirectory ramDirectory = new RAMDirectory();
        Document document = new Document();
        document.add(new TextField("title", "Doc1", Field.Store.YES));
        document.add(new IntPoint("ID", 1));
        IndexWriter indexWriter = new IndexWriter(ramDirectory, new IndexWriterConfig(new StandardAnalyzer()));
        indexWriter.addDocument(document);
        indexWriter.commit();
        DirectoryReader directoryReader = DirectoryReader.open(ramDirectory);
        IndexSearcher indexSearcher = new IndexSearcher(directoryReader);
        document = new Document();
        document.add(new TextField("title", "Doc2", Field.Store.YES));
        document.add(new IntPoint("ID", 2));
        indexWriter.addDocument(document);
//即使commit，搜索依然不可见，需要重新打开reader
        indexWriter.commit();
//只能看到Doc1
        int count = indexSearcher.count(new MatchAllDocsQuery());
        if (count > 0) {
            TopDocs topDocs = indexSearcher.search(new MatchAllDocsQuery(), count);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
        }
        System.out.println("=================================");

//如果发现有新数据更新，则会返回一个新的reader
        DirectoryReader newReader = DirectoryReader.openIfChanged(directoryReader);
        if (newReader != null) {
            indexSearcher = new IndexSearcher(newReader);
        }
        count = indexSearcher.count(new MatchAllDocsQuery());
        if (count > 0) {
            TopDocs topDocs = indexSearcher.search(new MatchAllDocsQuery(), count);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
        }
        System.out.println("=================================");

        document = new Document();
        document.add(new TextField("title", "Doc3", Field.Store.YES));
        document.add(new IntPoint("ID", 3));
        indexWriter.addDocument(document);
        indexWriter.commit();

        //只有索引有变化时才返回新的 reader（不是完全打开一个 new reader，会复用 old reader 的一些资源，并入新索引，降低一些开销）， 否则返回 null。
        newReader = DirectoryReader.openIfChanged(directoryReader);
        indexSearcher = new IndexSearcher(newReader);
        count = indexSearcher.count(new MatchAllDocsQuery());
        if (count > 0) {
            TopDocs topDocs = indexSearcher.search(new MatchAllDocsQuery(), count);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
        }
        System.out.println("=================================");

        directoryReader.close();
    }
}