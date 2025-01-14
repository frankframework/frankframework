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
package org.frankframework.senders;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedResultPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringResolver;
import org.frankframework.util.TransformerPool;

/**
 * FixedResultSender, same behaviour as {@link FixedResultPipe}, but now as a ISender.
 *
 * @ff.parameters Any parameters defined on the sender will be used for replacements. Each occurrence of <code>${name-of-parameter}</code> in the file fileName will be replaced by its corresponding value-of-parameter. This works only with files, not with values supplied in attribute {@link #setReturnString(String) returnString}.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@Category(Category.Type.BASIC)
public class FixedResultSender extends AbstractSenderWithParameters {

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
				returnString = StreamUtil.resourceToString(ClassLoaderUtils.getResourceURL(this, getFilename()), Misc.LINE_SEPARATOR);
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
			returnString = returnString.replace(replaceFrom, replaceTo);
		}
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
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
					String from = "${"+pv.getDefinition().getName()+"}";
					String to = pv.asStringValue("");
					result= result.replace(from, to);
				}
			}
		}

		if (isSubstituteVars()){
			result=StringResolver.substVars(returnString, session);
		}

		if (transformerPool != null) {
			try{
				result = transformerPool.transform(result);
			} catch (IOException | TransformerException e) {
				throw new SenderException("got error transforming message [" + result + "] with [" + getStyleSheetName() + "]", e);
			} catch (SAXException se) {
				throw new SenderException("got error converting string [" + result + "] to source", se);
			}
		}
		log.debug("returning fixed result [{}]", result);
		return new SenderResult(result);
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	/**
	 * should values between ${ and } be resolved from the pipelinesession
	 * @ff.default false
	 */
	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}

	/** Name of the file containing the result message */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** returned message */
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
