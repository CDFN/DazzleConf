/* 
 * DazzleConf-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * DazzleConf-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DazzleConf-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with DazzleConf-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */
package space.arim.dazzleconf.internal;

import java.lang.reflect.Method;
import java.util.List;

import space.arim.dazzleconf.annote.ConfComment;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.sorter.SortableConfigurationEntry;

public abstract class ConfEntry implements SortableConfigurationEntry {

	private final Method method;
	private final String key;
	private final List<String> comments;

	ConfEntry(Method method) {
		this.method = method;
		key = findKey(method);
		comments = findComments(method);
	}
	
	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public String getKey() {
		return key;
	}
	
	@Override
	public List<String> getComments() {
		return comments;
	}
	
	private static String findKey(Method method) {
		ConfKey confKey = method.getAnnotation(ConfKey.class);
		return (confKey != null) ? confKey.value() : method.getName();
	}
	
	private static List<String> findComments(Method method) {
		ConfComment commentAnnotation = method.getAnnotation(ConfComment.class);
		if (commentAnnotation != null) {
			return ImmutableCollections.listOf(commentAnnotation.value());
		}
		ConfComments commentsAnnotation = method.getAnnotation(ConfComments.class);
		return (commentsAnnotation == null) ?
				ImmutableCollections.emptyList() : ImmutableCollections.listOf(commentsAnnotation.value());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getKey().hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConfEntry)) {
			return false;
		}
		ConfEntry other = (ConfEntry) obj;
		return getKey().equals(other.getKey());
	}

	@Override
	public String toString() {
		return "ConfEntry [key=" + getKey() + "]";
	}
	
}
