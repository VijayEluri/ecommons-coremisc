package org.apache.lucene.search.vectorhighlight;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo.Toffs;

/**
 * Another highlighter implementation.
 *
 */
public class FastVectorHighlighter {
	
	public static final boolean DEFAULT_PHRASE_HIGHLIGHT = true;
	public static final boolean DEFAULT_FIELD_MATCH = true;
	
	private final boolean phraseHighlight;
	private final boolean fieldMatch;
	private final FragListBuilder fragListBuilder;
	private FragmentsBuilder fragmentsBuilder;
	
	private String[] preTags;
	private String[] postTags;
	
	private FieldQuery fieldQuery;
	private int maxNumFragments;
	
	private int collectedFragments;
	private int totalMatches;
	
	
	/**
	 * a constructor. A FragListBuilder and a FragmentsBuilder can be specified (plugins).
	 * 
	 * @param phraseHighlight true of false for phrase highlighting
	 * @param fieldMatch true of false for field matching
	 * @param preTags
	 * @param postTags
	 */
	public FastVectorHighlighter( final boolean phraseHighlight, final boolean fieldMatch ){
		this.phraseHighlight = phraseHighlight;
		this.fieldMatch = fieldMatch;
		this.fragListBuilder = new SimpleFragListBuilder();
	}
	
	
	public void setTags( final String[] preTags, final String[] postTags ) {
		this.preTags = preTags;
		this.postTags = postTags;
	}
	
	/**
	 * create a FieldQuery object.
	 * 
	 * @param query a query
	 * @return the created FieldQuery object
	 */
	public void setQuery( final Query query ){
		fieldQuery = new FieldQuery( query, phraseHighlight, fieldMatch );
	}
	
	public void setMaxNumFragments(final int maxNumFragments) {
		this.maxNumFragments = maxNumFragments;
	}
	
	public void clear() {
		this.totalMatches = 0;
		this.collectedFragments = 0;
	}
	
//	/**
//	 * return the best fragment.
//	 * 
//	 * @param fieldQuery FieldQuery object
//	 * @param reader IndexReader of the index
//	 * @param docId document id to be highlighted
//	 * @param fieldName field of the document to be highlighted
//	 * @param fragCharSize the length (number of chars) of a fragment
//	 * @return the best fragment (snippet) string
//	 * @throws IOException
//	 */
//	public final void createBestFragment( final FieldQuery fieldQuery, final IndexReader reader, final int docId,
//			final String[] fieldName, final int fragCharSize ) throws IOException {
//		final FieldFragList fieldFragList = getFieldFragList( this.fieldQuery, reader, docId, fieldName[i], fragCharSize );
//		if (fragmentsBuilder == null) {
//			this.fragmentsBuilder = new ScoreOrderFragmentsBuilder(preTags, postTags);
//		}
//		return fragmentsBuilder.createFragment( reader, docId, fieldName, fieldFragList );
//	}
	
	/**
	 * return the best fragments.
	 * 
	 * @param fieldQuery FieldQuery object
	 * @param reader IndexReader of the index
	 * @param docId document id to be highlighted
	 * @param fieldName field of the document to be highlighted
	 * @param fragCharSize the length (number of chars) of a fragment
	 * @return 
	 * @return created fragments or null when no fragments created.
	 *         size of the array can be less than maxNumFragments
	 * @throws IOException
	 */
	public final String[] getBestFragments( final IndexReader reader, final int docId,
			final String fieldNames, final int fragCharSize ) throws IOException {
		final FieldFragList fieldFragList = getFieldFragList( this.fieldQuery, reader, docId, fieldNames, fragCharSize );
		final List<WeightedFragInfo> fragInfos = fieldFragList.fragInfos;
		for (int j = 0; j < fragInfos.size(); j++) {
			this.totalMatches += fragInfos.get(j).subInfos.size();
		}
		final int num = this.maxNumFragments - this.collectedFragments;
		if (num <= 0) {
			return null;
		}
		if (this.fragmentsBuilder == null) {
			this.fragmentsBuilder = new ScoreOrderFragmentsBuilder(preTags, postTags);
		}
		return fragmentsBuilder.createFragments( reader, docId, fieldNames, fieldFragList, num );
	}
	
	public int getTotalMatches() {
		return this.totalMatches;
	}
	
	public String getComplete( final IndexReader reader, final int docId, final String fieldName ) throws IOException {
		final FieldTermStack fieldTermStack = new FieldTermStack( reader, docId, fieldName, fieldQuery );
		final FieldPhraseList fieldPhraseList = new FieldPhraseList( fieldTermStack, fieldQuery );
		final String value = reader.document(docId).get(fieldName);
		int valueCharIndex = 0;
		final StringBuilder sb = new StringBuilder(value.length() + fieldPhraseList.phraseList.size() * 32);
		for (final WeightedPhraseInfo phraseInfo : fieldPhraseList.phraseList) {
			for (final Toffs to : phraseInfo.termsOffsets) {
				sb.append(value.substring(valueCharIndex, to.startOffset));
				sb.append(getPreTag(phraseInfo.seqnum));
				sb.append(value.substring(to.startOffset, to.endOffset));
				sb.append(getPostTag(phraseInfo.seqnum));
				valueCharIndex = to.endOffset;
			}
		}
		sb.append(value.substring(valueCharIndex));
		return sb.toString();
	}
	
	
	private FieldFragList getFieldFragList( final FieldQuery fieldQuery, final IndexReader reader, final int docId,
			final String fieldName, final int fragCharSize ) throws IOException {
		final FieldTermStack fieldTermStack = new FieldTermStack( reader, docId, fieldName, fieldQuery );
		final FieldPhraseList fieldPhraseList = new FieldPhraseList( fieldTermStack, fieldQuery );
		return fragListBuilder.createFieldFragList( fieldPhraseList, fragCharSize );
	}
	
	protected String getPreTag( final int num ) {
		return preTags.length > num ? preTags[num] : preTags[0];
	}
	
	protected String getPostTag( final int num ) {
		return postTags.length > num ? postTags[num] : postTags[0];
	}
	
	
	/**
	 * return whether phraseHighlight or not.
	 * 
	 * @return whether phraseHighlight or not
	 */
	public boolean isPhraseHighlight() { return phraseHighlight; }
	
	/**
	 * return whether fieldMatch or not.
	 * 
	 * @return whether fieldMatch or not
	 */
	public boolean isFieldMatch(){ return fieldMatch; }
	
}
