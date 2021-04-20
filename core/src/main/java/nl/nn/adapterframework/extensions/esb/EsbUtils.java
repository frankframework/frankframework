/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Some utilities for working with Esb.
 * 
 * @author Peter Leeuwenburgh
 */
public class EsbUtils {
	protected static Logger log = LogUtil.getLogger(EsbUtils.class);

	public static String receiveMessageAndMoveToErrorStorage(
			EsbJmsListener esbJmsListener, JdbcTransactionalStorage errorStorage) {
		String result = null;

		PoolingConnectionFactory jmsConnectionFactory = null;
		PoolingDataSource jdbcDataSource = null;
		BitronixTransactionManager btm = null;
		javax.jms.Connection jmsConnection = null;

		try {
			jmsConnectionFactory = getPoolingConnectionFactory(esbJmsListener);
			if (jmsConnectionFactory != null) {
				jdbcDataSource = getPoolingDataSource(errorStorage);
				if (jdbcDataSource != null) {
					String instanceNameLc = AppConstants.getInstance()
							.getString("instance.name.lc", null);
					String logDir = AppConstants.getInstance().getString(
							"log.dir", null);
					TransactionManagerServices.getConfiguration().setServerId(
							instanceNameLc + ".tm");
					TransactionManagerServices.getConfiguration()
							.setLogPart1Filename(
									logDir + File.separator + instanceNameLc
											+ "-btm1.tlog");
					TransactionManagerServices.getConfiguration()
							.setLogPart2Filename(
									logDir + File.separator + instanceNameLc
											+ "-btm2.tlog");
					btm = TransactionManagerServices.getTransactionManager();

					jmsConnection = jmsConnectionFactory.createConnection();

					Session jmsSession = null;
					MessageConsumer jmsConsumer = null;

					java.sql.Connection jdbcConnection = null;

					btm.begin();
					log.debug("started transaction ["
							+ btm.getCurrentTransaction().getGtrid() + "]");

					try {
						jmsSession = jmsConnection.createSession(true,
								Session.AUTO_ACKNOWLEDGE);
						String queueName = esbJmsListener
								.getPhysicalDestinationShortName();
						Queue queue = jmsSession.createQueue(queueName);
						jmsConsumer = jmsSession.createConsumer(queue);

						jmsConnection.start();

						long timeout = 30000;
						log.debug("looking for message on queue [" + queueName
								+ "] with timeout of [" + timeout + "] msec");
						Message rawMessage = jmsConsumer.receive(timeout);

						if (rawMessage == null) {
							log.debug("no message found on queue [" + queueName
									+ "]");
						} else {
							String id = rawMessage.getJMSMessageID();
							log.debug("found message on queue [" + queueName
									+ "] with messageID [" + id + "]");
							Serializable sobj = null;
							if (rawMessage != null) {
								if (rawMessage instanceof Serializable) {
									sobj = (Serializable) rawMessage;
								} else {
									try {
										sobj = new MessageWrapper(rawMessage,
												esbJmsListener);
									} catch (ListenerException e) {
										log.error(
												"could not wrap non serializable message for messageId ["
														+ id + "]", e);
										if (rawMessage instanceof TextMessage) {
											TextMessage textMessage = (TextMessage) rawMessage;
											sobj = textMessage.getText();
										} else {
											sobj = rawMessage.toString();
										}
									}
								}
							}

							jdbcConnection = jdbcDataSource.getConnection();

							result = errorStorage.storeMessage(jdbcConnection,
									id, id,
									new Date(System.currentTimeMillis()),
									"moved message", null, sobj);
						}

						log.debug("committing transaction ["
								+ btm.getCurrentTransaction().getGtrid() + "]");
						btm.commit();
					} catch (Exception e) {
						if (btm.getCurrentTransaction() != null) {
							log.debug("rolling back transaction ["
									+ btm.getCurrentTransaction().getGtrid()
									+ "]");
							btm.rollback();
						}
						log.error(
								"exception on receiving message and moving to errorStorage",
								e);
					} finally {
						if (jdbcConnection != null) {
							jdbcConnection.close();
						}
						if (jmsConnection != null) {
							jmsConnection.stop();
						}
						if (jmsConsumer != null) {
							jmsConsumer.close();
						}
						if (jmsSession != null) {
							jmsSession.close();
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(
					"exception on receiving message and moving to errorStorage",
					e);
		} finally {
			if (jmsConnection != null) {
				try {
					jmsConnection.close();
				} catch (JMSException e) {
					log.warn("exception on closing connection", e);
				}
			}
			if (jmsConnectionFactory != null) {
				jmsConnectionFactory.close();
			}
			if (jdbcDataSource != null) {
				jdbcDataSource.close();
			}
			if (btm != null) {
				btm.shutdown();
			}
		}
		return result;
	}

	public static String getProviderUrl(EsbJmsListener esbJmsListener) {
		EsbConnectionFactoryInfo ecfi = getEsbConnectionFactoryInfo(esbJmsListener);
		if (ecfi != null) {
			return ecfi.getUrl();
		}
		return null;
	}

	private static PoolingConnectionFactory getPoolingConnectionFactory(
			EsbJmsListener esbJmsListener) {
		EsbConnectionFactoryInfo ecfi = getEsbConnectionFactoryInfo(esbJmsListener);
		if (ecfi != null) {
			return setupJmsConnectionFactory(ecfi.getUrl(), ecfi.getUserName(),
					ecfi.getPassword());
		}
		return null;
	}

	public static PoolingDataSource getPoolingDataSource(JdbcTransactionalStorage errorStorage) {
		String dsUrl = null;
		String dsUserName = null;
		String dsPassword = null;

		java.sql.Connection errorStorageConnection = null;
		try {
			errorStorageConnection = errorStorage.getConnection();
		} catch (JdbcException e) {
			log.warn("error occured during getting errorStorage connection: " + e.getMessage());
		}
		if (errorStorageConnection == null) {
			log.warn("could not get errorStorage connection");
		} else {
			DatabaseMetaData md;
			try {
				md = errorStorageConnection.getMetaData();
				dsUrl = md.getURL();
				// dsUserName = md.getUserName();
				// dsPassword = md.getPassword();
			} catch (SQLException e) {
				log.warn("error occured during getting errorStorage metadata: " + e.getMessage());
			}

			if (dsUrl == null) {
				log.warn("could not get errorStorage url");
			} else {
				// onderstaande is nodig omdat het niet mogelijk is het
				// password op te vragen uit het DatabaseMetaData object of
				// uit het Connection object
				String confResString = null;
				try {
					confResString = Misc.getConfigurationResources();
					if (confResString != null) {
						confResString = XmlUtils.removeNamespaces(confResString);
					}
				} catch (IOException e) {
					log.warn("error getting configuration resources: " + e.getMessage());
				}
				String authDataAlias = null;
				if (confResString != null) {
					String dsName = errorStorage.getDatasourceName();
					if (dsName != null) {
						String xpathExpression = "XMI/JDBCProvider/factories[@jndiName='" + dsName + "']/@authDataAlias";
						try {
							Transformer t = XmlUtils.createXPathEvaluator(xpathExpression);
							authDataAlias = XmlUtils.transformXml(t, confResString);
						} catch (Exception e) {
							log.warn("error getting datasource authDataAlias: " + e.getMessage());
						}
					}
				}

				if (StringUtils.isEmpty(authDataAlias)) {
					log.warn("could not get errorStorage authDataAlias");
				} else {
					CredentialFactory cf = new CredentialFactory(authDataAlias, null, null);
					dsUserName = cf.getUsername();
					dsPassword = cf.getPassword();
					return setupJdbcDataSource(dsUrl, dsUserName, dsPassword);
				}
			}
		}
		return null;
	}

	private static EsbConnectionFactoryInfo getEsbConnectionFactoryInfo(EsbJmsListener esbJmsListener) {
		String cfUrl = null;
		String cfUserName = null;
		String cfPassword = null;

		Object managedConnectionFactory = null;
		try {
			managedConnectionFactory = esbJmsListener
					.getManagedConnectionFactory();
		} catch (JmsException e) {
			log.warn("error occured during getting managed connection factory: "
					+ e.getMessage());
		}
		if (managedConnectionFactory == null) {
			log.warn("could not get managed connection factory");
		} else {
			String contextFactoryClassname = getContextFactoryClassname(managedConnectionFactory);
			if (contextFactoryClassname == null) {
				log.warn("could not get context factory");
			} else {
				cfUrl = getProviderURL(managedConnectionFactory);
				String authDataAlias = getAuthDataAlias(managedConnectionFactory);
				if (authDataAlias != null) {
					CredentialFactory cf = new CredentialFactory(authDataAlias,
							null, null);
					cfUserName = cf.getUsername();
					cfPassword = cf.getPassword();
				}
				if (!contextFactoryClassname
						.equals("com.tibco.tibjms.naming.TibjmsInitialContextFactory")) {
					log.warn("did not expect context factory of type ["
							+ contextFactoryClassname + "]");
				} else {
					return new EsbConnectionFactoryInfo(
							managedConnectionFactory, contextFactoryClassname,
							cfUrl, cfUserName, cfPassword);
				}
			}
		}
		return null;
	}

	private static String getContextFactoryClassname(
			Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getContextFactoryClassname", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	private static String getProviderURL(Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getProviderURL", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	private static String getAuthDataAlias(Object managedConnectionFactory) {
		try {
			return (String) ClassUtils.invokeGetter(managedConnectionFactory,
					"getAuthDataAlias", true);
		} catch (Exception e) {
			// log.debug("Caught Exception: "+e.getMessage());
			return null;
		}
	}

	private static PoolingConnectionFactory setupJmsConnectionFactory(
			String url, String userName, String password) {
		String serverUrl = StringUtils.replace(url, "tibjmsnaming:", "tcp:");
		log.debug("setting up JmsConnectionFactory url [" + serverUrl
				+ "] username [" + userName + "] password ["
				+ StringUtils.repeat("*", password.length()) + "]");
		PoolingConnectionFactory jmsConnectionFactory = new PoolingConnectionFactory();
		jmsConnectionFactory
				.setClassName("com.tibco.tibjms.TibjmsXAConnectionFactory");
		jmsConnectionFactory.setUniqueName("tibcojms");
		jmsConnectionFactory.setMaxPoolSize(5);
		jmsConnectionFactory.setAllowLocalTransactions(true);
		jmsConnectionFactory.setUser(userName);
		jmsConnectionFactory.setPassword(password);
		jmsConnectionFactory.getDriverProperties().setProperty("serverUrl",
				serverUrl);
		jmsConnectionFactory.init();
		return jmsConnectionFactory;
	}

	private static PoolingDataSource setupJdbcDataSource(String url,
			String userName, String password) {
		log.debug("setting up JdbcDataSource url [" + url + "] username ["
				+ userName + "] password ["
				+ StringUtils.repeat("*", password.length()) + "]");
		PoolingDataSource jdbcDataSource = new PoolingDataSource();
		jdbcDataSource.setClassName("oracle.jdbc.xa.client.OracleXADataSource");
		jdbcDataSource.setUniqueName("oracle");
		jdbcDataSource.setMaxPoolSize(5);
		jdbcDataSource.setAllowLocalTransactions(true);
		// jdbcDataSource.setTestQuery("SELECT 1 FROM DUAL");
		jdbcDataSource.getDriverProperties().setProperty("user", userName);
		jdbcDataSource.getDriverProperties().setProperty("password", password);
		jdbcDataSource.getDriverProperties().setProperty("URL", url);
		jdbcDataSource.init();
		return jdbcDataSource;
	}

	public static String getQueueMessageCount(EsbJmsListener esbJmsListener) {
		EsbConnectionFactoryInfo ecfi = getEsbConnectionFactoryInfo(esbJmsListener);
		if (ecfi != null) {
			return getQueueMessageCount(ecfi.getUrl(), null,
					ecfi.getUserName(), ecfi.getPassword(),
					esbJmsListener.getPhysicalDestinationShortName(),
					esbJmsListener.getMessageSelector());
		}
		return null;
	}
	
	public static String getQueueMessageCount(String provUrl, String authAlias,
			String userName, String password, String queueName,
			String messageSelector) {
		try {
			Class<?>[] args_types = new Class<?>[6];
			args_types[0] = String.class;
			args_types[1] = String.class;
			args_types[2] = String.class;
			args_types[3] = String.class;
			args_types[4] = String.class;
			args_types[5] = String.class;
			Object[] args = new Object[6];
			args[0] = provUrl;
			args[1] = authAlias;
			args[2] = userName;
			args[3] = password;
			args[4] = queueName;
			args[5] = messageSelector;
			long messageCount = (Long) Class
					.forName(
							"nl.nn.adapterframework.extensions.tibco.TibcoUtils")
					.getMethod("getQueueMessageCount", args_types)
					.invoke(null, args);
			return String.valueOf(messageCount);
		} catch (Exception e) {
			log.warn("error occured during getting queue message count: "
					+ e.getMessage());
		}
		return null;
	}
}
