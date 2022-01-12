package nl.nn.adapterframework.testtool;

import nl.nn.adapterframework.util.XmlUtils;

public class Util {

	public static String throwableToXml(Throwable throwable) {
		String xml = "<throwable>";
		xml = xml + "<class>" + throwable.getClass().getName() + "</class>";
		xml = xml + "<message>" + XmlUtils.encodeChars(XmlUtils.replaceNonValidXmlCharacters((throwable.getMessage()))) + "</message>";
		Throwable cause = throwable.getCause();
		if (cause != null) {
			xml = xml + "<cause>" + throwableToXml(cause) + "</cause>";
		}
		xml = xml + "</throwable>";
		return xml;
	}

}
