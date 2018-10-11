package demo.tokenStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.StringReader;

/**
 * 描述:
 * testAttribute
 *
 * @Author HeFeng
 * @Create 2018-10-10 14:59
 */
public class TestAttribute {

    public static void main(String[] args) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        String input = "This is a test text for attribute! Just add-some word.";
        TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(input));
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute positionIncrementAttribute = tokenStream.addAttribute(PositionIncrementAttribute.class);
        OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
        TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
        PayloadAttribute payloadAttribute = tokenStream.addAttribute(PayloadAttribute.class);
        payloadAttribute.setPayload(new BytesRef("Just"));
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            System.out.print("[" + charTermAttribute + " increment:" + positionIncrementAttribute.getPositionIncrement() +
                    " start:" + offsetAttribute
                    .startOffset() + " end:" +
                    offsetAttribute
                            .endOffset() + " type:" + typeAttribute.type() + " payload:" + payloadAttribute.getPayload() +
                    "]\n");
        }

        tokenStream.end();
        tokenStream.close();
    }

}