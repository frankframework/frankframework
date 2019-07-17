package nl.nn.adapterframework.extensions.aspose.services.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Gerard van der Hoorn(d937275)
 * 
 */
public final class DateUtil {

	private static final String DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ssZ";

	private DateUtil() {
	}

	/**
	 * Clone a date, i.e. give a new instance of the same date.
	 * 
	 * @param date
	 * @return
	 */
	public static Date cloneDate(Date date) {
		return date == null ? null : (Date) date.clone();
	}

	/**
	 * Clone a GregorianCalendar, i.e. give a new instance of the same date.
	 * 
	 * @param date
	 * @return
	 */
	public static GregorianCalendar clone(GregorianCalendar value) {
		return value == null ? null : (GregorianCalendar) value.clone();
	}

	public static DateFormat getDateFormatSecondsHuman() {
		return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	}

	public static DateFormat getDateFormatSeconds() {
		return new SimpleDateFormat("yyyyMMddHHmmss");
	}

	public static String format(Date date) {
		SimpleDateFormat format = new SimpleDateFormat(DATETIME_FORMAT);

		if (date == null) {
			return "&ltTime is not available&gt";
		} else {
			return format.format(date);
		}
	}

	public static String format(GregorianCalendar calendar) {
		if (calendar == null) {

			return format((Date) null);
		} else {
			return format(calendar.getTime());
		}
	}
}
