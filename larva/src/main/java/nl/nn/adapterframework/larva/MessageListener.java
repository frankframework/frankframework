package nl.nn.adapterframework.larva;

import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Listener for all the activity in Larva Test Tool.
 * It contains each activity as a message object.
 *
 * @author Murat Kaan Meral
 *
 */
public class MessageListener {

	//public synchronized static final String LOG_LEVEL_ORDER = "[debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]";
	private static Logger logger = LogUtil.getLogger(TestTool.class);
	private LinkedList<Message> messages;
	private int selectedLogLevel;
	private static final List<String> LOG_LEVEL = new ArrayList<String>() {{
		// CAUTION! Order matters!!
		add("Debug");
		add("Pipeline Messages Prepared For Diff");
		add("Pipeline Messages");
		add("Wrong Pipeline Messages with Diff");
		add("Wrong Pipeline Messages");
		add("Step Passed/Failed");
		add("Test Properties");
		add("Scenario Passed/Failed");
		add("Scenario Failed");
		add("Total");
		add("Errors");
	}};
	public static int DEFAULT_LOG_LEVEL = 3;

	public MessageListener(LinkedList<Message> messages, String selectedLogLevel) {
		this.messages = messages;
		this.selectedLogLevel = LOG_LEVEL.indexOf(selectedLogLevel);
	}

	public MessageListener(){
		this.messages = new LinkedList<>();
		this.selectedLogLevel = MessageListener.DEFAULT_LOG_LEVEL;
	}

	/**
	 * @return Returns all messages above selected log level.
	 */
	public synchronized JSONArray getMessages() {
		return getMessages(0);
	}

	/**
	 * Returns all of the messages after given timestamp, and above selected log level.
	 * @param timestamp timestamp for filtering messages.
	 * @return JSONArray containing all the messages that fit the criteria.
	 */
	public synchronized JSONArray getMessages(long timestamp) {
		return getMessagesFromList(messages, timestamp);
	}

	/**
	 * Returns messages after the given timestamp and above the selected log level.
	 * @param messages the linked list of messages to be filtered. Note: the list should be sorted timestamps.
	 * @param timestamp for filtering messages based on time.
	 * @return list of messages that fit the criteria.
	 */
	private synchronized JSONArray getMessagesFromList(LinkedList<Message> messages, long timestamp){
		if(messages == null)
			return new JSONArray();

		List<JSONObject> result = new ArrayList<>();
		Iterator<Message> messageIterator = messages.descendingIterator();
		while(messageIterator.hasNext()) {
			Message m = messageIterator.next();
			if(m.timestamp >= timestamp && m.logLevel >= selectedLogLevel) {
				try {
					JSONObject message = new JSONObject()
							.put("name", m.testName)
							.put("timestamp", m.timestamp)
							.put("logLevel", LOG_LEVEL.get(m.logLevel))
							.put("messages", m.message);
					result.add(message);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			// Since it is linked list, it will be ordered by their insertion time (in reverse order)
			// So the moment message timestamp is before what we want, we can say no other message will be later than timestamp
			if(m.timestamp < timestamp)
				break;
		}

		return new JSONArray(result);
	}

	/**
	 * Returns the last total message. After the execution finished, it will return the last output message
	 * that states the successful/autosaved/failed messages.
	 * @return The last total message. Null, if there were no total messages.
	 */
	public synchronized String getLastTotalMessage() {
		Iterator<Message> messageIterator = messages.descendingIterator();
		while(messageIterator.hasNext()) {
			Message m = messageIterator.next();
			if (m.logLevel == getLevelOfLog("Total")) {
				return m.message.get("Message");
			}
		}
		return null;
	}

	public synchronized void debugMessage(String testName, String message) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("DEBUG"), System.currentTimeMillis());
		messages.add(m);
		System.out.println(message);
		logger.debug(message);
	}

	private synchronized void singleMessage(String testName, String message, String debugLevel) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf(debugLevel), System.currentTimeMillis());
		messages.add(m);
	}

	void testInitiationMessage(String testName, String directory) {
		Map<String, String> map = new HashMap<>();
		map.put("directory", directory);
		messages.add(new Message(testName, map, LOG_LEVEL.indexOf("Test Properties"), System.currentTimeMillis()));
	}

	void testStatusMessage(String testName, String status) {
		Map<String, String> map = new HashMap<>();
		map.put("status", status);
		messages.add(new Message(testName, map, LOG_LEVEL.indexOf("Test Properties"), System.currentTimeMillis()));
	}

	public synchronized void debugPipelineMessage(String testName, String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	void debugPipelineMessagePreparedForDiff(String testName, String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	public synchronized void wrongPipelineMessage(String testName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(2);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	void wrongPipelineMessage(String testName, String stepDisplayName, String message, String pipelineMessage, String pipelineMessageExpected, String originalFilePath) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		map.put("pipelineMessageExpected", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Wrong Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	void wrongPipelineMessagePreparedForDiff(String testName, String stepDisplayName, String pipelineMessagePreparedForDiff, String pipelineMessageExpectedPreparedForDiff, String originalFilePath) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Pipeline Message", pipelineMessagePreparedForDiff);
		map.put("Pipeline Message Expected", pipelineMessageExpectedPreparedForDiff);
		map.put("Filepath", originalFilePath);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Wrong Pipeline Messages with Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	void stepMessage(String testName, String message) {
		singleMessage(testName, message , "Step Passed/Failed");
	}

	void scenarioMessage(String testName, String message) {
		singleMessage(testName, message, "Scenario Passed/Failed");
	}

	void scenarioFailedMessage(String testName, String message) {
		singleMessage(testName, message, "Scenario Failed");
	}

	void scenariosTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	void scenariosPassedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	void scenariosAutosavedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	void scenariosFailedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	public synchronized void errorMessage(String testName, String message) {
		singleMessage(testName, message, "Errors");
	}

	public synchronized void errorMessage(String testName, String message, Exception exception) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("Message", message);

		StringBuilder stacktrace = new StringBuilder();
		Throwable throwable = exception;
		while (throwable != null) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			printWriter.close();
			stacktrace.append(stringWriter.toString());
			throwable = throwable.getCause();
		}

		map.put("Stack Trace", stacktrace.toString());
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Errors"), System.currentTimeMillis());
		messages.add(m);
	}

	public synchronized String getSelectedLogLevel() {
		return LOG_LEVEL.get(selectedLogLevel);
	}

	public synchronized void setSelectedLogLevel(String selectedLogLevel) throws Exception {
		if (!LOG_LEVEL.contains(selectedLogLevel))
			throw new Exception("Given log level does not exist!");
		this.selectedLogLevel = LOG_LEVEL.indexOf(selectedLogLevel);
	}

	public synchronized List<String> getLogLevels() {
		return LOG_LEVEL;
	}

	private synchronized int getLevelOfLog(String logLevel) {
		return LOG_LEVEL.indexOf(logLevel);
	}

	public synchronized void clean() {
		messages = new LinkedList<Message>();
	}
}
