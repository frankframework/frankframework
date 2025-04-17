/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;

import org.apache.commons.lang3.StringUtils;

import com.saxonica.xqj.SaxonXQDataSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.IParameter;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StreamUtil;

/**
 * Perform an XQuery.
 *
 * @ff.parameters any parameters defined on the pipe will be passed as external variable to the XQuery
 *
 * @author Jaco de Groot
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class XQueryPipe extends FixedForwardPipe {

	private String xqueryName;
	private String xqueryFile;
	private XQPreparedExpression preparedExpression;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		URL url;
		if (StringUtils.isNotEmpty(getXqueryName())) {
			url = ClassLoaderUtils.getResourceURL(this, getXqueryName());
			if (url == null) {
				throw new ConfigurationException("could not find XQuery '" + getXqueryName() + "'");
			}
		} else if (StringUtils.isNotEmpty(getXqueryFile())) {
			File file = new File(getXqueryFile());
			try {
				url = file.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigurationException("could not create url for XQuery file", e);
			}
		} else {
			throw new ConfigurationException("no XQuery name or file specified");
		}

		String xquery;
		try {
			xquery = StreamUtil.resourceToString(url);
		} catch (IOException e) {
			throw new ConfigurationException("could not read XQuery", e);
		}
		SaxonXQDataSource dataSource = new SaxonXQDataSource();
		XQConnection connection;
		try {
			connection = dataSource.getConnection();
			preparedExpression = connection.prepareExpression(xquery);
		} catch (XQException e) {
			throw new ConfigurationException("could not create prepared expression", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, "got null input");
		}
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		try {
			String stringResult = input;
			preparedExpression.bindDocument(XQConstants.CONTEXT_ITEM, stringResult, null, null);
			Map<String, Object> parametervalues = getParameterList().getValues(message, session).getValueMap();
			for(IParameter parameter : getParameterList()) {
				preparedExpression.bindObject(new QName(parameter.getName()), parametervalues.get(parameter.getName()), null);
			}
			XQResultSequence resultSequence = preparedExpression.executeQuery();
			stringResult = resultSequence.getSequenceAsString(null);
			return new PipeRunResult(getSuccessForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on running xquery", e);
		}
	}

	/** name of the file (resource) on the classpath to read the xquery from */
	public void setXqueryName(String xqueryName){
		this.xqueryName = xqueryName;
	}

	public String getXqueryName() {
		return xqueryName;
	}

	/** name of the file on the file system to read the xquery from */
	public void setXqueryFile(String xqueryFile){
		this.xqueryFile = xqueryFile;
	}

	public String getXqueryFile() {
		return xqueryFile;
	}
}
