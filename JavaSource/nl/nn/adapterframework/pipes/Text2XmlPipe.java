/*
 * $Log: Text2XmlPipe.java,v $
 * Revision 1.7  2008-10-07 10:48:41  europe\m168309
 * added replaceNonXmlChars attribute
 *
 * Revision 1.6  2008/09/23 09:17:21  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed typo xmlag -> xmlTag in javadoc
 *
 * Revision 1.5  2006/04/28 06:19:37  Martijn IJsselmuiden <martijn.ijsselmuiden@ibissource.org>
 * fixed exception text
 *
 * Revision 1.4  2006/04/26 11:29:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added splitLines attribute
 *
 * Revision 1.3  2004/10/14 15:36:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added includeXmlDeclaration-attribute
 *
 * Revision 1.2  2004/04/27 11:42:40  unknown <unknown@ibissource.org>
 * Access properties via getters
 *
 * Revision 1.1  2004/04/27 10:51:34  unknown <unknown@ibissource.org>
 * Allows the conversion from a non-xml formatted text to a simple xml
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for converting text to or from xml. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setXmlTag(String) xmlTag}</td><td>the xml tag to encapsulate the text in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIncludeXmlDeclaration(boolean) includeXmlDeclaration}</td><td>controls whether a declation is included above the Xml text</td><td>true</td></tr>
 * <tr><td>{@link #setSplitLines(boolean) splitLines}</td><td>controls whether the lines of the input are places in separated &lt;line&gt; tags</td><td>false</td></tr>
 * <tr><td>{@link #setReplaceNonXmlChars(boolean) replaceNonXmlChars}</td><td>Replace all non XML chars (not in the <a href="http://www.w3.org/TR/2006/REC-xml-20060816/#NT-Char">character range as specified by the XML specification</a>) with the inverted question mark (0x00BF)</td><td>true</td></tr>
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
	private boolean includeXmlDeclaration = true;
	private boolean splitLines = false;
	private boolean replaceNonXmlChars = true;
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("You have not defined xmlTag");
		}
	}
	
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (isSplitLines() && input != null) {
			try {
				Reader reader = new StringReader(input.toString());
				if (replaceNonXmlChars) {
					reader = new EncapsulatingReader(reader, "", "", true);
				}
				BufferedReader br = new BufferedReader(reader);

				String l;
				StringBuffer result = new StringBuffer();

				while ((l = br.readLine()) != null) {
					result.append("<line><![CDATA["+l+"]]></line>");
				}
					
				input = result.toString();
				br.close();
			}
			catch (IOException e) {
				throw new PipeRunException(this, "Unexpected exception during splitting", e); 
			}
			
						
		} else if (replaceNonXmlChars && input != null) {
			input = "<![CDATA["+ XmlUtils.encodeCdataString(input.toString()) +"]]>";
		} else {
			input = "<![CDATA["+ input +"]]>";
		}
			
		String resultString = (isIncludeXmlDeclaration()?"<?xml version=\"1.0\" encoding=\"UTF-8\"?>":"") +
		"<" + getXmlTag() + ">"+input+"</" + xmlTag + ">";	
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

	public boolean isIncludeXmlDeclaration() {
		return includeXmlDeclaration;
	}

	public void setIncludeXmlDeclaration(boolean b) {
		includeXmlDeclaration = b;
	}

	public boolean isSplitLines() {
		return splitLines;
	}

	public void setSplitLines(boolean b) {
		splitLines = b;
	}

	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

}

