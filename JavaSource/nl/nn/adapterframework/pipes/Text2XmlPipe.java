/*
 * $Log: Text2XmlPipe.java,v $
 * Revision 1.1  2004-04-27 10:51:34  a1909356#db2admin
 * Allows the conversion from a non-xml formatted text to a simple xml
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for converting text to or from xml. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setXmlTag(String) xmlag}</td><td>the xml tag to encapsulate the text in</td><td>&nbsp;</td></tr>
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
public class Text2XmlPipe extends FixedForwardPipe {
	private String xmlTag;
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(xmlTag)) {
			throw new ConfigurationException("You have not defined xmlTag");
		}
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String resultString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><" + xmlTag + "><![CDATA[" + input + "]]></" + xmlTag + ">";	
		return new PipeRunResult(getForward(), resultString);
	}

	/**
	 * @return the xml tag to encapsulate the text in
	 */
	public String getXmlTag() {
		return xmlTag;
	}

	/**
	 * @param xmlTag
	 */
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

}

