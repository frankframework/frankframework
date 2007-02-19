/*
 * $Log: DateUtils.java,v $
 * Revision 1.8  2007-02-19 07:46:16  europe\L190409
 * add changeDate() functions (by Sanne Hoekstra)
 *
 * Revision 1.7  2006/08/21 15:13:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Utilities for formatting and parsing dates.
 * 
 * @author Johan Verrips IOS
 * @version Id
 */
public class DateUtils {
	public static final String version = "$RCSfile: DateUtils.java,v $ $Revision: 1.8 $ $Date: 2007-02-19 07:46:16 $";
	protected static Logger log = LogUtil.getLogger(DateUtils.class);
	

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

	/**
	 * 
	 * Add a number of years, months, days to a date specified in a shortIsoFormat, and return it in the same format.
	 * Als een datum component niet aangepast hoeft te worden, moet 0 meegegeven worden.
	 * Dus bijv: changeDate("2006-03-23", 2, 1, -4) = "2008-05-19"
	 * 
	 * @param 	date	A String representing a date in format yyyy-MM-dd.
	 * @param 	years
	 * @param 	months
	 * @param 	days
	 * @return
	 */
	public static String changeDate(String date, int years, int months, int days) {
		return changeDate(date, years, months, days, "yyyy-MM-dd");
	}
	
	/**
	 * 
	 * Add a number of years, months, days to a date specified in a certain format, and return it in the same format.
	 * Als een datum component niet aangepast hoeft te worden, moet 0 meegegeven worden.
	 * Dus bijv: changeDate("2006-03-23", 2, 1, -4) = "2008-05-19"
	 * 
	 * @param 	date	A String representing a date in format (dateFormat).
	 * @param 	years	int
	 * @param 	months	int
	 * @param 	days	int
	 * @param 	dateFormat	A String representing the date format of date.
	 * @return  
	 */
	public static String changeDate(String date, int years, int months, int days, String dateFormat) {
		if (log.isDebugEnabled()) log.debug("changeDate date " + date + " years " + years + " months " + months + " days " + days);
		String result = "";
		try {
			SimpleDateFormat df = new SimpleDateFormat(dateFormat);
			Date d = df.parse(date);
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.add(Calendar.YEAR, years);
			cal.add(Calendar.MONTH, months);
			cal.add(Calendar.DAY_OF_MONTH, days);
			result = df.format(cal.getTime());
		}
		catch (Throwable t) {
			log.error("Could not finish changeDate", t);
		}
		log.debug("changeDate result" + result);
		return result;
	}


}
