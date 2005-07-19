/*
 * $Log: FileNameComparator.java,v $
 * Revision 1.1  2005-07-19 11:01:17  europe\L190409
 * introduction of FileNameComparator
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.util.Comparator;

/**
 * Compares filenames, so directory listings appear in a kind of 'natural' order.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class FileNameComparator implements Comparator {

	public int compare(Object arg0, Object arg1) {
		return compareFilenames((File) arg0, (File) arg1);
	}
	//TODO: provide natural order comparator, that compares numbers the way you would like/expect	
	public static int compareFilenames(File f0, File f1) {
		int result;
		if (f0.isDirectory()!=f1.isDirectory()) {
			if (f0.isDirectory()) {
				return 1;
			}
			return -1;
		}
		result = f0.getName().compareToIgnoreCase(f1.getName());
		if (result==0) {
			result = f0.getName().compareTo(f1.getName());
			if (result==0) {
				long lendif = f1.length()-f0.length();
				if (lendif > 0) {
					result=1;
				} else if (lendif < 0) {
					result=-1;
				}
			}
		}
		return result;
	}

}
