/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.core.SenderException;
import org.frankframework.doc.Category;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.doc.ReferTo;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.senders.XsltSender;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TransformerPool.OutputType;
import org.springframework.beans.factory.InitializingBean;


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * @ff.parameters any parameters defined on the pipe will be applied to the created transformer
 *
 * @author Johan Verrips
 */
@Category("Basic")
@ElementType(ElementTypes.TRANSLATOR)
public class XsltPipe extends FixedForwardPipe implements InitializingBean {

	private String sessionKey=null;

	private final @Getter XsltSender sender = createXsltSender();

	{
		setSizeStatistics(true);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SpringUtils.autowireByName(getApplicationContext(), sender);
	}

	protected XsltSender createXsltSender() {
		return new XsltSender();
	}

	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		sender.setName(getName());
		sender.configure();
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			sender.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
	}

	@Override
	public void stop() {
		try {
			sender.close();
		} catch (SenderException e) {
			log.warn("exception closing XsltSender",e);
		}
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (Message.isEmpty(input)) {
			throw new PipeRunException(this, "got null input");
		}
		try {
			if (StringUtils.isNotEmpty(getSessionKey())) {
				input.preserve();
			}
			Message result = sender.sendMessage(input, session).getResult();
			if (StringUtils.isNotEmpty(getSessionKey())) {
				session.put(getSessionKey(), result.asString());
				return new PipeRunResult(getSuccessForward(), input);
			}
			return new PipeRunResult(getSuccessForward(), result);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on transforming input", e);
		}
	}

	/**
	 * If true, then this pipe will process the XSLT while streaming in a different thread. Can be used to switch streaming xslt off for debugging purposes
	 * @ff.default set by appconstant xslt.streaming.default
	 */
	public void setStreamingXslt(boolean streamingActive) {
		sender.setStreamingXslt(streamingActive);
	}

	@Override
	public ParameterList getParameterList() {
		return sender.getParameterList();
	}

	@Override
	public void addParameter(IParameter rhs) {
		sender.addParameter(rhs);
	}

	@ReferTo(XsltSender.class)
	public void setStyleSheetName(String stylesheetName) {
		sender.setStyleSheetName(stylesheetName);
	}

	@ReferTo(XsltSender.class)
	public void setStyleSheetNameSessionKey(String newSessionKey) {
		sender.setStyleSheetNameSessionKey(newSessionKey);
	}

	@ReferTo(XsltSender.class)
	public void setStyleSheetCacheSize(int size) {
		sender.setStyleSheetCacheSize(size);
	}

	@ReferTo(XsltSender.class)
	public void setXpathExpression(String string) {
		sender.setXpathExpression(string);
	}
	public String getXpathExpression() {
		return sender.getXpathExpression();
	}

	@ReferTo(XsltSender.class)
	public void setOmitXmlDeclaration(boolean b) {
		sender.setOmitXmlDeclaration(b);
	}

	@ReferTo(XsltSender.class)
	public void setDisableOutputEscaping(boolean b) {
		sender.setDisableOutputEscaping(b);
	}

	@ReferTo(XsltSender.class)
	public void setNamespaceDefs(String namespaceDefs) {
		sender.setNamespaceDefs(namespaceDefs);
	}
	public String getNamespaceDefs() {
		return sender.getNamespaceDefs();
	}

	@ReferTo(XsltSender.class)
	public void setOutputType(OutputType outputType) {
		sender.setOutputType(outputType);
	}

	@ReferTo(XsltSender.class)
	public void setIndentXml(boolean b) {
		sender.setIndentXml(b);
	}

	@ReferTo(XsltSender.class)
	public void setRemoveNamespaces(boolean b) {
		sender.setRemoveNamespaces(b);
	}

	@ReferTo(XsltSender.class)
	public void setHandleLexicalEvents(boolean b) {
		sender.setHandleLexicalEvents(b);
	}

	@ReferTo(XsltSender.class)
	public void setSkipEmptyTags(boolean b) {
		sender.setSkipEmptyTags(b);
	}

	@ReferTo(XsltSender.class)
	public void setXsltVersion(int xsltVersion) {
		sender.setXsltVersion(xsltVersion);
	}

	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("Please use 'storeResultInSessionKey' with preserveInput=true")
	/** If set, then the XsltPipe stores it result in the session using the supplied sessionKey, and returns its input as result */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	private String getSessionKey() {
		return sessionKey;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		sender.setName("Sender of Pipe ["+name+"]");
	}

}
