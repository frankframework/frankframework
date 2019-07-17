/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class PageConvertUtil {

	private static final float CM_P_INCH = 2.54f;
	private static final float DPI = 72.0f;

	private static final float DPCM = DPI / CM_P_INCH;

	public static final float PAGE_WIDHT_IN_CM = 21.0f;
	public static final float PAGE_HEIGTH_IN_CM = 29.7f;

	private PageConvertUtil() {
	}

	/**
	 * Converts centimeter to points (DPI).
	 * @param cm
	 * @return
	 */
	public static float convertCmToPoints(float cm) {
		return cm * DPCM;
	}
}
