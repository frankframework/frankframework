/*
 * $Log: DateUtils.java,v $
 * Revision 1.7  2006-08-21 15:13:37  europe\L190409
 * added full-generic format
 *
 * Revision 1.6  2006/01/19 12:18:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected fullIsoFormat
 *
 * Revision 1.5  2005/10/17 07:35:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added format(long)
 *
 */
package nl.nn.adapterframework.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Utilities for formatting and parsing dates.
 * 
 * @author Johan Verrips IOS
 * @version Id
 */
public class DateUtils {
	public static final String version = "$RCSfile: DateUtils.java,v $ $Revision: 1.7 $ $Date: 2006-08-21 15:13:37 $";
	

	public static final String fullIsoFormat          = "yyyy-MM-dd'T'HH:mm:sszzz";
    public static final String shortIsoFormat         = "yyyy-MM-dd";

	/**
	 * Format for "yyyy-MM-dd HH:mm:ss.SSS"
	 */
	public static final String FORMAT_FULL_GENERIC      = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Format for "######.###"
     */
    public final static String FORMAT_MILLISECONDS	   ="######.###";

    /**
     * Format for "dd-MM-yy HH:mm"
     */
	public final static String FORMAT_GENERICDATETIME  ="dd-MM-yy HH:mm";


    /**
     * Format for "dd-MM-yy"
     */
	public final static String FORMAT_DATE             ="dd-MM-yy";

    /**
     * Format for "HH:mm:ss"
     */
	public final static String FORMAT_TIME_HMS         ="HH:mm:ss";	

	/**
     * Format for "dd-MM-yy HH:mm,SSS"
     */
	public final static String FORMAT_DATETIME_MILLISECONDS  ="dd-MM-yy HH:mm,SSS";

	
	public static String format(Date date, String dateFormat) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(date);
	}

	public static String format(long date, String dateFormat) {
		return format(new Date(date),dateFormat);
	}



    /**
     * Method getIsoTimeStamp.
     * 
     * Get current date-time timestamp in ISO 8601 format.
     * @return String
     */
    public static String getIsoTimeStamp() {
        return format(new Date(), fullIsoFormat);
    }
    /**
     * Parses a string to a Date, according to the pattern
     */
    static public Date parseToDate(String s, String dateFormat) {
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);
        ParsePosition p = new ParsePosition(0);
        Date result = df.parse(s, p);
        return result;
    }

}
