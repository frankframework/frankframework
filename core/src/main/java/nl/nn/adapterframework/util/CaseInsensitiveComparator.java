package nl.nn.adapterframework.util;

import java.util.Comparator;

public class CaseInsensitiveComparator implements Comparator {

	public int compare(Object object1, Object object2) {
		int result = 0;
		if (object1 instanceof String && object2 instanceof String) {
			String string1 = ((String)object1).toLowerCase();
			String string2 = ((String)object2).toLowerCase();
			result = string1.compareTo(string2);
		}
		return result;
	}

}
