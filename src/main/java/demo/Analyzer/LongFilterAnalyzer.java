package demo.Analyzer;

import demo.search.SpanQueryDemo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 描述:
 * 自定义Analyzer实现字长过滤
 *
 * @Author HeFeng
 * @Create 2018-10-10 15:19
 */
class LongFilterAnalyzer extends Analyzer {
    public static final Logger logger = LoggerFactory.getLogger(LongFilterAnalyzer.class);
    private int len;

    public int getLen() {
        return this.len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public LongFilterAnalyzer() {
        super();
    }

    public LongFilterAnalyzer(int len) {
        super();
        setLen(len);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new WhitespaceTokenizer();
        //过滤掉长度<len，并且>20的token
        TokenStream tokenStream = new LengthFilter(source, len, 20);
        return new TokenStreamComponents(source, tokenStream);
    }

    public static void main(String[] args) {
        //把长度小于2的过滤掉，开区间
        Analyzer analyzer = new LongFilterAnalyzer(2);
        String words = "I am a java coder! Testingtestingtesting!";
        TokenStream stream = analyzer.tokenStream("myfield", words);
        try {
            stream.reset();
            CharTermAttribute offsetAtt = stream.addAttribute(CharTermAttribute.class);
            while (stream.incrementToken()) {
                logger.info(offsetAtt.toString());
            }
            stream.end();
            stream.close();
        } catch (IOException e) {
        }
    }
}