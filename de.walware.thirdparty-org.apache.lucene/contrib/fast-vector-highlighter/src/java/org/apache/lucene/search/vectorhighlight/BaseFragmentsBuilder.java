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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo.Toffs;

public abstract class BaseFragmentsBuilder implements FragmentsBuilder {

  protected String[] preTags, postTags;
  public static final String[] COLORED_PRE_TAGS = {
    "<b style=\"background:yellow\">", "<b style=\"background:lawngreen\">", "<b style=\"background:aquamarine\">",
    "<b style=\"background:magenta\">", "<b style=\"background:palegreen\">", "<b style=\"background:coral\">",
    "<b style=\"background:wheat\">", "<b style=\"background:khaki\">", "<b style=\"background:lime\">",
    "<b style=\"background:deepskyblue\">", "<b style=\"background:deeppink\">", "<b style=\"background:salmon\">",
    "<b style=\"background:peachpuff\">", "<b style=\"background:violet\">", "<b style=\"background:mediumpurple\">",
    "<b style=\"background:palegoldenrod\">", "<b style=\"background:darkkhaki\">", "<b style=\"background:springgreen\">",
    "<b style=\"background:turquoise\">", "<b style=\"background:powderblue\">"
  };
  
  
  protected BaseFragmentsBuilder( final String[] preTags, final String[] postTags ){
    this.preTags = preTags;
    this.postTags = postTags;
  }
  
  static Object checkTagsArgument( final Object tags ){
    if( tags instanceof String ) return tags;
    else if( tags instanceof String[] ) return tags;
    throw new IllegalArgumentException( "type of preTags/postTags must be a String or String[]" );
  }
  
  public abstract List<WeightedFragInfo> getWeightedFragInfoList( List<WeightedFragInfo> src );
  
  public String createFragment( final IndexReader reader, final int docId,
      final String fieldName, final FieldFragList fieldFragList ) throws IOException {
    final String[] fragments = createFragments( reader, docId, fieldName, fieldFragList, 1 );
    if( fragments == null || fragments.length == 0 ) return null;
    return fragments[0];
  }

  public String[] createFragments( final IndexReader reader, final int docId,
      final String fieldName, final FieldFragList fieldFragList, final int maxNumFragments )
      throws IOException {
    if( maxNumFragments < 0 )
      throw new IllegalArgumentException( "maxNumFragments(" + maxNumFragments + ") must be positive number." );

    final List<WeightedFragInfo> fragInfos = getWeightedFragInfoList( fieldFragList.fragInfos );
    
    final List<String> fragments = new ArrayList<String>( maxNumFragments );
    final Field[] values = getFields( reader, docId, fieldName );
    if( values.length == 0 ) return null;
    final StringBuilder buffer = new StringBuilder();
    final int[] nextValueIndex = { 0 };
    for( int n = 0; n < maxNumFragments && n < fragInfos.size(); n++ ){
      final WeightedFragInfo fragInfo = fragInfos.get( n );
      addFragments( buffer, nextValueIndex, values, fragInfo, fragments );
    }
    return fragments.toArray( new String[fragments.size()] );
  }
  
  protected Field[] getFields( IndexReader reader, int docId, String fieldName) throws IOException {
    // according to javadoc, doc.getFields(fieldName) cannot be used with lazy loaded field???
    Document doc = reader.document( docId, new MapFieldSelector( new String[]{ fieldName } ) );
    return doc.getFields( fieldName ); // according to Document class javadoc, this never returns null
  }

