/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.LogUtil;

/**
 * See Spring's native PropertySourceFactory
 * TODO use PropertySources (PropertiesPropertySource)
 */
public class JdbcPropertySourceFactory implements ApplicationContextAware {
	public static final String JDBC_PROPERTIES_KEY = "AppConstants.properties.jdbc";
	private @Setter ApplicationContext applicationContext;
	private final Logger log = LogUtil.getLogger(this);

	public Properties createPropertySource(String name) {
		return createPropertySource(name, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
	}

	public Properties createPropertySource(String name, String datasourceName) {
		JdbcFacade ibisProp = (JdbcFacade) applicationContext.getAutowireCapableBeanFactory().autowire(JdbcFacade.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		log.debug("looking up properties in database with datasouce [{}]", datasourceName);
		ibisProp.setDatasourceName(datasourceName);
		ibisProp.setName("retrieveJdbcPropertiesFromDatabase");
		try {
			ibisProp.configure();
		} catch (ConfigurationException e) {
			log.error("could not configure JdbcFacade", e);
		}

		try (Connection conn = ibisProp.getConnection()) {
			if (ibisProp.getDbmsSupport().isTablePresent(conn, "IBISPROP")) {
				Properties properties = executePropertiesQuery(conn);
				if(!properties.isEmpty()) {
					log.info("found [{}] properties in database with datasouce [{}]", properties.size(), datasourceName);
					return properties;
					//return new PropertiesPropertySource(name, properties);
				} else {
					log.debug("did not find any properties in database with datasouce [{}]", datasourceName);
				}
			} else {
				log.info("table [ibisprop] not present in database with datasouce [{}]", datasourceName);
			}
		} catch (SQLException e) {
			log.error("error opening connection, unable to read properties from database", e);
		} catch (Exception e) {
			log.error("error processing properties found in database", e);
		}

		return null;
	}

	private Properties executePropertiesQuery(Connection connection) throws JdbcException {
		Properties props = new Properties();
		String query = "SELECT NAME, VALUE FROM IBISPROP";
		if (log.isDebugEnabled()) log.debug("prepare and execute query [{}]", query);
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					props.put(rs.getString(1), rs.getString(2));
				}
				return props;
			}
		} catch (SQLException e) {
			throw new JdbcException("could not obtain value using query ["+query+"]", e);
		}
	}
}
