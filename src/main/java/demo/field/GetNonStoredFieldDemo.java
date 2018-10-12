package demo.field;

/**
 * 描述: 获取没有存储的字段值的几种方法
 * GetNonStoredFieldDemo
 *
 * testGetFieldByStore 演示存储Field值时如何获取
 testGetFieldByTerms 演示通过Terms获取没有存储的Field值
 testGetFieldByFieldDocWithSorted 演示通过FieldDoc获取没有存储的值
 testGetFieldByTermVector 演示通过TermVector获取没有存储的值
 testGetFieldByTermVectors 演示通过TermVectors获取没有存储的值
 * Description: Lucene 6.5.0
 *
 * @Author HeFeng
 * @Create 2018-10-12 11:27
 */
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import java.io.IOException;

import static org.apache.lucene.search.SortField.Type.STRING;

public class GetNonStoredFieldDemo {
    private RAMDirectory ramDirectory = new RAMDirectory();
    private IndexWriter indexWriter = new IndexWriter(ramDirectory, new IndexWriterConfig(new WhitespaceAnalyzer()));

    public GetNonStoredFieldDemo() throws IOException {
    }

    @Test
    public void testGetFieldByStore() throws IOException {
        initIndexForStore();
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(ramDirectory));
        int count = indexSearcher.count(new MatchAllDocsQuery());
        TopDocs search = indexSearcher.search(new MatchAllDocsQuery(), count);
        ScoreDoc[] scoreDocs = search.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("IDX") + "=>" + doc.get("title"));
        }
        ramDirectory.close();
    }

    @Test
    public void testGetFieldByTerms() throws IOException {
        initIndexForTerms();
        Fields fields = MultiFields.getFields(DirectoryReader.open(ramDirectory));
        Terms idx = fields.terms("IDX");
        Terms title = fields.terms("title");
//or you can use like this
//TermsEnum idxIter = MultiFields.getTerms(DirectoryReader.open(ramDirectory), "IDX").iterator();
        TermsEnum idxIter = idx.iterator();
        TermsEnum titleIter = title.iterator();
        BytesRef bytesRef;
        while ((bytesRef = idxIter.next()) != null) {
            System.out.println(bytesRef.utf8ToString() + "=>" + titleIter.next().utf8ToString());
        }
        ramDirectory.close();
    }

    @Test
    public void testGetFieldByFieldDocWithSorted() throws IOException {
        initIndexForFieldDocWithSorted();
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(ramDirectory));
        int count = indexSearcher.count(new MatchAllDocsQuery());
//must use method which returns TopFieldDocs
        TopFieldDocs search = indexSearcher.search(new MatchAllDocsQuery(), count, new Sort(new SortField("IDX", STRING)));
        ScoreDoc[] scoreDocs = search.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            FieldDoc fieldDoc = (FieldDoc) scoreDoc;
            Object[] fields = fieldDoc.fields;
            if (fields[0] instanceof BytesRef) {
                BytesRef temp = (BytesRef) fields[0];
                System.out.println(temp.utf8ToString() + "=>" + indexSearcher.doc(scoreDoc.doc).get("title"));
            }
        }
        ramDirectory.close();
    }

    @Test
    public void testGetFieldByTermVector() throws IOException {
        initIndexForTermVector();
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(ramDirectory));
        int count = indexSearcher.count(new MatchAllDocsQuery());
        TopDocs search = indexSearcher.search(new MatchAllDocsQuery(), count);
        ScoreDoc[] scoreDocs = search.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Terms idx = indexSearcher.getIndexReader().getTermVector(doc, "IDX");
            TermsEnum iterator = idx.iterator();
            BytesRef bytesRef;
            while ((bytesRef = iterator.next()) != null) {
                System.out.println(bytesRef.utf8ToString() + "=>" + indexSearcher.doc(doc).get("title"));
            }
        }
        ramDirectory.close();
    }

    @Test
    public void testGetFieldByTermVectors() throws IOException {
        initIndexForTermVector();
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(ramDirectory));
        int count = indexSearcher.count(new MatchAllDocsQuery());
        TopDocs search = indexSearcher.search(new MatchAllDocsQuery(), count);
        ScoreDoc[] scoreDocs = search.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Fields termVectors = indexSearcher.getIndexReader().getTermVectors(doc);
            Terms idx = termVectors.terms("IDX");
            TermsEnum iterator = idx.iterator();
            BytesRef bytesRef;
            while ((bytesRef = iterator.next()) != null) {
                System.out.println(bytesRef.utf8ToString() + "=>" + indexSearcher.doc(doc).get("title"));
            }
        }
        ramDirectory.close();
    }

    private void initIndexForStore() throws IOException {
        Document document = new Document();
        document.add(new StringField("IDX", "IDX01", Field.Store.YES));
        document.add(new StringField("title", "TITLE01", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX02", Field.Store.YES));
        document.add(new StringField("title", "TITLE02", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX03", Field.Store.YES));
        document.add(new StringField("title", "TITLE03", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX04", Field.Store.YES));
        document.add(new StringField("title", "TITLE04", Field.Store.YES));
        indexWriter.addDocument(document);
        indexWriter.close();
    }

    private void initIndexForTerms() throws IOException {
        Document document = new Document();
        document.add(new StringField("IDX", "IDX01", Field.Store.NO));
        document.add(new StringField("title", "TITLE01", Field.Store.NO));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX02", Field.Store.NO));
        document.add(new StringField("title", "TITLE02", Field.Store.NO));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX03", Field.Store.NO));
        document.add(new StringField("title", "TITLE03", Field.Store.NO));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("IDX", "IDX04", Field.Store.NO));
        document.add(new StringField("title", "TITLE04", Field.Store.NO));
        indexWriter.addDocument(document);
        indexWriter.close();
    }

    private void initIndexForTermVector() throws IOException {
        FieldType fieldType = new FieldType();
        fieldType.setStoreTermVectors(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        Document document = new Document();
        document.add(new Field("IDX", "IDX01", fieldType));
        document.add(new StringField("title", "TITLE01", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new Field("IDX", "IDX02", fieldType));
        document.add(new StringField("title", "TITLE02", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new Field("IDX", "IDX03", fieldType));
        document.add(new StringField("title", "TITLE03", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new Field("IDX", "IDX04", fieldType));
        document.add(new StringField("title", "TITLE04", Field.Store.YES));
        indexWriter.addDocument(document);
        indexWriter.close();
    }

    private void initIndexForFieldDocWithSorted() throws IOException {
        Document document = new Document();
        document.add(new SortedDocValuesField("IDX", new BytesRef("IDX01")));
        document.add(new StringField("title", "TITLE01", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new SortedDocValuesField("IDX", new BytesRef("IDX02")));
        document.add(new StringField("title", "TITLE02", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new SortedDocValuesField("IDX", new BytesRef("IDX03")));
        document.add(new StringField("title", "TITLE03", Field.Store.YES));
        indexWriter.addDocument(document);

        document = new Document();
        document.add(new SortedDocValuesField("IDX", new BytesRef("IDX04")));
        document.add(new StringField("title", "TITLE04", Field.Store.YES));
        indexWriter.addDocument(document);
        indexWriter.close();
    }

}