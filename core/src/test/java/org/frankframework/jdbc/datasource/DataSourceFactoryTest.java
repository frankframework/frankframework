package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

public class DataSourceFactoryTest {

	@Test
	public void testCanFindH2() throws Exception {
		DataSourceFactory factory = new DataSourceFactory();
		ResourceObjectLocator rbof = new ResourceObjectLocator();
		rbof.afterPropertiesSet();
		factory.setObjectLocators(List.of(rbof));
		DataSource ds = factory.getDataSource("jdbc/H2");
		assertNotNull(ds);
	}
}
