/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Functions to manipulate the configuration. 
 *
 * @author  Peter Leeuwenburgh
 */
public class ConfigurationUtils {
	protected static Logger log = LogUtil.getLogger(ConfigurationUtils.class);

	private static final String CONFIGURATION_STUB4TESTTOOL_KEY = "stub4testtool.configuration";
	private static final String VALIDATORS_DISABLED_KEY = "validators.disabled";

	private static String stub4testtool_xslt = "/xml/xsl/stub4testtool.xsl";
	private static String active_xslt = "/xml/xsl/active.xsl";

	public static String getActivatedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		URL active_xsltSource = ClassUtils.getResourceURL(configuration.getClassLoader(), active_xslt);
		if (active_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + active_xslt + "]");
		}
		try {
			Transformer active_transformer = XmlUtils.createTransformer(active_xsltSource);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/active.xsl; Line #34; Column #13; java.lang.NullPointerException
			return XmlUtils.transformXml(active_transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + active_xslt + "]", e);
		} catch (TransformerConfigurationException tce) {
			throw new ConfigurationException("got error creating transformer from file [" + active_xslt + "]", tce);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + active_xsltSource.toString() + "] from [" + active_xslt + "]", te);
		} catch (DomBuilderException de) {
			throw new ConfigurationException("caught DomBuilderException", de);
		}
	}

	public static String getStubbedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		URL stub4testtool_xsltSource = ClassUtils.getResourceURL(configuration.getClassLoader(), stub4testtool_xslt);
		if (stub4testtool_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + stub4testtool_xslt + "]");
		}
		try {
			Transformer stub_transformer = XmlUtils.createTransformer(stub4testtool_xsltSource);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/stub4testtool.xsl; Line #210; Column #13; java.lang.NullPointerException
			Map parameters = new Hashtable();
			parameters.put("disableValidators", AppConstants.getInstance().getBoolean(VALIDATORS_DISABLED_KEY, false));
			XmlUtils.setTransformerParameters(stub_transformer, parameters);
			return XmlUtils.transformXml(stub_transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + stub4testtool_xslt + "]", e);
		} catch (TransformerConfigurationException tce) {
			throw new ConfigurationException("got error creating transformer from file [" + stub4testtool_xslt + "]", tce);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + stub4testtool_xsltSource.toString() + "] from [" + stub4testtool_xslt + "]", te);
		} catch (DomBuilderException de) {
			throw new ConfigurationException("caught DomBuilderException", de);
		}
	}

	public static boolean stubConfiguration() {
		return AppConstants.getInstance().getBoolean(CONFIGURATION_STUB4TESTTOOL_KEY, false);
	}

	public static byte[] getConfigFromDatabase(IbisContext ibisContext, String name) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, null);
	}

	public static byte[] getConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm)
			throws ConfigurationException {
		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance()
					.getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return null;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			String query = "SELECT CONFIG FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG= 'TRUE'";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getBytes(1);
			}
		} catch (SenderException e) {
			throw new ConfigurationException(e);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.close();
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					log.warn("Could not close resultset", e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
		return null;
	}
}