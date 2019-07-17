/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.DatatypeConverter;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class ConvertUtil {

	public static String convertTimestampToStr(Date xsdDateTime) {
		if (xsdDateTime != null) {
			Calendar calendar = null;
			calendar = new GregorianCalendar();
			calendar.setTime(xsdDateTime);
			return DatatypeConverter.printDateTime(calendar);
		} else {
			return null;
		}
	}

	public static Date convertToTimestamp(String xsdDateTime) {
		if (xsdDateTime == null) {
			return null;
		}
		Calendar result = DatatypeConverter.parseDateTime(xsdDateTime);
		return result == null ? null : result.getTime();
	}

}
