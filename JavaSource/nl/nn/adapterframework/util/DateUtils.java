package nl.nn.adapterframework.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Utilities for formatting and parsing dates.
 * @version Id
 *
 * @author Johan Verrips IOS
 */
public class DateUtils {
	public static final String version="$Id: DateUtils.java,v 1.4 2004-11-10 12:59:27 L190409 Exp $";
	

	public static final String fullIsoFormat          = "yyyy-MM-dd'T'hh:mm:sszzz";
    public static final String shortIsoFormat         = "yyyy-MM-dd";

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
	
	static public String format(Date obj, String dateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        ParsePosition pos = new ParsePosition(0);
        return formatter.format((Date) obj);
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
