/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.errormessageformatters;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

/**
 * Applies a XSLT-stylesheet to the standard error generated by an {@link ErrorMessageFormatter}.
 *
 * If the transformation does not succeed, this 'standard' error message is returned and an exception is logged.
 *
 * Hint: use <code>xpathExression="/errorMessage/@message"</code> for a single compact string as errormessage.
 *
 * @author Johan Verrips IOS
 */
public class XslErrorMessageFormatter extends ErrorMessageFormatter {

	protected ParameterList paramList = null;

	private @Getter String styleSheet;
	private @Getter String xpathExpression;

	@Override
	public Message format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime) {

		Message result = super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);

		if (StringUtils.isNotEmpty(getStyleSheet()) || StringUtils.isNotEmpty(getXpathExpression())) {
			try {
				TransformerPool transformerPool;

				if (StringUtils.isNotEmpty(getStyleSheet())) {
					Resource xsltSource = Resource.getResource(this, getStyleSheet());
					transformerPool = TransformerPool.getInstance(xsltSource, 0);
				} else {
					transformerPool = TransformerPool.getXPathTransformerPool(null, getXpathExpression(), OutputType.TEXT, false, getParameterList());
				}

				Map<String, Object> parameterValues = null;
				if (getParameterList() != null) {
					try {
						getParameterList().configure();
					} catch (ConfigurationException e) {
						log.error("exception while configuring parameters", e);
					}

					try {
						parameterValues = getParameterList().getValues(new Message(errorMessage), new PipeLineSession()).getValueMap();
					} catch (ParameterException e) {
						log.error("got exception extracting parameters", e);
					}
				}
				result = Message.asMessage(transformerPool.transform(Message.asSource(result), parameterValues));
			} catch (IOException e) {
				log.error(" cannot retrieve [{}]", getStyleSheet(), e);
			} catch (TransformerConfigurationException te) {
				log.error("got error creating transformer from file [{}]", getStyleSheet(), te);
			} catch (Exception tfe) {
				log.error("could not transform [{}] using stylesheet [{}]", result, getStyleSheet(), tfe);
			}
		} else {
			log.warn("no stylesheet or xpathExpresstion defined for XslErrorMessageFormatter");
		}

		return result;
	}

	public void addParameter(Parameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	public ParameterList getParameterList() {
		return paramList;
	}

	/**
	 * URL to the stylesheet used to transform the output of the standard {@link ErrorMessageFormatter}
	 */
	public void setStyleSheet(String newStyleSheet) {
		styleSheet = newStyleSheet;
	}

	/** xPathExpression to use for transformation */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
}