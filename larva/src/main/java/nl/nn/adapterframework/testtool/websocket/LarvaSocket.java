package nl.nn.adapterframework.webcontrol.websocket;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.nn.adapterframework.testtool.*;
import nl.nn.adapterframework.util.LogUtil;
/**
 * @author Murat Kaan Meral
 *
 */
@ServerEndpoint(value="/larva/websocket/")
public class LarvaSocket {
	private final int INTERVAL = 100;
	private static Logger logger = LogUtil.getLogger(LarvaSocket.class);
	private Thread testRunner;

	/**
	 * When new socket opens, it sends list for root directories and available scenarios.
	 * @param session
	 * @throws IOException
	 */
	@OnOpen
	public void onOpen(Session session) throws IOException {
		// Send available params "name":type
		// Send list of possible tests
		logger.debug("Opened new websocket with id: " + session.getId());
		JSONObject scenarioList = getScenarioList(null);
		JSONObject rootDirectories = new JSONObject(TestPreparer.scenariosRootDirectories);
		JSONArray logLevels = new JSONArray(MessageListener.getLogLevels());
		JSONObject toReturn = new JSONObject();
		try {
			toReturn.append("scenarios", scenarioList);
			toReturn.append("rootDirectories", rootDirectories);
			toReturn.append("logLevels", logLevels);
		} catch (JSONException e) {
			e.printStackTrace();
			returnError(session, e.getMessage());
			return;
		}

		session.getBasicRemote().sendText(toReturn.toString());
	}

	@OnMessage
	public void onMessage(Session session, String strMessage) throws IOException {
		JSONObject message;

		try {
			message = new JSONObject(strMessage);
		} catch (JSONException e) {
			e.printStackTrace();
			returnError(session, e.getMessage());
			return;
		}
		String type;

		try {
			type = message.getString("type");
		} catch (JSONException e) {
			e.printStackTrace();
			returnError(session, e.getMessage());
			return;
		}

		// If execute request is send
		if(type.equalsIgnoreCase("execute")) {
			String paramExecute, currentScenariosRootDirectory;
			int paramWaitBeforeCleanUp, numberOfThreads;

			// Check if required params exist
			String[] params = {"paramExecute", "scenariosRootDirectory", "waitBeforeCleanUp", "numberOfThreads"};
			for(String param : params) {
				if(! message.has(param)) {
					returnError(session, "Message should contain the parameter " + param);
					return;
				}
			}

			try {
				paramExecute = message.getString("paramExecute");
				currentScenariosRootDirectory = message.getString("scenariosRootDirectory");
				paramWaitBeforeCleanUp = message.getInt("waitBeforeCleanUp");
				numberOfThreads = message.getInt("numberOfThreads");
				testRunner = new Thread() {
					String paramExecute, currentScenariosRootDirectory;
					int paramWaitBeforeCleanUp, numberOfThreads;

					@Override
					public void run() {
						TestTool.runScenarios(paramExecute, paramWaitBeforeCleanUp, currentScenariosRootDirectory, numberOfThreads);
					}

					public Thread initTestParams(String paramExecute, int paramWaitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads) {
						this.paramExecute = paramExecute;
						this.paramWaitBeforeCleanUp = paramWaitBeforeCleanUp;
						this.currentScenariosRootDirectory = currentScenariosRootDirectory;
						this.numberOfThreads = numberOfThreads;

						return this;
					}
				}.initTestParams(paramExecute, paramWaitBeforeCleanUp, currentScenariosRootDirectory, numberOfThreads);;

				// Start Tests
				testRunner.start();

				// As tests are executed send the messages to frontend.
				long timestamp = 0;
				JSONArray messages;
				JSONObject toReturn;
				while(testRunner.isAlive()) {
					List<Message> messageList = MessageListener.getMessages(timestamp);
					messages = new JSONArray(messageList);
					timestamp = System.currentTimeMillis();

					toReturn = new JSONObject().append("messages", messages);
					session.getAsyncRemote().sendText(toReturn.toString());

					try {
						Thread.sleep(INTERVAL);
					}catch (Exception e) {
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

		// If Log Level Is Changed
		}else if(type.equalsIgnoreCase("setLogLevel")) {
			if(!message.has("logLevel")) {
				returnError(session, "No log level specified!");
				return;
			}
			try {
				String logLevel = message.getString("logLevel");
				MessageListener.setSelectedLogLevel(logLevel);
			}catch (JSONException e) {
				e.printStackTrace();
				returnError(session, "Could not change log level due to: " + e.getMessage());
				return;
			}
		}


		// Receive params
		// First send test groups (to create a table)

		// While tests are continuing
		// Send test data
	}

	@OnClose
	public void onClose(Session session) throws IOException {
		// Make sure tests are done running
		// then clean the variables

		if (testRunner.isAlive()) {
			testRunner.interrupt();
		}
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		logger.error("Error on session (" + session.getId() + ") \n" + throwable.getMessage());
	}

	private void returnError(Session session, String errorMessage) throws IOException {
		session.getBasicRemote().sendText("{\"error\": \"" + errorMessage + "\"}");
	}

	private JSONObject getScenarioList(String rootDirectory) {
		rootDirectory = TestPreparer.initScenariosRootDirectories(rootDirectory);
		Map<String, List<File>> scenarios = TestPreparer.readScenarioFiles(rootDirectory, false);
		Map<String, String> scenarioList = TestPreparer.getScenariosList(rootDirectory, scenarios, 100);
		return new JSONObject(scenarioList);
	}

}