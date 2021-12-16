package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;

public enum TransactionManagerType {
	DATASOURCE(URLDataSourceFactory.class), 
	BTM(BTMXADataSourceFactory.class), 
	NARAYANA(NarayanaXADataSourceFactory.class);

	private @Getter URLDataSourceFactory dataSourceFactory;

	private TransactionManagerType(Class<? extends URLDataSourceFactory> clazz) {
		try {
			dataSourceFactory = clazz.newInstance();
		} catch (Exception e) {
			fail(ExceptionUtils.getStackTrace(e));
		}
	}

	public List<DataSource> getAvailableDataSources() {
		return getDataSourceFactory().getAvailableDataSources();
	}
}