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

import java.util.List;


/**
 * @since 1.5
 */
public class CollectionUtils {
	
	
	public static String toString(final List<?> list, final String sep) {
		final int n = list.size();
		if (n <= 0) {
			return ""; //$NON-NLS-1$
		}
		else if (n == 1) {
			return list.get(0).toString();
		}
		else {
			final StringBuilder sb = new StringBuilder(list.get(0).toString());
			for (int i = 1; i < n; i++) {
				sb.append(sep);
				sb.append(list.get(i).toString());
			}
			return sb.toString();
		}
	}
	
}
