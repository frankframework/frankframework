/*
 * $Log: XPathPipe.java,v $
 * Revision 1.1  2004-04-27 10:52:17  a1909356#db2admin
 * Pipe that evaluates an xpath expression on the inpup
 * 
 */
package nl.nn.adapterframework.pipes;

import java.io.ByteArrayInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>Expression to evaluate</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @version Id
 */
public class XPathPipe extends FixedForwardPipe {
	private String xpathExpression;
	private Transformer transformer;
	
	/* 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(xpathExpression))
			throw new ConfigurationException("xpathExpression must be filled");
			
		String xsl = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
			"<xsl:output method=\"text\"/>" +
			"<xsl:strip-space elements=\"*\"/>" +
   			"<xsl:template match=\"/\">" +
			"<xsl:value-of select=\"" + xpathExpression + "\"/>" +
			"</xsl:template>" +
			"</xsl:stylesheet>";
			
		try {
			transformer = XmlUtils.createTransformer(new StreamSource(new ByteArrayInputStream(xsl.getBytes())));
		}
		catch(TransformerConfigurationException e) {
			throw new ConfigurationException(e);
		}
	}

	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String in = (String)input;
		String out = null; 
		
		if (! StringUtils.isEmpty(in)) {
			try {
				out = XmlUtils.transformXml(transformer, new StreamSource(new ByteArrayInputStream(in.getBytes())));
			} 
			catch (Exception e) {
				throw new PipeRunException(this, "Error during xsl transformation", e);
			}
		}
		return new PipeRunResult(getForward(), out);
	}
	
	/**
	 * @return the xpath expression to evaluate
	 */
	public String getXpathExpression() {
		return xpathExpression;
	}

	/**
	 * @param xpathExpression the xpath expression to evaluate
	 */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}

}
