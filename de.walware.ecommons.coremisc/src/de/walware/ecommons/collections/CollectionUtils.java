/*=============================================================================#
 # Copyright (c) 2012-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;


/**
 * @since 1.1
 */
public class CollectionUtils {
	
	
	/**
	 * @since 1.1
	 */
	@Deprecated
	public static <E> ConstList<E> emptyConstList() {
		return ConstArrayList.EMPTY_CONST_LIST;
	}
	
	/**
	 * @since 1.1
	 */
	@Deprecated
	public static <E> ConstList<E> asConstList(final Collection<E> c) {
		if (c instanceof ConstList) {
			return (ConstList<E>) c;
		}
		return new ConstArrayList<E>(c);
	}
	
	
	/**
	 * @since 1.1
	 */
	public static String toString(final List<?> list, final String sep) {
		final int n= list.size();
		if (n <= 0) {
			return ""; //$NON-NLS-1$
		}
		else if (n == 1) {
			return list.get(0).toString();
		}
		else if (list instanceof RandomAccess) {
			final StringBuilder sb= new StringBuilder(list.get(0).toString());
			for (int i= 1; i < n; i++) {
				sb.append(sep);
				sb.append(list.get(i).toString());
			}
			return sb.toString();
		}
		else {
			final Iterator<?> iter= list.iterator();
			final StringBuilder sb= new StringBuilder(iter.next().toString());
			for (int i= 1; i < n; i++) {
				sb.append(sep);
				sb.append(iter.next().toString());
			}
			return sb.toString();
		}
	}
	
	/**
	 * @since 1.5
	 */
	public static String toString(final Collection<?> c, final String sep) {
		if (c instanceof List) {
			return toString((List<?>) c, sep);
		}
		final int n= c.size();
		if (n <= 0) {
			return ""; //$NON-NLS-1$
		}
		else if (n == 1) {
			return c.iterator().next().toString();
		}
		else {
			final Iterator<?> iter= c.iterator();
			final StringBuilder sb= new StringBuilder(iter.next().toString());
			for (int i= 1; i < n; i++) {
				sb.append(sep);
				sb.append(iter.next().toString());
			}
			return sb.toString();
		}
	}
	
}
