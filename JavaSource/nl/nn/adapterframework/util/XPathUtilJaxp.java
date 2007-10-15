package nl.nn.adapterframework.util;

import java.util.List;
import java.io.InputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.core.IbisException;


public class XPathUtilJaxp extends AbstractXPathUtil implements XPathUtil {
	/**
	 * Parse XML file 'in' using given XPath expression and
	 * return the result as a list of strings.
	 */
	public List parseXpath(String xpathExpression, InputStream in) throws IbisException {
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			InputSource inputSource = new InputSource(in);
			NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, inputSource, XPathConstants.NODESET);
			List result = makeListFromNodeList(nodes);
			return result;
		} catch (Exception ex) {
			throw new IbisException("Cannot parse XPath expression '" + xpathExpression
				+ "' against XML file", ex);
		}
	 }
}
