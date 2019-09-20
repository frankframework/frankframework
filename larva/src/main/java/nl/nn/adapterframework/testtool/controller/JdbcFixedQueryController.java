package nl.nn.adapterframework.testtool.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.ResultComparer;
import nl.nn.adapterframework.testtool.ScenarioTester;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;

/**
 * This class is used to initialize and execute Fixed Query Senders.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class JdbcFixedQueryController {
	
	@Autowired
	static IbisContext ibisContext;
	
	/**
	 * Initializes Fixed Query Senders and adds them to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param jdbcFixedQuerySenders senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param appConstants constants required for database access.
	 */
	public static void initSender(Map<String, Map<String, Object>> queues, List<String> jdbcFixedQuerySenders, Properties properties, AppConstants appConstants) {
		MessageListener.debugMessage("Initialize jdbc fixed query senders");
		Iterator<String> iterator = jdbcFixedQuerySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String datasourceName = (String)properties.get(name + ".datasourceName");
			String username = (String)properties.get(name + ".username");
			String password = (String)properties.get(name + ".password");
			boolean allFound = false;
			String preDelete = ""; 
			int preDeleteIndex = 1;
			String queryType = (String)properties.get(name + ".queryType");
			String getBlobSmartString = (String)properties.get(name + ".getBlobSmart");
			boolean getBlobSmart = false;
			if (getBlobSmartString != null) {
				getBlobSmart = Boolean.valueOf(getBlobSmartString).booleanValue();
			}
			if (datasourceName == null) {
				ScenarioTester.closeQueues(queues, properties);
				queues = null;
				MessageListener.errorMessage("Could not find datasourceName property for " + name);
			} else {
				Map<String, Object> querySendersInfo = new HashMap<String, Object>();
				while (!allFound && queues != null) {
					preDelete = (String)properties.get(name + ".preDel" + preDeleteIndex);
					if (preDelete != null) {
						FixedQuerySender deleteQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						deleteQuerySender.setName("Test Tool pre delete query sender");
						deleteQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						deleteQuerySender.setQueryType("delete");
						deleteQuerySender.setQuery("delete from " + preDelete);
						try {
							deleteQuerySender.configure();				 		
							deleteQuerySender.open(); 						
							deleteQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
							deleteQuerySender.close();
						} catch(ConfigurationException e) {
							ScenarioTester.closeQueues(queues, properties);
							queues = null;
							MessageListener.errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
						} catch(TimeOutException e) {
							ScenarioTester.closeQueues(queues, properties);
							queues = null;
							MessageListener.errorMessage("Time out on execute pre delete query for '" + name + "': " + e.getMessage(), e);
						} catch(SenderException e) {
							ScenarioTester.closeQueues(queues, properties);
							queues = null;
							MessageListener.errorMessage("Could not execute pre delete query for '" + name + "': " + e.getMessage(), e);
						}
						preDeleteIndex++;
					} else {
						allFound = true;
					}	
				}
				if (queues != null) {
					String prePostQuery = (String)properties.get(name + ".prePostQuery");
					if (prePostQuery != null) {
						FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						prePostFixedQuerySender.setName("Test Tool query sender");
						prePostFixedQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						//prePostFixedQuerySender.setUsername(username);
						//prePostFixedQuerySender.setPassword(password);
						prePostFixedQuerySender.setQueryType("select");
						prePostFixedQuerySender.setQuery(prePostQuery);
						try {
							prePostFixedQuerySender.configure();
						} catch(ConfigurationException e) {
							ScenarioTester.closeQueues(queues, properties);
							queues = null;
							MessageListener.errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
						}
						if (queues != null) {
							try {
								prePostFixedQuerySender.open();
							} catch(SenderException e) {
								ScenarioTester.closeQueues(queues, properties);
								queues = null;
								MessageListener.errorMessage("Could not open (pre/post) '" + name + "': " + e.getMessage(), e);
							}
						}
						if (queues != null) {
							try {
								String result = prePostFixedQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
								querySendersInfo.put("prePostQueryFixedQuerySender", prePostFixedQuerySender);
								querySendersInfo.put("prePostQueryResult", result);
							} catch(TimeOutException e) {
								ScenarioTester.closeQueues(queues, properties);
								queues = null;
								MessageListener.errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e);
							} catch(SenderException e) {
								ScenarioTester.closeQueues(queues, properties);
								queues = null;
								MessageListener.errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e);
							}
						}
					}
				}
				if (queues != null) {
					String readQuery = (String)properties.get(name + ".readQuery");
					if (readQuery != null) {
						FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
						readQueryFixedQuerySender.setName("Test Tool query sender");
						readQueryFixedQuerySender.setDatasourceName(appConstants.getResolvedProperty("jndiContextPrefix") + datasourceName);
						//readQueryFixedQuerySender.setUsername(username);
						//readQueryFixedQuerySender.setPassword(password);
						
						if ((queryType != null) && (! queryType.equals(""))) {
							readQueryFixedQuerySender.setQueryType(queryType);	
						} else {
							readQueryFixedQuerySender.setQueryType("select");	
						}
						
						readQueryFixedQuerySender.setQuery(readQuery);
						readQueryFixedQuerySender.setBlobSmartGet(getBlobSmart);
						try {
							readQueryFixedQuerySender.configure();
						} catch(ConfigurationException e) {
							ScenarioTester.closeQueues(queues, properties);
							queues = null;
							MessageListener.errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
						}
						if (queues != null) {
							try {
								readQueryFixedQuerySender.open();
								querySendersInfo.put("readQueryQueryFixedQuerySender", readQueryFixedQuerySender);
							} catch(SenderException e) {
								ScenarioTester.closeQueues(queues, properties);
								queues = null;
								MessageListener.errorMessage("Could not open '" + name + "': " + e.getMessage(), e);
							}
						}
					}
				}
				if (queues != null) {
					String waitBeforeRead = (String)properties.get(name + ".waitBeforeRead");
					if (waitBeforeRead != null) {
						try {
							querySendersInfo.put("readQueryWaitBeforeRead", new Integer(waitBeforeRead));
						} catch(NumberFormatException e) {
							MessageListener.errorMessage("Value of '" + name + ".waitBeforeRead' not a number: " + e.getMessage(), e);
						}
					}
					queues.put(name, querySendersInfo);
					MessageListener.debugMessage("Opened jdbc connection '" + name + "'");
				}
			}
		}

	}

	/**
	 * Closes the Fixed Query Senders on the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return true if there are still messages remaining for this pipe.
	 */
	public static boolean closeConnection(Map<String, Map<String, Object>> queues, Properties properties) {
		boolean remainingMessagesFound = false;
		MessageListener.debugMessage("Close jdbc connections");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String name = (String)iterator.next();
			if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(name + ".className"))) {
				Map<?, ?> querySendersInfo = (Map<?, ?>)queues.get(name);
				FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
				if (prePostFixedQuerySender != null) {
					try {
						/* Check if the preResult and postResult are not equal. If so, then there is a
						 * database change that has not been read in the scenario.
						 * So set remainingMessagesFound to true and show the entry.
						 * (see also executeFixedQuerySenderRead() )
						 */
						String preResult = (String)querySendersInfo.get("prePostQueryResult");
						String postResult = prePostFixedQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
						if (!preResult.equals(postResult)) {
							
							String message = null;
							FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
							try {
								message = readQueryFixedQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
							} catch(TimeOutException e) {
								MessageListener.errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e);
							} catch(SenderException e) {
								MessageListener.errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e);
							}
							if (message != null) {
								MessageListener.wrongPipelineMessage("Found remaining message on '" + name + "'", message);
							}

							remainingMessagesFound = true;
							
						}
						prePostFixedQuerySender.close();
					} catch(TimeOutException e) {
						MessageListener.errorMessage("Time out on close (pre/post) '" + name + "': " + e.getMessage(), e);
					} catch(SenderException e) {
						MessageListener.errorMessage("Could not close (pre/post) '" + name + "': " + e.getMessage(), e);
					}
				}
				FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
				readQueryFixedQuerySender.close();
			}
		}
		return remainingMessagesFound;
	}
	
	/**
	 * Reads message from the pipe and then compares it to the content of the expected result file.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @return 0 if no problems, 1 if error has occurred, 2 if it has been autosaved.
	 */
	public static int read(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		
		Map querySendersInfo = (Map)queues.get(queueName);
		Integer waitBeforeRead = (Integer)querySendersInfo.get("readQueryWaitBeforeRead");

		if (waitBeforeRead != null) {
			try {
				Thread.sleep(waitBeforeRead.intValue());
			} catch(InterruptedException e) {
			}
		}
		boolean newRecordFound = true;
		FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)querySendersInfo.get("prePostQueryFixedQuerySender");
		if (prePostFixedQuerySender != null) {
			try {
				String preResult = (String)querySendersInfo.get("prePostQueryResult");
				MessageListener.debugPipelineMessage(stepDisplayName, "Pre result '" + queueName + "':", preResult);
				String postResult = prePostFixedQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
				MessageListener.debugPipelineMessage(stepDisplayName, "Post result '" + queueName + "':", postResult);
				if (preResult.equals(postResult)) {
					newRecordFound = false;
				}
				/* Fill the preResult with postResult, so TestTool.closeQueues is able to determine if there
				 * are remaining messages left.
				 */
				querySendersInfo.put("prePostQueryResult", postResult);
			} catch(TimeOutException e) {
				MessageListener.errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e);
			} catch(SenderException e) {
				MessageListener.errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e);
			}
		}
		String message = null;
		if (newRecordFound) {
			FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)querySendersInfo.get("readQueryQueryFixedQuerySender");
			try {
				message = readQueryFixedQuerySender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, TestTool.TESTTOOL_DUMMY_MESSAGE);
			} catch(TimeOutException e) {
				MessageListener.errorMessage("Time out on execute query for '" + queueName + "': " + e.getMessage(), e);
			} catch(SenderException e) {
				MessageListener.errorMessage("Could not execute query for '" + queueName + "': " + e.getMessage(), e);
			}
		}
		if (message == null) {
			if ("".equals(fileName)) {
				result = TestTool.RESULT_OK;
			} else {
				MessageListener.errorMessage("Could not read jdbc message (null returned) or no new message found (pre result equals post result)");
			}
		} else {
			if ("".equals(fileName)) {
				MessageListener.debugPipelineMessage(stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
			} else {
				result = ResultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName);
			}
		}

		return result;
	}

}
