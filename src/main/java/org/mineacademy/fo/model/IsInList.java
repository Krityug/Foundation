package org.mineacademy.fo.model;

import java.util.Collection;

import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictSet;

/**
 * A simple class allowing you to match if something is in that list.
 *
 * Example: The list contains "apple", "red", "car",
 * you call isInList("car") and you get true. Same for any other data type
 *
 * If you create new IsInList("*") or with an empty list, everything will be matched
 *
 * @param <T>
 */
public final class IsInList<T> {

	/**
	 * The internal set for matching
	 */
	private final StrictSet<T> list;

	/**
	 * Is everything matched?
	 */
	private final boolean matchAll;

	/**
	 * Create a new matching list
	 *
	 * @param list
	 */
	public IsInList(StrictSet<T> list) {
		this(list.getSource());
	}

	/**
	 * Create a new matching list
	 *
	 * @param list
	 */
	public IsInList(StrictList<T> list) {
		this(list.getSource());
	}

	/**
	 * Create a new matching list
	 *
	 * @param list
	 */
	public IsInList(Collection<T> list) {
		this.list = new StrictSet<>(list);

		if (list.isEmpty())
			matchAll = true;

		else if (list.iterator().next().equals("*"))
			matchAll = true;

		else
			matchAll = false;
	}

	/**
	 * Return true if the given value is in this list
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean contains(T toEvaluateAgainst) {
		return matchAll || list.contains(toEvaluateAgainst);
	}
}
