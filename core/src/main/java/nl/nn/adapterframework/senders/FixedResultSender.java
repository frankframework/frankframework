/*
   Copyright 2013-2016 Nationale-Nederlanden, 2021-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.senders;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedResultPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * FixedResultSender, same behaviour as {@link FixedResultPipe}, but now as a ISender.
 *
 * @ff.parameters Any parameters defined on the sender will be used for replacements. Each occurrence of <code>${name-of-parameter}</code> in the file fileName will be replaced by its corresponding value-of-parameter. This works only with files, not with values supplied in attribute {@link #setReturnString(String) returnString}.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@Category("Basic")
public class FixedResultSender extends SenderWithParametersBase {

	private @Getter String filename;
	private @Getter String returnString;
	private @Getter boolean substituteVars=false;
	private @Getter String replaceFrom = null;
	private @Getter String replaceTo = null;
	private @Getter String styleSheetName = null;

	private TransformerPool transformerPool;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(getFilename())) {
			try {
				returnString = Misc.resourceToString(ClassUtils.getResourceURL(this, getFilename()), Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("Pipe [" + getName() + "] got exception loading ["+getFilename()+"]", e);
			}
		}
		if ((StringUtils.isEmpty(getFilename())) && getReturnString()==null) {  // allow an empty returnString to be specified
			throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
		}
		if(StringUtils.isNotEmpty(getStyleSheetName())) {
			transformerPool = TransformerPool.configureStyleSheetTransformer(this, getStyleSheetName(), 0);
		}
		if (StringUtils.isNotEmpty(getReplaceFrom())) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String result=returnString;
		if (paramList!=null) {
			ParameterValueList pvl;
			try {
				pvl = paramList.getValues(message, session);
			} catch (ParameterException e) {
				throw new SenderException("exception extracting parameters",e);
			}
			if (pvl!=null) {
				for(ParameterValue pv : pvl) {
					result=replace(result,"${"+pv.getDefinition().getName()+"}",pv.asStringValue(""));
				}
			}
		}

		if (isSubstituteVars()){
			result=StringResolver.substVars(returnString, session);
		}

		if (transformerPool != null) {
			try{
				result = transformerPool.transform(Message.asSource(result));
			} catch (IOException | TransformerException e) {
				throw new SenderException(getLogPrefix()+"got error transforming message [" + result + "] with [" + getStyleSheetName() + "]", e);
			} catch (SAXException se) {
				throw new SenderException(getLogPrefix()+"got error converting string [" + result + "] to source", se);
			}
		}
		log.debug("returning fixed result [" + result + "]");
		return new SenderResult(result);
	}

	public static String replace (String target, String from, String to) {
		// target is the original string
		// from   is the string to be replaced
		// to     is the string which will used to replace
		int start = target.indexOf (from);
		if (start==-1) return target;
		int lf = from.length();
		char [] targetChars = target.toCharArray();
		StringBuilder builder = new StringBuilder();
		int copyFrom=0;
		while (start != -1) {
			builder.append (targetChars, copyFrom, start-copyFrom);
			builder.append (to);
			copyFrom=start+lf;
			start = target.indexOf (from, copyFrom);
		}
		builder.append (targetChars, copyFrom, targetChars.length-copyFrom);
		return builder.toString();
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@IbisDoc({"should values between ${ and } be resolved from the pipelinesession", "false"})
	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String fileName) {
		setFilename(fileName);
	}

	/**
	 * Sets the name of the filename. The fileName should not be specified
	 * as an absolute path, but as a resource in the classpath.
	 *
	 * @param filename the name of the file to return the contents from
	 */
	@IbisDoc({"name of the file containing the resultmessage", ""})
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@IbisDoc({"returned message", ""})
	public void setReturnString(String returnString) {
		this.returnString = returnString;
	}

	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

}
