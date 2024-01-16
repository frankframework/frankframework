/*
   Copyright 2019 Integration Partners

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
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
	 */
	public static float convertCmToPoints(float cm) {
		return cm * DPCM;
	}
}
