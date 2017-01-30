package org.transdroid.connect.util;

public final class StringUtil {

	private StringUtil() {}

	public static boolean isEmpty(String string) {
		return string == null || string.equals("");
	}

}
