package nl.nn.adapterframework.util;

import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.StringReader;
/**
 * Class to handle all kinds of conversions.
 * @version Id
 *
 * @author Gerrit van Brakel IOS
 */
public class Variant {
	public static final String version="$Id: Variant.java,v 1.3 2004-03-26 10:42:39 NNVZNL01#L180564 Exp $";
	
	private String data=null;
public Variant(Object obj) {
	this(obj.toString());
}
public Variant(String obj) {
    super();
    data = obj;
}
public Reader asReader() {
	return new StringReader(data);
}
/**
 * Insert the method's description here.
 * Creation date: (25-02-2003 16:20:38)
 * @return java.lang.String
 */
public String asString() {
	return data;
}
public InputSource asXmlInputSource() {

    StringReader sr;

    sr = new StringReader(data);

    return new InputSource(sr);
}
public Source asXmlSource() {

    StringReader sr;

    sr = new StringReader(data);

    return new StreamSource(sr);
}
}
