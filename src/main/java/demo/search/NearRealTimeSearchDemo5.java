package demo.search;

/**
 * 描述: 使用定时线程执行刷新 另外实现方式参考demo2程序
 * NearRealTimeSearchDemo5
 *
 * @Author HeFeng
 * @Create 2018-10-12 10:47
 */
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NearRealTimeSearchDemo5 {
    private static SearcherManager searcherManager;

    public static void main(String[] args) throws IOException {
        //定时任务
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new RefreshThread(), 1, 1, TimeUnit.SECONDS);

        RAMDirectory ramDirectory = new RAMDirectory();
        Document document = new Document();
        document.add(new TextField("title", "Doc1", Field.Store.YES));
        document.add(new IntPoint("ID", 1));
        IndexWriter indexWriter = new IndexWriter(ramDirectory, new IndexWriterConfig(new StandardAnalyzer()));
        indexWriter.addDocument(document);
        indexWriter.commit();

        searcherManager = new SearcherManager(ramDirectory, null);
        document = new Document();
        document.add(new TextField("title", "Doc2", Field.Store.YES));
        document.add(new IntPoint("ID", 2));
        indexWriter.addDocument(document);
//即使commit，搜索依然不可见，需要重新打开reader
        indexWriter.commit();
//只能看到Doc1
        IndexSearcher indexSearcher = searcherManager.acquire();
        int count = indexSearcher.count(new MatchAllDocsQuery());
        if (count > 0) {
            TopDocs topDocs = indexSearcher.search(new MatchAllDocsQuery(), count);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
        }
        System.out.println("=================================");
//最好放到finally语句块中
        searcherManager.release(indexSearcher);

//休息2s中，定时线程应该已经刷新了
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        indexSearcher = searcherManager.acquire();
        count = indexSearcher.count(new MatchAllDocsQuery());
        if (count > 0) {
            TopDocs topDocs = indexSearcher.search(new MatchAllDocsQuery(), count);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
        }
        System.out.println("=================================");
        searcherManager.release(indexSearcher);
    }

    static class RefreshThread implements Runnable {
        @Override
        public void run() {
            try {
                searcherManager.maybeRefresh();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}