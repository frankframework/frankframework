package nl.nn.adapterframework.jdbc;

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
import nl.nn.adapterframework.util.LogUtil;

/**
 * See Spring's native PropertySourceFactory
 * TODO use PropertySources (PropertiesPropertySource)
 */
public class JdbcPropertySourceFactory implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;
	private Logger log = LogUtil.getLogger(this);

	public Properties createPropertySource(String name) {
		return createPropertySource(name, JndiDataSourceFactory.DEFAULT_DATASOURCE_NAME);
	}

	public Properties createPropertySource(String name, String datasourceName) {
		JdbcFacade ibisProp = (JdbcFacade) applicationContext.getAutowireCapableBeanFactory().autowire(JdbcFacade.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		log.debug("looking up properties in database with datasouce ["+datasourceName+"]");
		ibisProp.setDatasourceName(datasourceName);
		ibisProp.setName("retrieveJdbcPropertiesFromDatabase");

		try (Connection conn = ibisProp.getConnection()) {
			if (ibisProp.getDbmsSupport().isTablePresent(conn, "IBISPROP")) {
				Properties properties = executePropertiesQuery(conn);
				if(properties.size() > 0) {
					log.info("found ["+properties.size()+"] properties in database with datasouce ["+datasourceName+"]");
					return properties;
					//return new PropertiesPropertySource(name, properties);
				} else {
					log.debug("did not find any properties in database with datasouce ["+datasourceName+"]");
				}
			} else {
				log.info("table [ibisprop] not present in database with datasouce ["+datasourceName+"]");
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
		if (log.isDebugEnabled()) log.debug("prepare and execute query ["+query+"]");
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
