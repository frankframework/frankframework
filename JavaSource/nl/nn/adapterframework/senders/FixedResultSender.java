/*
 * $Log: FixedResultSender.java,v $
 * Revision 1.4  2009-12-04 18:23:34  m00f069
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.3  2009/11/18 17:28:03  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.2  2008/08/18 11:21:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.1  2008/05/15 15:08:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * FixedResultSender, same behaviour as {@link FixedResult}, but now as a ISender.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.FixedResultSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td>        <td>name of the file containing the resultmessage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnString(String) returnString}</td><td>returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstituteVars(boolean) substituteVars}</td><td>Should values between ${ and } be resolved from the PipeLineSession</td><td>False</td></tr>
 * <tr><td>{@link #setReplaceFrom(String) replaceFrom}</td><td>string to search for in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplaceTo(String) replaceTo}</td><td>string that will replace each of the strings found in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the output message</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr>
 *   <td><i>any</i></td><td><i>any</i></td>
 * 	 <td>Any parameters defined on the sender will be used for replacements. Each occurrence
 * 		 of <code>${name-of-parameter}</code> in the file {@link #setFileName(String) fileName} 
 *       will be replaced by its corresponding <i>value-of-parameter</i>. <br>
 *       This works only with files, not with values supplied in attribute {@link #setReturnString(String) returnString}</td>
 * </tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class FixedResultSender extends SenderWithParametersBase {

	private String fileName;
	private String returnString;
	private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;

	/**
	 * checks for correct configuration, and translates the fileName to
	 * a file, to check existence. 
	 * If a fileName was specified, the contents of the file is put in the
	 * <code>returnString</code>, so that allways the <code>returnString</code>
	 * may be returned.
	 * @throws ConfigurationException
	 */
	public void configure() throws ConfigurationException {
		super.configure();
	    
		if (StringUtils.isNotEmpty(fileName)) {
			try {
				returnString = Misc.resourceToString(ClassUtils.getResourceURL(this,fileName), SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("Pipe [" + getName() + "] got exception loading ["+fileName+"]", e);
			}
		}
		if ((StringUtils.isEmpty(fileName)) && returnString==null) {  // allow an empty returnString to be specified
			throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
		}
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
	}
 
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		message = debugSenderInput(correlationID, message);
		String result=returnString;
		try {
			if (prc!=null) {
				ParameterValueList pvl;
				try {
					pvl = prc.getValues(paramList);
				} catch (ParameterException e) {
					throw new SenderException("exception extracting parameters",e);
				}
				if (pvl!=null) {
					for (int i=0; i<pvl.size(); i++) {
						ParameterValue pv = pvl.getParameterValue(i);
						result=replace(result,"${"+pv.getDefinition().getName()+"}",pv.asStringValue(""));
					}
				}
			}
	
			if (getSubstituteVars()){
				result=StringResolver.substVars(returnString, prc.getSession());
			}
	
			if (StringUtils.isNotEmpty(styleSheetName)) {
				URL xsltSource = ClassUtils.getResourceURL(this, styleSheetName);
				if (xsltSource!=null) {
					try{
						String xsltResult = null;
						Transformer transformer = XmlUtils.createTransformer(xsltSource);
						xsltResult = XmlUtils.transformXml(transformer, result);
						result = xsltResult;
					} catch (IOException e) {
						throw new SenderException("cannot retrieve ["+ styleSheetName + "], resource [" + xsltSource.toString() + "]", e);
					} catch (TransformerConfigurationException te) {
						throw new SenderException("got error creating transformer from file [" + styleSheetName + "]", te);
					} catch (TransformerException te) {
						throw new SenderException("got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", te);
					} catch (DomBuilderException te) {
						throw new SenderException("caught DomBuilderException", te);
					}
				}
			}
	
			log.debug("returning fixed result [" + result + "]");
		} catch(Throwable throwable) {
			debugSenderAbort(correlationID, throwable);
		}
		return debugSenderOutput(correlationID, result);
	}

	public static String replace (String target, String from, String to) {   
		// target is the original string
		// from   is the string to be replaced
		// to     is the string which will used to replace
		int start = target.indexOf (from);
		if (start==-1) return target;
		int lf = from.length();
		char [] targetChars = target.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int copyFrom=0;
		while (start != -1) {
			buffer.append (targetChars, copyFrom, start-copyFrom);
			buffer.append (to);
			copyFrom=start+lf;
			start = target.indexOf (from, copyFrom);
		}
		buffer.append (targetChars, copyFrom, targetChars.length-copyFrom);
		return buffer.toString();
	}


	public boolean isSynchronous() {
		return true;
	}



	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}
	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

	/**
	 * Sets the name of the filename. The fileName should not be specified
	 * as an absolute path, but as a resource in the classpath.
	 *
	 * @param fileName the name of the file to return the contents from
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
	}

	public void setReturnString(String returnString) {
		this.returnString = returnString;
	}
	public String getReturnString() {
		return returnString;
	}

	public String getReplaceFrom() {
		return replaceFrom;
	}
	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public String getReplaceTo() {
		return replaceTo;
	}
	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public String getStyleSheetName() {
		return styleSheetName;
	}
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

}
