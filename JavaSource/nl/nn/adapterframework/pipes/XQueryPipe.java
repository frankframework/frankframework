/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: XQueryPipe.java,v $
 * Revision 1.1  2013-01-30 16:47:58  m00f069
 * Added XQueryPipe
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;

import net.sf.saxon.xqj.SaxonXQDataSource;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Perform an XQuery.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XsltPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXQueryName(String) xqueryName}</td><td>name of the file (resource) on the classpath to read the xquery from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXQueryFile(String) xqueryFile}</td><td>name of the file on the file system to read the xquery from</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be passed as external variable to the XQuery</td></tr>
 * </table>
 * </p>
 * 
 * @author Jaco de Groot
 */

public class XQueryPipe extends FixedForwardPipe {
	private String xquery;
	private String xqueryName;
	private String xqueryFile;
	private XQPreparedExpression preparedExpression;

	public void configure() throws ConfigurationException {
		super.configure();
		URL url;
		if (StringUtils.isNotEmpty(getXqueryName())) {
			url = ClassUtils.getResourceURL(this, getXqueryName());
			if (url == null) {
				throw new ConfigurationException(getLogPrefix(null) + "could not find XQuery '" + getXqueryName() + "'");
			}
		} else if (StringUtils.isNotEmpty(getXqueryFile())) {
			File file = new File(getXqueryFile());
			try {
				url = file.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigurationException(getLogPrefix(null) + "could not create url for XQuery file", e);
			}
		} else {
			throw new ConfigurationException(getLogPrefix(null) + "no XQuery name or file specified");
		}

		try {
			xquery = Misc.resourceToString(url);
		} catch (IOException e) {
			throw new ConfigurationException(getLogPrefix(null) + "could not read XQuery", e);
		}
		SaxonXQDataSource dataSource = new SaxonXQDataSource();
		XQConnection connection;
		try {
			connection = dataSource.getConnection();
			preparedExpression = connection.prepareExpression(xquery);
		} catch (XQException e) {
			throw new ConfigurationException(getLogPrefix(null) + "could not create prepared expression", e);
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "got null input");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "got an invalid type as input, expected String, got "
					+ input.getClass().getName());
		}
		try {
			String stringResult = (String)input;
			// We already specifically use Saxon in this pipe, hence set xslt2
			// to true to make XmlUtils use the Saxon
			// DocumentBuilderFactoryImpl.
			ParameterResolutionContext prc = new ParameterResolutionContext(stringResult, session, isNamespaceAware(), true);
			Map parametervalues = null;
			if (getParameterList() != null) {
				parametervalues = prc.getValueMap(getParameterList());
			}
			preparedExpression.bindDocument(XQConstants.CONTEXT_ITEM, stringResult, null, null);
			Iterator iterator = getParameterList().iterator();
			while (iterator.hasNext()) {
				Parameter parameter = (Parameter)iterator.next();
				preparedExpression.bindObject(new QName(parameter.getName()), parametervalues.get(parameter.getName()), null);
			}
			XQResultSequence resultSequence = preparedExpression.executeQuery();
			stringResult = resultSequence.getSequenceAsString(null);
			return new PipeRunResult(getForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)+" Exception on running xquery", e);
		}
	}

	public void setXqueryName(String xqueryName){
		this.xqueryName = xqueryName;
	}

	public String getXqueryName() {
		return xqueryName;
	}

	public void setXqueryFile(String xqueryFile){
		this.xqueryFile = xqueryFile;
	}

	public String getXqueryFile() {
		return xqueryFile;
	}
}
