package nl.nn.adapterframework.util;

import java.util.List;
import java.io.InputStream;
import nl.nn.adapterframework.core.IbisException;


public interface XPathUtil {
	/**
	 * Parse XML file 'resourceName' using given XPath expression and
	 * return the result as a list of strings.
	 */
	List parseXpath(String xpathExpression, String resourceName) throws IbisException;
	/**
	 * Parse XML file 'resourceName' using given XPath expression and
	 * return the result as a single string.
	 */
	String parseXpathToString(String xpathExpression, String resourceName) throws IbisException;
	/**
	 * Parse XML file 'in' using given XPath expression and
	 * return the result as a list of strings.
	 */
	List parseXpath(String xpathExpression, InputStream in) throws IbisException;
	/**
	 * Parse XML file 'in' using given XPath expression and
	 * return the result as a single string.
	 */
	String parseXpathToString(String xpathExpression, InputStream in) throws IbisException;
    
}
