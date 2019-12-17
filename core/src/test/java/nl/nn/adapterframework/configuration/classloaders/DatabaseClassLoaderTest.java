/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.classloaders;

import static org.junit.Assert.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ClassLoaderManager;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

public class DatabaseClassLoaderTest extends ClassLoaderTestBase<DatabaseClassLoader> {

	@Override
	public DatabaseClassLoader createClassLoader(ClassLoader parent) throws Exception {
		mockJMS();
		mockDatabase();

		DatabaseClassLoader cl = new DatabaseClassLoader(parent);
		return cl;
	}

	@Override
	protected String getScheme() {
		return "classpath";
	}

	/* only call this once! */
	private void mockJMS() {
		JmsRealm jmsRealm = spy(new JmsRealm());
		jmsRealm.setDatasourceName("fake");
		jmsRealm.setRealmName("myRealm");
		JmsRealmFactory.getInstance().registerJmsRealm(jmsRealm);
	}

	private void mockDatabase() throws Exception {
		mockDatabase(false);
	}

	private void mockDatabase(boolean throwException) throws Exception {
		// Mock a FixedQuerySender
		FixedQuerySender fq = mock(FixedQuerySender.class);
		doReturn(new GenericDbmsSupport()).when(fq).getDbmsSupport();

		Connection conn = mock(Connection.class);
		doReturn(conn).when(fq).getConnection();
		PreparedStatement stmt = mock(PreparedStatement.class);
		doReturn(stmt).when(conn).prepareStatement(anyString());
		ResultSet rs = mock(ResultSet.class);
		doReturn(!throwException).when(rs).next();
		doReturn("dummy").when(rs).getString(anyInt());
		URL file = this.getClass().getResource(JAR_FILE);
		doReturn(Misc.streamToBytes(file.openStream())).when(rs).getBytes(anyInt());
		doReturn(rs).when(stmt).executeQuery();
		doReturn(fq).when(ibisContext).createBeanAutowireByName(FixedQuerySender.class);
	}

	/* test files that are only present in the JAR_FILE zip */
	@Test
	public void classloaderOnlyFile() {
		resourceExists("fileOnlyOnZipClassPath.xml");
	}

	@Test
	public void classloaderOnlyFolder() {
		resourceExists("ClassLoader/fileOnlyOnZipClassPath.xml");
	}

	/**
	 * This test makes sure that when the config can't be found, it throws an ConfigurationException
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingERROR() throws Exception {
		mockDatabase(true);

		appConstants.put("configurations."+getConfigurationName()+".reportLevel", "ERROR");
		ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
		ClassLoader config = null;
		boolean makeSureAnExceptionIsThrown = false;
		try {
			config = manager.get(getConfigurationName());
		}
		catch (ConfigurationException e) {
			String msg = e.getMessage();
			assertTrue(msg.startsWith("Could not get config"));
			assertTrue(msg.endsWith("from database"));
			makeSureAnExceptionIsThrown = true;
		}
		finally {
			assertNull(config);
			assertTrue(makeSureAnExceptionIsThrown);
		}
	}

	/**
	 * This test makes sure that when the config can't be found, it only throws a DEBUG error in the log4j logger
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingDEBUG() throws Exception {
		TestAppender appender = new TestAppender();
		Logger logger = LogUtil.getRootLogger();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(appender);
		boolean makeSureNoExceptionIsThrown = false;
		try {
			mockDatabase(true);

			appConstants.put("configurations."+getConfigurationName()+".reportLevel", "DEBUG");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); //Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);
		}
		finally {
			logger.removeAppender(appender);
		}
		assertTrue(makeSureNoExceptionIsThrown);

		List<LoggingEvent> log = appender.getLog();
		LoggingEvent firstLogEntry = log.get(log.size()-1);
		assertEquals(ClassLoaderManager.class.getCanonicalName(), firstLogEntry.getLoggerName());
		assertEquals(Level.DEBUG, firstLogEntry.getLevel());
		String msg = (String) firstLogEntry.getMessage();
		System.out.println(msg);
		assertTrue(msg.startsWith("error configuring ClassLoader for configuration ["));
		assertTrue(msg.endsWith("]"));
	}

	/**
	 * This test makes sure that when the config can't be found, it only throws an INFO error in the log4j logger
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingINFO() throws Exception {
		TestAppender appender = new TestAppender();
		Logger logger = LogUtil.getRootLogger();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(appender);
		boolean makeSureNoExceptionIsThrown = false;
		try {
			mockDatabase(true);

			appConstants.put("configurations."+getConfigurationName()+".reportLevel", "INFO");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); //Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);
		}
		finally {
			logger.removeAppender(appender);
		}
		assertTrue(makeSureNoExceptionIsThrown);

		List<LoggingEvent> log = appender.getLog();
		LoggingEvent firstLogEntry = log.get(log.size()-1);
		assertEquals(IbisContext.class.getCanonicalName(), firstLogEntry.getLoggerName());
		assertEquals(Level.INFO, firstLogEntry.getLevel());
		String msg = (String) firstLogEntry.getMessage();
		assertTrue(msg.contains("error configuring ClassLoader for configuration [")); //Has log4j prefix
		assertTrue(msg.endsWith("]"));
	}

	/**
	 * This test makes sure that when the config can't be found, it only throws a WARN error in the log4j logger
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingWARN() throws Exception {
		TestAppender appender = new TestAppender();
		Logger logger = LogUtil.getRootLogger();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(appender);
		boolean makeSureNoExceptionIsThrown = false;
		try {
			mockDatabase(true);

			appConstants.put("configurations."+getConfigurationName()+".reportLevel", "WARN");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); //Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);
		}
		finally {
			logger.removeAppender(appender);
			assertTrue(makeSureNoExceptionIsThrown);
		}

		List<LoggingEvent> log = appender.getLog();
		LoggingEvent firstLogEntry = log.get(log.size()-1);
		assertEquals(ClassLoaderManager.class.getCanonicalName(), firstLogEntry.getLoggerName());
		assertEquals(Level.WARN, firstLogEntry.getLevel());
		String msg = (String) firstLogEntry.getMessage();
		assertTrue(msg.startsWith("error configuring ClassLoader for configuration ["));
		assertTrue(msg.endsWith("]"));
	}

	class TestAppender extends AppenderSkeleton {
		private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(final LoggingEvent loggingEvent) {
			log.add(loggingEvent);
		}

		@Override
		public void close() {
		}

		public List<LoggingEvent> getLog() {
			return new ArrayList<LoggingEvent>(log);
		}
	}
}
