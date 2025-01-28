/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.ReferTo;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.senders.XsltSender;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * @ff.parameters any parameters defined on the pipe will be applied to the created transformer
 *
 * @author Johan Verrips
 */
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class XsltPipe extends FixedForwardPipe implements InitializingBean {

	private final @Getter XsltSender sender = createXsltSender();

	public XsltPipe() {
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
	public void start() {
		super.start();
		sender.start();
	}

	@Override
	public void stop() {
		sender.stop();
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (Message.isEmpty(input)) {
			throw new PipeRunException(this, "got null input");
		}

		try {
			Message result = sender.sendMessage(input, session).getResult();

			return new PipeRunResult(getSuccessForward(), result);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on transforming input", e);
		}
	}

	@Override
	public @Nonnull ParameterList getParameterList() {
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

	@Override
	public void setName(String name) {
		super.setName(name);
		sender.setName("Sender of Pipe ["+name+"]");
	}
}
