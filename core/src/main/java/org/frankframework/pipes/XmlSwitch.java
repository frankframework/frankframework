/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlUtils;


/**
 * Selects an exitState, based on either the content of the input message, by means
 * of a XSLT-stylesheet, the content of a session variable or, by default, by returning the name of the root-element.
 *
 * @author Johan Verrips
 */
@Forward(name = "*", description = "name of the root-element")
@Forward(name = "*", description = "result of transformation, when <code>styleSheetName</code> or <code>xpathExpression</code> is specified")
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class XmlSwitch extends AbstractPipe {

	public static final String XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT = "Switch: Forward Found";
	public static final String XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT = "Switch: Forward Not Found";

	private @Getter String styleSheetName = null;
	private @Getter String xpathExpression = null;
	private @Getter String namespaceDefs = null;
	private @Getter String storeForwardInSessionKey = null;
	private @Getter String notFoundForwardName = null;
	private @Getter String emptyForwardName = null;
	private @Getter int xsltVersion = 0; // set to 0 for auto-detect.
	private @Getter String forwardNameSessionKey = null;
	private @Getter boolean namespaceAware = XmlUtils.isNamespaceAwareByDefault();

	private TransformerPool transformerPool = null;

	/**
	 * If no {@link #setStyleSheetName(String) styleSheetName} is specified, the
	 * switch uses the root node.
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();
		if (getNotFoundForwardName() != null && findForward(getNotFoundForwardName()) == null) {
			ConfigurationWarnings.add(this, log, "has a notFoundForwardName attribute. However, this forward [" + getNotFoundForwardName() + "] is not configured.");
		}

		if (getEmptyForwardName() != null && findForward(getEmptyForwardName()) == null) {
			ConfigurationWarnings.add(this, log, "has a emptyForwardName attribute. However, this forward [" + getEmptyForwardName() + "] is not configured.");
		}

		if (StringUtils.isNotEmpty(getXpathExpression()) || StringUtils.isNotEmpty(getStyleSheetName())) {
			transformerPool = TransformerPool.configureTransformer0(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), OutputType.TEXT, false, getParameterList(), getXsltVersion());
		} else {
			transformerPool = XmlUtils.getGetRootNodeNameTransformerPool();
		}

		registerEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
		registerEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
	}

	@Override
	public void start() {
		super.start();
		if (transformerPool != null) {
			transformerPool.open();
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (transformerPool != null) {
			transformerPool.close();
		}
	}

	/**
	 * This is where the action takes place, the switching is done. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.<br/>
	 * As WebLogic has the problem that when a not well-formed XML stream is given to
	 * `weblogic.xerces` the transformer gets corrupt, on an exception the configuration is done again, so that the
	 * transformer is re-initialized.
	 */
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward = getForward(message, session);

		log.debug("determined forward [{}]", forward);

		PipeForward pipeForward = getPipeForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException(this, "cannot find forward or pipe named [" + forward + "]");
		}
		if (StringUtils.isNotEmpty(getStoreForwardInSessionKey())) {
			session.put(getStoreForwardInSessionKey(), pipeForward.getName());
		}

		return new PipeRunResult(pipeForward, message);
	}

	private String getForward(Message message, PipeLineSession session) throws PipeRunException {
		if (StringUtils.isNotEmpty(getForwardNameSessionKey())) {
			return session.getString(getForwardNameSessionKey());
		}

		if (!(StringUtils.isEmpty(getXpathExpression()) && StringUtils.isEmpty(getStyleSheetName())) || StringUtils.isEmpty(getGetInputFromSessionKey())) {
			try {
				Map<String, Object> parametervalues = null;
				ParameterList parameterList = getParameterList();

				if (!parameterList.isEmpty()) {
					parametervalues = parameterList.getValues(message, session, isNamespaceAware()).getValueMap();
				}

				message.preserve();
				return transformerPool.transform(message, parametervalues, isNamespaceAware());
			} catch (Throwable e) {
				throw new PipeRunException(this, "got exception on transformation", e);
			}
		}

		if (StringUtils.isNotEmpty(getGetInputFromSessionKey())) {
			try {
				// Use the message as forward if none of the cases above apply
				return message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "Error reading message", e);
			}
		}

		return null;
	}

	private PipeForward getPipeForward(String forward) {
		PipeForward pipeForward;
		if (StringUtils.isEmpty(forward) && getEmptyForwardName() != null) {
			throwEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
			pipeForward = findForward(getEmptyForwardName());
		} else {
			if (findForward(forward) != null) {
				throwEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
				pipeForward = findForward(forward);
			} else {
				log.info("determined forward [{}], which is not defined. Will use [{}] instead", forward, getNotFoundForwardName());
				throwEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
				pipeForward = findForward(getNotFoundForwardName());
			}
		}
		return pipeForward;
	}

	/**
	 * stylesheet may return a string representing the forward to look up
	 *
	 * @ff.default <i>a stylesheet that returns the name of the root-element</i>
	 */
	public void setStyleSheetName(String styleSheetName) {
		this.styleSheetName = styleSheetName;
	}

	/** xpath-expression that returns a string representing the forward to look up. It's possible to refer to a parameter (which e.g. contains a value from a sessionkey) by using the parameter name prefixed with $ */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}

	/** Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some use other cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace. */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/** Forward returned when the pipename derived from the stylesheet could not be found. */
	public void setNotFoundForwardName(String notFound) {
		notFoundForwardName = notFound;
	}

	/** Forward returned when the content, on which the switch is performed, is empty. if <code>emptyforwardname</code> is not specified, <code>notfoundforwardname</code> is used. */
	public void setEmptyForwardName(String empty) {
		emptyForwardName = empty;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto-detect
	 *
	 * @ff.default 0
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion = xsltVersion;
	}

	/** Selected forward name will be stored in the specified session key. */
	public void setStoreForwardInSessionKey(String storeForwardInSessionKey) {
		this.storeForwardInSessionKey = storeForwardInSessionKey;
	}

	/** Session key that will be used to get the forward name from. */
	public void setForwardNameSessionKey(String forwardNameSessionKey) {
		this.forwardNameSessionKey = forwardNameSessionKey;
	}

	/**
	 * controls namespace-awareness of XSLT transformation
	 *
	 * @ff.default true
	 */
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
}
