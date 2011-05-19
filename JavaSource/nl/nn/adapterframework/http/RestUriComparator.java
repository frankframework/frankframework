/*
 * $Log: RestUriComparator.java,v $
 * Revision 1.1  2011-05-19 15:11:27  L190409
 * first version of Rest-provider support
 *
 */
package nl.nn.adapterframework.http;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class RestUriComparator implements Comparator {

	public int compare(Object o1, Object o2) {
		String uri1=(String) o1;
		String uri2=(String) o2;
		if (StringUtils.isEmpty(uri1)) {
			return StringUtils.isEmpty(uri1)?0:1;
		}
		int result=uri1.length()-uri2.length();
		if (result==0) {
			result=uri1.compareTo(uri2);
		}
		return result;
	}

}
