/*
 * $Log: FileNameComparator.java,v $
 * Revision 1.2  2005-08-22 09:05:51  europe\L190409
 * added natural-order comparison
 *
 * Revision 1.1  2005/07/19 11:01:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of FileNameComparator
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.util.Comparator;

/**
 * Compares filenames, so directory listings appear in a kind of natural order.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class FileNameComparator implements Comparator {

	protected static int getNextIndex(String s, int start, boolean numericPart) {
		int pos=start;
		while (pos<s.length() && !Character.isWhitespace(s.charAt(pos)) && 
								  numericPart == Character.isDigit(s.charAt(pos))) {
			pos++;
		}
		return pos;
	}

	protected static int skipWhitespace(String s, int start) {
		while (start<s.length() && Character.isWhitespace(s.charAt(start))) {
			start++;
		}
		return start;
	}


	public static int compareStringsNaturalOrder(String s0, String s1, boolean caseSensitive) {
		int result=0;
		int start_pos0=0;
		int start_pos1=0;
		int end_pos0=0;
		int end_pos1=0;
		boolean numeric=false;
		
		while(result ==0 && end_pos0<s0.length() && end_pos1<s1.length()) {
			start_pos0=skipWhitespace(s0, start_pos0);
			start_pos1=skipWhitespace(s1, start_pos1);
			end_pos0=getNextIndex(s0, start_pos0, numeric);			
			end_pos1=getNextIndex(s1, start_pos1, numeric);
			String part0=s0.substring(start_pos0,end_pos0);
			String part1=s1.substring(start_pos1,end_pos1);
			if (numeric) {
				long lres;
				try {
					lres = Long.parseLong(part0)-Long.parseLong(part1); 
				} catch (NumberFormatException e) {
					lres = part0.compareTo(part1);
				}
				if (lres!=0) {
					if (lres<0) {
						return -1;
					} 
					return 1;
				}
			} else {
				if (caseSensitive) {
					result = part0.compareTo(part1);
				} else {
					result = part0.compareToIgnoreCase(part1);
				}
			}
			start_pos0=end_pos0;
			start_pos1=end_pos1;
			numeric=!numeric;
		}
		if (result!=0) {
			return result;
		}
		if (end_pos0<s0.length()) {
			return 1;
		}
		if (end_pos1<s1.length()) {
			return -1;
		}
		return 0;
	}

	public static int compareFilenames(File f0, File f1) {
		int result;
		if (f0.isDirectory()!=f1.isDirectory()) {
			if (f0.isDirectory()) {
				return 1;
			}
			return -1;
		}
		result = compareStringsNaturalOrder(f0.getName(),f1.getName(),false);
		if (result!=0) {
			return result; 
		}
		result = compareStringsNaturalOrder(f0.getName(),f1.getName(),true);
		if (result!=0) {
			return result; 
		}
		result = f0.getName().compareToIgnoreCase(f1.getName());
		if (result!=0) {
			return result; 
		}
		result = f0.getName().compareTo(f1.getName());
		if (result==0) {
			long lendif = f1.length()-f0.length();
			if (lendif > 0) {
				result=1;
			} else if (lendif < 0) {
				result=-1;
			}
		}
		return result;
	}

	public int compare(Object arg0, Object arg1) {
		return compareFilenames((File) arg0, (File) arg1);
	}

}
