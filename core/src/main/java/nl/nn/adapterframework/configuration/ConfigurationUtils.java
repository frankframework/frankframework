/*
   Copyright 2013, 2016-2018 Nationale-Nederlanden

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
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
 * @author  Jaco de Groot
 */
public class ConfigurationUtils {
	private static Logger log = LogUtil.getLogger(ConfigurationUtils.class);

	private static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	private static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";
	private static final String ACTIVE_XSLT = "/xml/xsl/active.xsl";
	private static final String VALIDATORS_DISABLED_KEY = "validators.disabled";

	public static boolean stubConfiguration() {
		return AppConstants.getInstance().getBoolean(STUB4TESTTOOL_CONFIGURATION_KEY, false);
	}

	public static String getStubbedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		Map<String, Object> parameters = new Hashtable<String, Object>();
		// Parameter disableValidators has been used to test the impact of
		// validators on memory usage.
		parameters.put("disableValidators", AppConstants.getInstance().getBoolean(VALIDATORS_DISABLED_KEY, false));
		return getTweakedConfiguration(configuration, originalConfig, STUB4TESTTOOL_XSLT, parameters);
	}

	public static String getActivatedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		return getTweakedConfiguration(configuration, originalConfig, ACTIVE_XSLT, null);
	}

	public static String getTweakedConfiguration(Configuration configuration,
			String originalConfig, String tweakXslt,
			Map<String, Object> parameters) throws ConfigurationException {
		URL tweak_xsltSource = ClassUtils.getResourceURL(configuration.getClassLoader(), tweakXslt);
		if (tweak_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + tweakXslt + "]");
		}
		try {
			Transformer tweak_transformer = XmlUtils.createTransformer(tweak_xsltSource);
			XmlUtils.setTransformerParameters(tweak_transformer, parameters);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/active.xsl; Line #34; Column #13; java.lang.NullPointerException
			return XmlUtils.transformXml(tweak_transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + tweakXslt + "]", e);
		} catch (TransformerConfigurationException tce) {
			throw new ConfigurationException("got error creating transformer from file [" + tweakXslt + "]", tce);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + tweak_xsltSource.toString() + "] from [" + tweakXslt + "]", te);
		} catch (DomBuilderException de) {
			throw new ConfigurationException("caught DomBuilderException", de);
		}
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, null);
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, jmsRealm, null);
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm, String version) throws ConfigurationException {
		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return null;
			}
		}
		if (StringUtils.isEmpty(version)) {
			version = null; //Make sure this is null when empty!
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			String query;
			if(version == null) {//Return active config
				query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG='"+(qs.getDbmsSupport().getBooleanValue(true))+"'";
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				rs = stmt.executeQuery();
			}
			else {
				query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, version);
				rs = stmt.executeQuery();
			}
			if (rs.next()) {
				Map<String, Object> configuration = new HashMap<String, Object>(5);
				byte[] jarBytes = rs.getBytes(1);
				if(jarBytes == null) return null;

				configuration.put("CONFIG", jarBytes);
				configuration.put("VERSION", rs.getString(2));
				configuration.put("FILENAME", rs.getString(3));
				configuration.put("CREATED", rs.getString(4));
				configuration.put("USER", rs.getString(5));
				return configuration;
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

	public static boolean addConfigToDatabase(IbisContext ibisContext, String jmsRealm, boolean activate_config, boolean automatic_reload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return false;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			int updated = 0;

			if (activate_config) {
				String query = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"+(qs.getDbmsSupport().getBooleanValue(false))+"' WHERE NAME=?");
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				updated = stmt.executeUpdate();
			}
			if (updated > 0) {
				String query = ("DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION = ?");
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, version);
				stmt.execute();
			}

			String query = ("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)");
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			stmt.setString(2, version);
			stmt.setString(3, fileName);
			stmt.setBinaryStream(4, file);
			stmt.setString(5, ruser);
			stmt.setObject(6, qs.getDbmsSupport().getBooleanValue(activate_config));
			stmt.setObject(7, qs.getDbmsSupport().getBooleanValue(automatic_reload));

			return stmt.execute();
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
	}

	public static boolean makeConfigActive(IbisContext ibisContext, String name, String version, String jmsRealm) throws ConfigurationException {
		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return false;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();

		try {
			qs.open();
			conn = qs.getConnection();
			int updated = 0;

			String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
			PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
			selectStmt.setString(1, name);
			selectStmt.setString(2, version);
			rs = selectStmt.executeQuery();
			if(rs.next()) {
				String query = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"+(qs.getDbmsSupport().getBooleanValue(false))+"' WHERE NAME=?");
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				updated = stmt.executeUpdate();
	
				if(updated > 0) {
					String query2 = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"+(qs.getDbmsSupport().getBooleanValue(true))+"' WHERE NAME=? AND VERSION=?");
					PreparedStatement stmt2 = conn.prepareStatement(query2);
					stmt2.setString(1, name);
					stmt2.setString(2, version);
					return (stmt2.executeUpdate() > 0) ? true : false;
				}
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
		return false;
	}
}