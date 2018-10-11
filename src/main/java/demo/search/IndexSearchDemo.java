package demo.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * 描述:
 * IndexSearchDemo
 *
 * @Author HeFeng
 * @Create 2018-10-11 10:59
 */

public class IndexSearchDemo {
    public static final Logger logger = LoggerFactory.getLogger(IndexSearchDemo.class);

    private Directory directory = new RAMDirectory();
    private String[] ids = {"1", "2"};
    private String[] countries = {"Netherlands", "Italy"};
    private String[] contents = {"Amsterdam has lots of bridges", "Venice has lots of canals, not bridges"};
    private String[] cities = {"Amsterdam", "Venice"};
    private IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new WhitespaceAnalyzer());
    private IndexWriter indexWriter;

    @Before
    public void createIndex() {
        try {
            indexWriter = new IndexWriter(directory, indexWriterConfig);
            for (int i = 0; i < 2; i++) {
                Document document = new Document();
                Field idField = new StringField("id", ids[i], Field.Store.YES);
                Field countryField = new StringField("country", countries[i], Field.Store.YES);
                Field contentField = new TextField("content", contents[i], Field.Store.NO);
                Field cityField = new StringField("city", cities[i], Field.Store.YES);
                document.add(idField);
                document.add(countryField);
                document.add(contentField);
                document.add(cityField);
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTermQuery() throws IOException {
        Term term = new Term("id", "2");
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(new TermQuery(term), 10);
        Assert.assertEquals(1, search.totalHits);
    }

    @Test
    public void testMatchNoDocsQuery() throws IOException {
        Query query = new MatchNoDocsQuery();
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(0, search.totalHits);
    }

    @Test
    public void testTermRangeQuery() throws IOException {
        //搜索起始字母范围从A到Z的city
        Query query = new TermRangeQuery("city", new BytesRef("A"), new BytesRef("Z"), true, true);
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(2, search.totalHits);
    }

    @Test
    public void testQueryParser() throws ParseException, IOException {
        //使用WhitespaceAnalyzer分析器不会忽略大小写，也就是说大小写敏感
        QueryParser queryParser = new QueryParser("content", new WhitespaceAnalyzer());
        Query query = queryParser.parse("+lots +has");
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 1);
        Assert.assertEquals(2, search.totalHits);
        query = queryParser.parse("lots OR bridges");
        search = indexSearcher.search(query, 10);
        Assert.assertEquals(2, search.totalHits);

        //有点需要注意，在QueryParser解析通配符表达式的时候，一定要用引号包起来，然后作为字符串传递给parse函数
        query = new QueryParser("field", new StandardAnalyzer()).parse("\"This is some phrase*\"");
        Assert.assertEquals("analyzed", "\"? ? some phrase\"", query.toString("field"));

        //语法参考：http://lucene.apache.org/core/6_0_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
        //使用QueryParser解析"~"，~代表编辑距离，~后面参数的取值在0-2之间，默认值是2，不要使用浮点数
        QueryParser parser = new QueryParser("city", new WhitespaceAnalyzer());
        //例如，roam~，该查询会匹配foam和roams，如果~后不跟参数，则默认值是2
        //QueryParser在解析的时候不区分大小写（会全部转成小写字母），所以虽少了一个字母，但是首字母被解析为小写的v，依然不匹配，所以编辑距离是2
        query = parser.parse("Venic~2");
        search = indexSearcher.search(query, 10);
        Assert.assertEquals(1, search.totalHits);
    }

    @Test
    public void testBooleanQuery() throws IOException {
        Query termQuery = new TermQuery(new Term("country", "Beijing"));
        Query termQuery1 = new TermQuery(new Term("city", "Venice"));
        //测试OR查询，或者出现Beijing或者出现Venice
        BooleanQuery build = new BooleanQuery.Builder().add(termQuery, BooleanClause.Occur.SHOULD).add(termQuery1, BooleanClause.Occur.SHOULD).build();
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(build, 10);
        Assert.assertEquals(1, search.totalHits);
        //使用BooleanQuery实现 国家是(Italy OR Netherlands) AND contents中包含(Amsterdam)操作
        BooleanQuery build1 = new BooleanQuery.Builder().add(new TermQuery(new Term("country", "Italy")), BooleanClause.Occur.SHOULD).add(new TermQuery
                (new Term("country",
                        "Netherlands")), BooleanClause.Occur.SHOULD).build();
        BooleanQuery build2 = new BooleanQuery.Builder().add(build1, BooleanClause.Occur.MUST).add(new TermQuery(new Term("content", "Amsterdam")), BooleanClause.Occur
                .MUST).build();
        search = indexSearcher.search(build2, 10);
        Assert.assertEquals(1, search.totalHits);
    }

    @Test
    public void testPhraseQuery() throws IOException {
        //设置两个短语之间的跨度为2，也就是说has和bridges之间的短语小于等于均可检索到
        PhraseQuery build = new PhraseQuery.Builder().setSlop(2).add(new Term("content", "has")).add(new Term("content", "bridges")).build();
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(build, 10);
        Assert.assertEquals(1, search.totalHits);
        build = new PhraseQuery.Builder().setSlop(1).add(new Term("content", "Venice")).add(new Term("content", "lots")).add(new Term("content",
                "canals")).build();
        search = indexSearcher.search(build, 10);
        Assert.assertNotEquals(1, search.totalHits);
    }

    @Test
    public void testMatchAllDocsQuery() throws IOException {
        Query query = new MatchAllDocsQuery();
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(2, search.totalHits);
    }

    @Test
    public void testFuzzyQuery() throws IOException, ParseException {
        //注意是区分大小写的，同时默认的编辑距离的值是2
        //注意两个Term之间的编辑距离必须小于两者中最小者的长度：the edit distance between the terms must be less than the minimum length term
        Query query = new FuzzyQuery(new Term("city", "Veni"));
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(1, search.totalHits);
    }

    @Test
    public void testWildcardQuery() throws IOException {
        //*代表0个或者多个字母
        Query query = new WildcardQuery(new Term("content", "*dam"));
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(1, search.totalHits);
        //?代表0个或者1个字母
        query = new WildcardQuery(new Term("content", "?ridges"));
        search = indexSearcher.search(query, 10);
        Assert.assertEquals(2, search.totalHits);
        query = new WildcardQuery(new Term("content", "b*s"));
        search = indexSearcher.search(query, 10);
        Assert.assertEquals(2, search.totalHits);
    }

    @Test
    public void testPrefixQuery() throws IOException {
        //使用前缀搜索以It打头的国家名，显然只有Italy符合
        PrefixQuery query = new PrefixQuery(new Term("country", "It"));
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(query, 10);
        Assert.assertEquals(1, search.totalHits);
    }

    private IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(directory));
    }

    @Test
    public void testToken() throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream
                tokenStream = analyzer.tokenStream("myfield", new StringReader("Some text content for my test!"));
        OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            logger.info("token: " + tokenStream.reflectAsString(true).toString());
            logger.info("token start offset: " + offsetAttribute.startOffset());
            logger.info("token end offset: " + offsetAttribute.endOffset());
        }
    }

    @Test
    public void testMultiPhraseQuery() throws IOException {
        Term[] terms = new Term[]{new Term("content", "has"), new Term("content", "lots")};
        Term term2 = new Term("content", "bridges");
        //多个add之间认为是OR操作，即(has lots)和bridges之间的slop不大于3，不计算标点
        MultiPhraseQuery multiPhraseQuery = new MultiPhraseQuery.Builder().add(terms).add(term2).setSlop(3).build();
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(directory));
        TopDocs search = indexSearcher.search(multiPhraseQuery, 10);
        Assert.assertEquals(2, search.totalHits);
    }

    //使用BooleanQuery类模拟MultiPhraseQuery类的功能
    @Test
    public void testBooleanQueryImitateMultiPhraseQuery() throws IOException {
        PhraseQuery first = new PhraseQuery.Builder().setSlop(3).add(new Term("content", "Amsterdam")).add(new Term("content", "bridges"))
                .build();
        PhraseQuery second = new PhraseQuery.Builder().setSlop(1).add(new Term("content", "Venice")).add(new Term("content", "lots")).build();
        BooleanQuery booleanQuery = new BooleanQuery.Builder().add(first, BooleanClause.Occur.SHOULD).add(second, BooleanClause.Occur.SHOULD).build();
        IndexSearcher indexSearcher = getIndexSearcher();
        TopDocs search = indexSearcher.search(booleanQuery, 10);
        Assert.assertEquals(2, search.totalHits);
    }
}