package demo.tokenStream;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述:
 * CourtesyTitleFilter
 *
 * @Author HeFeng
 * @Create 2018-10-10 15:06
 */
public class CourtesyTitleFilter extends TokenFilter {
    Map<String, String> courtesyTitleMap = new HashMap<>();
    private CharTermAttribute termAttribute;

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected CourtesyTitleFilter(TokenStream input) {
        super(input);
        termAttribute = addAttribute(CharTermAttribute.class);
        courtesyTitleMap.put("Dr", "doctor");
        courtesyTitleMap.put("Mr", "mister");
        courtesyTitleMap.put("Mrs", "miss");
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        String small = termAttribute.toString();
        if (courtesyTitleMap.containsKey(small)) {
            termAttribute.setEmpty().append(courtesyTitleMap.get(small));
        }
        return true;
    }
}