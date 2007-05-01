/*
 * $Log: FixedResultSender.java,v $
 * Revision 1.1  2007-05-01 14:10:23  europe\L190409
 * introduction of FixedResultSender, modeled after FixedResult-pipe
 *
 */
package nl.nn.adapterframework.pipes;

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
 * @author  Gerrit van Brakel
 * @since   4.6.0
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
		String result=returnString;

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
	
		return result;
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
