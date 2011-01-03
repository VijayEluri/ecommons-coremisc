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

package org.apache.lucene.extra.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.CharStream;


public class StringStream extends CharStream {
	
	
	private final String string;
	private final int length;
	
	private int next;
	private int mark;
	
	
	public StringStream(final String s) {
		string = s;
		length = s.length();
	}
	
	
	@Override
	public int read() throws IOException {
		if (next < length) {
			return string.charAt(next++);
		}
		return -1;
	}
	
	@Override
	public int read(final char[] cbuf, final int off, final int len) throws IOException {
		if (next < length) {
			final int n = Math.min(length - next, len);
			string.getChars(next, next + n, cbuf, off);
			next += n;
			return n;
		}
		return -1;
	}
	
	@Override
	public long skip(final long n) throws IOException {
		if (next < length) {
			final long nn = Math.min(length - next, n);
			next += nn;
			return nn;
		}
		return 0;
	}
	
	@Override
	public boolean ready() throws IOException {
		return true;
	}
	@Override
	public void close() throws IOException {
	}
	
	@Override
	public boolean markSupported() {
		return true;
	}
	
	@Override
	public void mark(final int readAheadLimit) throws IOException {
		mark = next;
	}
	
	@Override
	public void reset() throws IOException {
		next = mark;
	}
	
	@Override
	public int correctOffset(final int currentOff) {
		return currentOff;
	}
	
	
	@Override
	public String toString() {
		return string;
	}
	
}
