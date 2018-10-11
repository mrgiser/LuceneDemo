package demo.Analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class StopAnalyzerExtend extends Analyzer {
    public static final Logger logger = LoggerFactory.getLogger(StopAnalyzerExtend.class);
    private CharArraySet stopWordSet;//停止词词典

    public CharArraySet getStopWordSet() {
        return this.stopWordSet;
    }

    public void setStopWordSet(CharArraySet stopWordSet) {
        this.stopWordSet = stopWordSet;
    }

    public StopAnalyzerExtend() {
        super();
        setStopWordSet(StopAnalyzer.ENGLISH_STOP_WORDS_SET);
    }

    /**
     * @param stops 需要扩展的停止词
     */
    public StopAnalyzerExtend(List<String> stops) {
        this();
        //stopWordSet = getStopWordSet();
        stopWordSet = CharArraySet.copy(getStopWordSet());
        stopWordSet.addAll(StopFilter.makeStopSet(stops));
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new LowerCaseTokenizer();
        return new TokenStreamComponents(source, new StopFilter(source, stopWordSet));
    }

    public static void main(String[] args) throws IOException {
        ArrayList<String> strings = new ArrayList<String>() {{
            add("小鬼子");
            add("美国佬");
        }};
        Analyzer analyzer = new StopAnalyzerExtend(strings);
        String content = "小鬼子 and 美国佬 are playing together!";
        TokenStream tokenStream = analyzer.tokenStream("myfield", content);
        tokenStream.reset();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            // 已经过滤掉自定义停用词
            // 输出：playing together
            logger.info(charTermAttribute.toString());
        }
        tokenStream.end();
        tokenStream.close();
    }
}