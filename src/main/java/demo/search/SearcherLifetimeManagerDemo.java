package demo.search;

/**
 * 描述:为了保证一个良好的用户体验，
 * 需要对用户在一个搜索会话期间分页的时候使用同样一个 IndexSearcher，
 * 这样在分页期间，可以保证搜索结果的排序是稳定不变的，
 * 从而用户也不会再次看到同样的内容。
 * SearcherLifetimeManagerDemo
 *
 * @Author HeFeng
 * @Create 2018-10-12 10:59
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
import org.apache.lucene.search.SearcherLifetimeManager;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

public class SearcherLifetimeManagerDemo {

    public static void main(String[] args) throws IOException {

        SearcherLifetimeManager searcherLifetimeManager = new SearcherLifetimeManager();

        RAMDirectory ramDirectory = new RAMDirectory();
        Document document = new Document();
        document.add(new TextField("title", "Doc1", Field.Store.YES));
        document.add(new IntPoint("ID", 1));

        IndexWriter indexWriter = new IndexWriter(ramDirectory, new IndexWriterConfig(new StandardAnalyzer()));
        indexWriter.addDocument(document);
        indexWriter.commit();

        document = new Document();
        document.add(new TextField("title", "Doc2", Field.Store.YES));
        document.add(new IntPoint("ID", 2));

        indexWriter.addDocument(document);
        indexWriter.commit();

        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(ramDirectory));
//记录当前的searcher，保存token，当有后续的搜索请求到来，例如用户翻页，那么用这个token去获取对应的那个searcher
        long record = searcherLifetimeManager.record(indexSearcher);
        indexSearcher = searcherLifetimeManager.acquire(record);
        if (indexSearcher != null) {
// Searcher is still here
            try {
// do searching...
            } finally {
                searcherLifetimeManager.release(indexSearcher);
// Do not use searcher after this!
                indexSearcher = null;
            }
        } else {
// Searcher was pruned -- notify user session timed out, or, pull fresh searcher again
        }
//由于保留许多的searcher是非常耗系统资源的，包括打开发files和RAM，所以最好在一个单独的线程中，定期的重新打开searcher和定时的去清理旧的searcher
//丢弃所有比指定的时间都老的searcher
        searcherLifetimeManager.prune(new SearcherLifetimeManager.PruneByAge(600.0));
    }
}