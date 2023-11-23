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
package nl.nn.adapterframework.pipes;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.doc.SupportsOutputStreaming;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.senders.XsltSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * @ff.parameters any parameters defined on the pipe will be applied to the created transformer
 *
 * @author Johan Verrips
 */
@Category("Basic")
@SupportsOutputStreaming
@ElementType(ElementTypes.TRANSLATOR)
public class XsltPipe extends StreamingPipe implements InitializingBean {

	private String sessionKey=null;

	private @Getter XsltSender sender = createXsltSender();

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
	public boolean canStreamToNextPipe() {
		return super.canStreamToNextPipe() && StringUtils.isEmpty(getSessionKey());
	}

	@Override
	protected boolean canProvideOutputStream() {
		return super.canProvideOutputStream() && StringUtils.isEmpty(getSessionKey());
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (Message.isEmpty(input)) {
			throw new PipeRunException(this, "got null input");
		}
		try {
			IForwardTarget nextPipe;
			if (canStreamToNextPipe()) {
				nextPipe = getNextPipe();
			} else {
				nextPipe=null;
				if (StringUtils.isNotEmpty(getSessionKey())) {
					input.preserve();
				}
			}
			PipeRunResult prr = sender.sendMessage(input, session, nextPipe);
			Message result = prr.getResult();
			PipeForward forward = prr.getPipeForward();
			if (nextPipe==null || forward.getPath()==null) {
				forward=getSuccessForward();
			}
			if (StringUtils.isNotEmpty(getSessionKey())) {
				session.put(getSessionKey(), result.asString());
				return new PipeRunResult(forward, input);
			}
			return new PipeRunResult(forward, result);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on transforming input", e);
		}
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}

	/**
	 * If true, then this pipe will process the XSLT while streaming in a different thread. Can be used to switch streaming xslt off for debugging purposes
	 * @ff.default set by appconstant xslt.streaming.default
	 */
	public void setStreamingXslt(boolean streamingActive) {
		sender.setStreamingXslt(streamingActive);
	}


	@Override
	protected MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		return sender.provideOutputStream(session, getNextPipe());
	}

	@Override
	public ParameterList getParameterList() {
		return sender.getParameterList();
	}

	@Override
	public void addParameter(Parameter rhs) {
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
	@ReferTo(XsltSender.class)
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	@ConfigurationWarning("It's value is now auto detected. If necessary, replace with a setting of xsltVersion")
	public void setXslt2(boolean b) {
		sender.setXslt2(b);
	}

	@Deprecated
	@ConfigurationWarning("Please use 'storeResultInSessionKey' with preserveInput=true")
	/** If set, then the XsltPipe stores it result in the session using the supplied sessionKey, and returns its input as result */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	@Deprecated
	public String getSessionKey() {
		return sessionKey;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		sender.setName("Sender of Pipe ["+name+"]");
	}

}
