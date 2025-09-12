/*
   Copyright 2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package org.frankframework.configuration.classloaders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.ClassLoaderManager;
import org.frankframework.configuration.IbisManager;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.testutil.TestAppender;
import org.frankframework.util.StreamUtil;

public class DatabaseClassLoaderTest extends ConfigurationClassLoaderTestBase<DatabaseClassLoader> {
	private static final String ERROR_PREFIX = "error configuring ClassLoader for configuration [";
	private static final String ERROR_SUFFIX = "]";

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
		JmsRealmFactory.getInstance().addJmsRealm(jmsRealm);
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
		doReturn(StreamUtil.streamToBytes(file.openStream())).when(rs).getBytes(anyInt());
		doReturn(rs).when(stmt).executeQuery();

		// Mock applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		IbisManager ibisManager = mock(IbisManager.class);
		doReturn(ibisManager).when(ibisContext).getIbisManager();
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(applicationContext).when(ibisManager).getApplicationContext();
		doReturn(beanFactory).when(applicationContext).getAutowireCapableBeanFactory();
		doReturn(fq).when(beanFactory).createBean(FixedQuerySender.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

		@SuppressWarnings("rawtypes") // IbisContext.log is a void method
		Answer answer = new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				String message = invocation.getArgument(0);
				MessageEventLevel level = invocation.getArgument(1);
				Exception exception = invocation.getArgument(2);
				new ApplicationMessageEvent(spy(ApplicationContext.class), message, level, exception);
				return null;
			}
		};
		// Mock the IbisContext's log method which uses getApplicationContext which in turn creates a
		// new ApplicationContext if non exists. This functionality should be removed sometime in the future.
		// During testing, the IbisContext never initialises and thus there is no ApplicationContext. The
		// creation of the ApplicationContext during the test phase causes IllegalStateExceptions
		// In turn this causes the actual thing we want to test to never be 'hit', aka the log message.
		doAnswer(answer).when(ibisContext).log(anyString(), any(MessageEventLevel.class), any(Exception.class));
	}

	/* test files that are only present in the JAR_FILE zip */
	@Test
	@SuppressWarnings("java:S2699") // Method contains indirect assertion
	public void classloaderOnlyFile() {
		resourceExists("fileOnlyOnZipClassPath.xml");
	}

	@Test
	@SuppressWarnings("java:S2699") // Method contains indirect assertion
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

		String key = "configurations."+getConfigurationName()+".reportLevel";
		appConstants.setProperty(key, "ERROR");
		ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
		ClassLoader config = null;
		boolean makeSureAnExceptionIsThrown = false;
		try {
			config = manager.get(getConfigurationName());
		}
		catch (ClassLoaderException e) {
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
		boolean makeSureNoExceptionIsThrown = false;
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			mockDatabase(true);

			String key = "configurations."+getConfigurationName()+".reportLevel";
			appConstants.setProperty(key, "DEBUG");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); // Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);

			assertTrue(makeSureNoExceptionIsThrown);

			List<LogEvent> log = appender.getLogEvents();
			LogEvent firstLogEntry = log.get(log.size()-1);
			assertEquals(ClassLoaderManager.class.getCanonicalName(), firstLogEntry.getLoggerName());
			assertEquals(Level.DEBUG, firstLogEntry.getLevel());
			String msg = firstLogEntry.getMessage().getFormattedMessage();
			assertThat(msg, Matchers.startsWith(ERROR_PREFIX));
			assertThat(msg, Matchers.endsWith(ERROR_SUFFIX));
		}
	}

	/**
	 * This test makes sure that when the config can't be found, it only throws an INFO error in the log4j logger
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingINFO() throws Exception {
		boolean makeSureNoExceptionIsThrown = false;
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			mockDatabase(true);

			String key = "configurations."+getConfigurationName()+".reportLevel";
			appConstants.setProperty(key, "INFO");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); // Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);

			assertTrue(makeSureNoExceptionIsThrown);

			List<LogEvent> log = appender.getLogEvents();
			LogEvent firstLogEntry = log.get(log.size()-1);
			assertEquals(MessageEvent.class.getCanonicalName(), firstLogEntry.getLoggerName());
			assertEquals(Level.INFO, firstLogEntry.getLevel());

			String msg = firstLogEntry.getMessage().getFormattedMessage();
			assertThat(msg, StringContains.containsString(ERROR_PREFIX));// Ignore the log4j prefix
			assertThat(msg, Matchers.endsWith(ERROR_SUFFIX));
		}
	}

	/**
	 * This test makes sure that when the config can't be found, it only throws a WARN error in the log4j logger
	 * @throws Exception
	 */
	@Test
	public void testExceptionHandlingWARN() throws Exception {
		boolean makeSureNoExceptionIsThrown = false;
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			mockDatabase(true);

			String key = "configurations."+getConfigurationName()+".reportLevel";
			appConstants.setProperty(key, "WARN");
			ClassLoaderManager manager = new ClassLoaderManager(ibisContext);
			ClassLoader config = manager.get(getConfigurationName()); // Does not throw an exception

			makeSureNoExceptionIsThrown = true;
			assertNull(config);

			List<LogEvent> log = appender.getLogEvents();
			LogEvent firstLogEntry = log.get(log.size()-1);
			assertEquals(ClassLoaderManager.class.getCanonicalName(), firstLogEntry.getLoggerName());
			assertEquals(Level.WARN, firstLogEntry.getLevel());
			String msg = firstLogEntry.getMessage().getFormattedMessage();
			assertThat(msg, Matchers.startsWith(ERROR_PREFIX));
			assertThat(msg, Matchers.endsWith(ERROR_SUFFIX));
		}
		finally {
			assertTrue(makeSureNoExceptionIsThrown);
		}
	}
}
