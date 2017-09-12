package nl.nn.adapterframework.http.rest;

import java.util.Comparator;

public class ApiUriComparator implements Comparator<String> {

	public int compare(String uri1, String uri2) {
		int uri1c = uri1.length() - uri1.replace("*", "").length();
		int uri2c = uri2.length() - uri2.replace("*", "").length();

		int result=uri1c-uri2c;
		if (result==0) {
			result=uri1.compareTo(uri2);
		}
		return result;
	}
}
