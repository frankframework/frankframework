/*
 * $Log: FixedErrorMessage.java,v $
 * Revision 1.4  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2006/06/20 14:09:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added stylesheet attribute
 *
 * Revision 1.1  2005/09/27 09:33:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of FixedErrorMessage
 *
 */
package nl.nn.adapterframework.errormessageformatters;

import java.net.URL;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * ErrorMessageFormatter that returns a fixed message with replacements.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td><td>name of the file containing the resultmessage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnString(String) returnString}</td><td>returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplaceFrom(String) replaceFrom}</td><td>string to search for in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplaceTo(String) replaceTo}</td><td>string that will replace each of the strings found in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the output message</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Peter Leeuwenburgh
 * @since   4.3
 * @version Id
 */
public class FixedErrorMessage extends ErrorMessageFormatter {
	private String fileName = null;
	private String returnString = null;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;

	public String format(
		String message,
		Throwable t,
		INamedObject location,
		String originalMessage,
		String messageId,
		long receivedTime) {

		String stringToReturn = getReturnString();
		if (stringToReturn==null) {
			stringToReturn="";
		}
		if (StringUtils.isNotEmpty(getFileName())) {
			try {
				stringToReturn += Misc.resourceToString(ClassUtils.getResourceURL(this,getFileName()), SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				log.error("got exception loading error message file [" + getFileName() + "]", e);
			}
		}  
		if (StringUtils.isEmpty(stringToReturn)) {
			stringToReturn = super.format(message, t, location, originalMessage, messageId, receivedTime);
		}

		if (StringUtils.isNotEmpty(getReplaceFrom())) {
			stringToReturn = Misc.replace(stringToReturn, getReplaceFrom(), getReplaceTo() );
		}

		if (StringUtils.isNotEmpty(styleSheetName)) {
			URL xsltSource = ClassUtils.getResourceURL(this, styleSheetName);
			if (xsltSource!=null) {
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(xsltSource);
					xsltResult = XmlUtils.transformXml(transformer, stringToReturn);
					stringToReturn = xsltResult;
				} catch (Throwable e) {
					log.error("got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", e);
				}
			}
		}
	
		return stringToReturn;
	}


	public void setReturnString(String string) {
		returnString = string;
	}
	public String getReturnString() {
		return returnString;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
	}


	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}
	public String getReplaceFrom() {
		return replaceFrom;
	}


	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}
	public String getReplaceTo() {
		return replaceTo;
	}

	public String getStyleSheetName() {
		return styleSheetName;
	}
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}
}