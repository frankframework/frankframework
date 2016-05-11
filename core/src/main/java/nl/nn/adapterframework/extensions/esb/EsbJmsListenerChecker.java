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
package nl.nn.adapterframework.extensions.esb;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Checker for lost connections in EsbJmsListeners.
 * 
 * @author Peter Leeuwenburgh
 */
public class EsbJmsListenerChecker {
	protected static Logger log = LogUtil
			.getLogger(EsbJmsListenerChecker.class);

	public static void doCheck(IbisManager ibisManager,
			PlatformTransactionManager txManager, String logPrefix) {
		long idleTimeout = AppConstants.getInstance().getInt(
				"check.esbJmsListeners.idleTimeout", 300) * 1000;
		for (Configuration configuration : ibisManager.getConfigurations()) {
			String msg;
			List<String> jmsRealmNames = new ArrayList<String>();
			for (IAdapter adapter : configuration.getRegisteredAdapters()) {
				if (adapter instanceof Adapter) {
					for (Iterator receiverIt = adapter.getReceiverIterator(); receiverIt
							.hasNext();) {
						IReceiver receiver = (IReceiver) receiverIt.next();
						if (receiver instanceof ReceiverBase) {
							ReceiverBase rb = (ReceiverBase) receiver;
							if (rb.getRunState().equals(RunStateEnum.STARTED)) {
								// if (true) {
								long lastMessageDate = rb.getLastMessageDate();
								if (lastMessageDate == 0
										|| System.currentTimeMillis()
												- lastMessageDate > idleTimeout) {
									IListener listener = rb.getListener();
									if (listener instanceof EsbJmsListener) {
										EsbJmsListener esbJmsListener = (EsbJmsListener) listener;
										if (esbJmsListener.getMessageProtocol()
												.equals("FF")) {
											Object managedConnectionFactory = null;
											try {
												managedConnectionFactory = esbJmsListener
														.getManagedConnectionFactory();
												if (managedConnectionFactory == null) {
													msg = logPrefix
															+ "could not get managed connection factory";
													warn(adapter, msg);
												} else {
													String contextFactoryClassname = getContextFactoryClassname(managedConnectionFactory);
													String providerURL = getProviderURL(managedConnectionFactory);
													String authDataAlias = getAuthDataAlias(managedConnectionFactory);
													String username = null;
													String password = null;
													msg = logPrefix
															+ "found esbJmsListener ["
															+ esbJmsListener
																	.getName()
															+ "] with managedConnectionFactoryClassname ["
															+ managedConnectionFactory
																	.getClass()
																	.getName()
															+ "] having contextFactoryClassname ["
															+ contextFactoryClassname
															+ "] providerURL ["
															+ providerURL
															+ "] authDataAlias ["
															+ authDataAlias + "]";
													if (authDataAlias != null) {
														CredentialFactory cf = new CredentialFactory(
																authDataAlias,
																null, null);
														username = cf.getUsername();
														password = cf.getPassword();
													}
													if (contextFactoryClassname != null
															&& contextFactoryClassname
																	.equals("com.tibco.tibjms.naming.TibjmsInitialContextFactory")) {
														log.debug(msg
																+ ", checking...");
														long age = getTibcoQueueFirstMessageAge(
																providerURL,
																authDataAlias,
																username,
																password,
																esbJmsListener
																		.getPhysicalDestinationShortName(),
																esbJmsListener
																		.getMessageSelector());
														if (age > idleTimeout) {
															msg = logPrefix
																	+ "most probably esbJmsListener ["
																	+ esbJmsListener
																			.getName()
																	+ "] has lost connection with queue ["
																	+ esbJmsListener
																			.getPhysicalDestinationShortName()
																	+ "]";
															warn(adapter, msg);
														}
													} else {
														log.debug(msg
																+ ", ignoring...");
													}
												}
											} catch (Throwable t) {
												msg = logPrefix
														+ "exception on checking queue ["
														+ esbJmsListener
																.getPhysicalDestinationShortName()
														+ "]";
												warn(adapter, msg, t);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
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

	private static long getTibcoQueueFirstMessageAge(String provUrl,
			String authAlias, String userName, String password,
			String queueName, String messageSelector)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
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
		return (Long) Class
				.forName("nl.nn.adapterframework.extensions.tibco.TibcoUtils")
				.getMethod("getQueueFirstMessageAge", args_types)
				.invoke(null, args);
	}

	private static void warn(IAdapter adapter, String msg) {
		warn(adapter, msg, null);
	}

	private static void warn(IAdapter adapter, String msg, Throwable t) {
		log.warn(msg, t);
		if (adapter != null) {
			adapter.getMessageKeeper().add(
					"WARNING: " + msg
							+ (t != null ? ": " + t.getMessage() : ""),
					MessageKeeperMessage.WARN_LEVEL);

		}
	}

	private static void error(IAdapter adapter, String msg) {
		error(adapter, msg, null);
	}

	private static void error(IAdapter adapter, String msg, Throwable t) {
		log.error(msg, t);
		if (adapter != null) {
			if (t != null) {
				if (!(t instanceof IbisException)) {
					msg += " (" + t.getClass().getName() + ")";
				}
			}
			adapter.getMessageKeeper().add(
					"ERROR: " + msg + (t != null ? ": " + t.getMessage() : ""),
					MessageKeeperMessage.ERROR_LEVEL);
		}
	}
}