	protected void addFragments( final StringBuilder buffer, final int[] index, final Field[] values,
			final WeightedFragInfo fragInfo, final List<String> fragments) throws IOException{
		int valuesIndex = index[0]; // idx for next value in values
		int valueOffset = 0; // offset of current value
		int valueCharIndex = 0; // idx for next char in value
		OFFSET: if (fragInfo.startOffset > 0) {
			while (valuesIndex < values.length) {
				valueCharIndex = fragInfo.startOffset - valueOffset;
				if (valueCharIndex <= values[valuesIndex].stringValue().length()) {
					break OFFSET;
				}
				if (values[valuesIndex].isTokenized()) {
					valueOffset++;
				}
				valueOffset += values[valuesIndex++].stringValue().length();
			}
		}
		if (valuesIndex >= values.length) {
			return;
		}
//		if (valuesIndex > 0) {
//			for (int i = 0; i < valuesIndex; i++) {
//				valuesOffset += values[i].length();
//				valuesOffset ++;
//			}
//		}
		FRAGMENTS: for( final SubInfo subInfo : fragInfo.subInfos ){
			for( final Toffs to : subInfo.termsOffsets ){
				while (to.startOffset > valueOffset + values[valuesIndex].stringValue().length()) {
					if (buffer.length() > 0) {
						final String value = values[valuesIndex].stringValue();
						buffer.append(value.substring(valueCharIndex));
						fragments.add(buffer.toString());
						buffer.setLength(0);
					}
					valueCharIndex = 0;
					if (values[valuesIndex].isTokenized()) {
						valueOffset++;
					}
					valueOffset += values[valuesIndex++].stringValue().length();
					if (valuesIndex >= values.length) {
						break FRAGMENTS;
					}
				}
				final String value = values[valuesIndex].stringValue();
				append(buffer, value, valueCharIndex, to.startOffset - valueOffset);
				buffer.append(getPreTag(subInfo.seqnum));
				valueCharIndex = Math.min(to.endOffset - valueOffset, value.length());
				append(buffer, value, to.startOffset - valueOffset, valueCharIndex);
				buffer.append(getPostTag(subInfo.seqnum));
			}
		}
		if (buffer.length() > 0) {
			final String value = values[valuesIndex].stringValue();
			append(buffer, value, valueCharIndex,
					Math.min(fragInfo.endOffset - valueOffset, value.length()) );
			fragments.add(buffer.toString());
			buffer.setLength(0);
		}
	}
	
	protected void append(final StringBuilder buffer, final String value, final int begin, final int end)
			throws IOException {
		buffer.append( value.substring( begin, end) );
	}
	
  protected String makeFragment( StringBuilder buffer, int[] index, Field[] values, WeightedFragInfo fragInfo ){
    final int s = fragInfo.startOffset;
    return makeFragment( fragInfo, getFragmentSource( buffer, index, values, s, fragInfo.endOffset ), s );
  }
  
  private String makeFragment( WeightedFragInfo fragInfo, String src, int s ){
    StringBuilder fragment = new StringBuilder();
    int srcIndex = 0;
    for( SubInfo subInfo : fragInfo.subInfos ){
      for( Toffs to : subInfo.termsOffsets ){
        fragment.append( src.substring( srcIndex, to.startOffset - s ) ).append( getPreTag( subInfo.seqnum ) )
          .append( src.substring( to.startOffset - s, to.endOffset - s ) ).append( getPostTag( subInfo.seqnum ) );
        srcIndex = to.endOffset - s;
      }
    }
    fragment.append( src.substring( srcIndex ) );
    return fragment.toString();
  }
  
  protected String getFragmentSource( StringBuilder buffer, int[] index, Field[] values,
      int startOffset, int endOffset ){
    while( buffer.length() < endOffset && index[0] < values.length ){
      buffer.append( values[index[0]].stringValue() );
      if( values[index[0]].isTokenized() && values[index[0]].stringValue().length() > 0 && index[0] + 1 < values.length )
        buffer.append( ' ' );
      index[0]++;
    }
    int eo = buffer.length() < endOffset ? buffer.length() : endOffset;
    return buffer.substring( startOffset, eo );
  }
  
  protected String getPreTag( final int num ){
    int n = num % preTags.length;
    return preTags[n];
  }
  
  protected String getPostTag( final int num ){
    int n = num % postTags.length;
    return postTags[n];
  }
}
