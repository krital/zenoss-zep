/*****************************************************************************
 * 
 * Copyright (C) Zenoss, Inc. 2010, all rights reserved.
 * 
 * This content is made available according to terms specified in
 * License.zenoss under the directory where your Zenoss product is installed.
 * 
 ****************************************************************************/


package org.zenoss.zep.index.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;

import java.io.IOException;
import java.io.Reader;

/**
 * Analyzer used for element and sub element identifiers.
 */
public final class IdentifierAnalyzer extends Analyzer {

    public static final int MIN_NGRAM_SIZE = 3;
    public static final int MAX_NGRAM_SIZE = MIN_NGRAM_SIZE;

    private static class SavedStreams {
        Tokenizer source;
        TokenStream result;
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        SavedStreams streams = (SavedStreams) getPreviousTokenStream();
        if (streams == null) {
            streams = new SavedStreams();
            streams.source = new WhitespaceTokenizer(IndexConstants.LUCENE_VERSION, reader);
            streams.result = new LowerCaseFilter(IndexConstants.LUCENE_VERSION, streams.source);
            streams.result = new NGramTokenFilter(streams.result, MIN_NGRAM_SIZE, MAX_NGRAM_SIZE);
            setPreviousTokenStream(streams);
        }
        else {
            streams.source.reset(reader);
        }
        return streams.result;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream tokenStream = new WhitespaceTokenizer(IndexConstants.LUCENE_VERSION, reader);
        tokenStream = new LowerCaseFilter(IndexConstants.LUCENE_VERSION, tokenStream);
        tokenStream = new NGramTokenFilter(tokenStream, MIN_NGRAM_SIZE, MAX_NGRAM_SIZE);
        return tokenStream;
    }
}
