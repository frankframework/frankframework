/*
 * $Log: RestUriComparator.java,v $
 * Revision 1.4  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/05/23 15:32:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first bugfixes
 *
 * Revision 1.1  2011/05/19 15:11:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
			return StringUtils.isEmpty(uri2)?0:1;
		}
		int result=uri2.length()-uri1.length();
		if (result==0) {
			result=uri1.compareTo(uri2);
		}
		return result;
	}

}
