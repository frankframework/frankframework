/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.util;

import java.io.InputStream;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import nl.nn.adapterframework.core.IbisException;
import org.w3c.dom.Node;

/**
 *
 * @author Tim
 */
public class XPathUtilXslt extends AbstractXPathUtil implements XPathUtil {

	public List parseXpath(String xpathExpression, InputStream in) throws IbisException {
		try {
			Source s = new StreamSource(in);
			Transformer t = XmlUtils.createXPathEvaluator(xpathExpression);
			DOMResult result = new DOMResult();
			t.transform(s, result);
			Node resultNode = result.getNode();
			return makeListFromNodeList(resultNode.getChildNodes());
		} catch (Exception ex) {
			throw new IbisException("Failed to parse XML against XPath expression '"
					+ xpathExpression + "'", ex);
		}
	}

	public String parseXpathToString(String xpathExpression, InputStream in) throws IbisException {
		// NB: It's not strictly neccesary to override this method as the
		// default implementation would have done just fine, but I expect
		// this to be a more efficient implementation.
		try {
			Source s = new StreamSource(in);
			Transformer t = XmlUtils.createXPathEvaluator(xpathExpression);
			return XmlUtils.transformXml(t, s);
		} catch (Exception ex) {
			throw new IbisException("Failed to parse XML against XPath expression '"
					+ xpathExpression + "'", ex);
		}
	}
    
    
}
