/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: DateUtils.java,v $
 * Revision 1.18  2012-12-13 10:40:18  europe\m168309
 * added parseXmlDateTime() method
 *
 * Revision 1.17  2011/11/30 13:51:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.15  2011/03/16 16:38:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added nextHigerValue()
 *
 * Revision 1.14  2008/09/01 15:54:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make generic dateformat more generic
 *
 * Revision 1.13  2008/09/01 15:36:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added seconds to generic datetime format
 *
 * Revision 1.12  2008/06/03 15:55:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added code for parsing in any format, and optimal formatting
 *
 * Revision 1.11  2008/02/13 12:58:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added format() with default format
 *
 * Revision 1.10  2007/10/08 12:25:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed incorrect date format
 *
 * Revision 1.9  2007/02/19 08:17:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add convertDate() function (by Sanne Hoekstra)
 *
 * Revision 1.8  2007/02/19 07:46:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.GDate;

/**
 * Utilities for formatting and parsing dates.
 * 
 * @author Johan Verrips IOS
 * @version $Id$
 */
public class DateUtils {
	public static final String version = "$RCSfile: DateUtils.java,v $ $Revision: 1.18 $ $Date: 2012-12-13 10:40:18 $";
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
	public final static String FORMAT_GENERICDATETIME  ="yyyy-MM-dd HH:mm:ss";


    /**
     * Format for "dd-MM-yy"
     */
	public final static String FORMAT_DATE             ="dd-MM-yy";

    /**
     * Format for "HH:mm:ss"
     */
	public final static String FORMAT_TIME_HMS         ="HH:mm:ss";	

	
	public static String format(Date date, String dateFormat) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(date);
	}

	public static String format(long date, String dateFormat) {
		return format(new Date(date),dateFormat);
	}

	public static String format(long date) {
		return format(new Date(date),FORMAT_FULL_GENERIC);
	}
	public static String format(Date date) {
		return format(date,FORMAT_FULL_GENERIC);
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
     * Parses a string to a Date, according to the XML Schema dateTime data type
     */
    static public Date parseXmlDateTime(String s) {
    	GDate gdate = new org.apache.xmlbeans.GDate(s);
        Date result = gdate.getDate();
		return result;
    }

	/**
	 * Parses a string to a Date, according to many possible conventions
	 */
	static public Date parseAnyDate(String dateInAnyFormat) throws CalendarParserException {
		Calendar c = CalendarParser.parse(dateInAnyFormat);
		Date d = new Date(c.getTimeInMillis());
		return d;
	}

	/**
	 * Formats a Date to a String, leaving out trailing zero values.
	 */
	static public String formatOptimal(Date d)  {
		String result;
		if ((d.getTime()%1000)==0 ) {
			if (d.getSeconds()==0) {
				if (d.getMinutes()==0 && d.getHours()==0) {
					result = format(d,"yyyy-MM-dd");
				} else {
					result = format(d,"yyyy-MM-dd HH:mm");
				}
			} else {
				result = format(d,"yyyy-MM-dd HH:mm:ss");
			}
		} else {
			result = format(d,"yyyy-MM-dd HH:mm:ss.SSS");
		}
		return result;
	}

	/**
	 * returns the next higher value, as if it was formatted optimally using formatOptimal().
	 */
	static public Date nextHigherValue(Date d)  {
		int delta;
		if ((d.getTime()%1000)==0 ) {
			if (d.getSeconds()==0) {
				if (d.getMinutes()==0 && d.getHours()==0) {
					delta = 24*60*60*1000;
					// result = format(d,"yyyy-MM-dd");
				} else {
					delta = 60*1000;
					//result = format(d,"yyyy-MM-dd HH:mm");
				}
			} else {
				delta = 1000;
				//result = format(d,"yyyy-MM-dd HH:mm:ss");
			}
		} else {
			delta=1;
			//result = format(d,"yyyy-MM-dd HH:mm:ss.SSS");
		}
		return new Date(d.getTime()+delta);
	}


	/**
	 * 
	 * Deze functie maakt het mogelijk om een datum formaat te veranderen.
	 * 
	 * @param 	from	String	date format from.
	 * @param 	to		String	date format to.
	 * @param 	value	String	date to reformat.
	 * @return
	 */
	public static String convertDate(String from, String to, String value) throws ParseException {
		log.debug("convertDate from " + from + " to " + to + " value " + value);
		String result = "";
//		try {
			SimpleDateFormat formatterFrom = new SimpleDateFormat(from);
			SimpleDateFormat formatterTo = new SimpleDateFormat(to);
			Date d = formatterFrom.parse(value);
			String tempStr = formatterFrom.format(d);
			
			if (tempStr.equals(value)) {	
				result = formatterTo.format(d);
			} else {
				log.warn("Error on validating input (" + value + ") with reverse check [" + tempStr+"]");
				throw new ParseException("Error on validating input (" + value + ") with reverse check [" + tempStr+"]",0);
			}
//		}
//		catch (Throwable t) {
//			log.error("Could not finish convertDate", t);
//		}
		log.debug("convertDate result" + result);
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
	public static String changeDate(String date, int years, int months, int days) throws ParseException {
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
	public static String changeDate(String date, int years, int months, int days, String dateFormat) throws ParseException {
		if (log.isDebugEnabled()) log.debug("changeDate date " + date + " years " + years + " months " + months + " days " + days);
		String result = "";
//try {
			SimpleDateFormat df = new SimpleDateFormat(dateFormat);
			Date d = df.parse(date);
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.add(Calendar.YEAR, years);
			cal.add(Calendar.MONTH, months);
			cal.add(Calendar.DAY_OF_MONTH, days);
			result = df.format(cal.getTime());
//		}
//		catch (Throwable t) {
//			log.error("Could not finish changeDate", t);
//		}
		log.debug("changeDate result" + result);
		return result;
	}


}
