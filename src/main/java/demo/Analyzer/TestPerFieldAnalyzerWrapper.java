package demo.Analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述:
 * PerFieldAnalyzerWrapper该类提供处理不同的Field使用不同的Analyzer的技术方案
 *
 * @Author HeFeng
 * @Create 2018-10-10 15:21
 */
public class TestPerFieldAnalyzerWrapper {
    public static final Logger logger = LoggerFactory.getLogger(TestPerFieldAnalyzerWrapper.class);

    @Test
    public void testPerFieldAnalyzerWrapper() throws IOException, ParseException {

        Map<String, Analyzer> fields = new HashMap<>();
        fields.put("partnum", new KeywordAnalyzer());
        //对于其他的域，默认使用SimpleAnalyzer分析器，对于指定的域partnum使用KeywordAnalyzer
        PerFieldAnalyzerWrapper perFieldAnalyzerWrapper = new PerFieldAnalyzerWrapper(new SimpleAnalyzer(), fields);

        Directory directory = new RAMDirectory();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(perFieldAnalyzerWrapper);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        Document document = new Document();
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        document.add(new Field("partnum", "Q36", fieldType));
        document.add(new Field("description", "Illidium Space Modulator", fieldType));
        indexWriter.addDocument(document);
        indexWriter.close();

        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(directory));
        //直接使用TermQuery是可以检索到的
        TopDocs search = indexSearcher.search(new TermQuery(new Term("partnum", "Q36")), 10);
        Assert.assertEquals(1, search.totalHits);
        //如果使用QueryParser，那么必须要使用PerFieldAnalyzerWrapper，否则如下所示，是检索不到的
        Query description = new QueryParser("description", new SimpleAnalyzer()).parse("partnum:Q36 AND SPACE");
        search = indexSearcher.search(description, 10);
        Assert.assertEquals(0, search.totalHits);
        logger.info("SimpleAnalyzer :" + description.toString());//+partnum:q +description:space，原因是SimpleAnalyzer会剥离非字母字符并将字母小写化

        //使用PerFieldAnalyzerWrapper可以检索到
        //partnum:Q36 AND SPACE表示在partnum中出现Q36，在description中出现SPACE
        description = new QueryParser("description", perFieldAnalyzerWrapper).parse("partnum:Q36 AND SPACE");
        search = indexSearcher.search(description, 10);
        Assert.assertEquals(1, search.totalHits);
        logger.info("(SimpleAnalyzer,KeywordAnalyzer) :" + description.toString());//+partnum:Q36 +description:space
    }

}