package nl.nn.adapterframework.testtool;

import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Murat Kaan Meral
 *
 * Listener for all the activity in Larva Test Tool.
 * It contains each activity as a message object.
 *
 */
public class MessageListener {

	//public synchronized static final String LOG_LEVEL_ORDER = "[debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]";
	private static Logger logger = LogUtil.getLogger(TestTool.class);
	private static LinkedList<Message> archive;
	private static LinkedList<Message> messages  = new LinkedList<Message>();
	private static int selectedLogLevel = 0;
	private static final List<String> LOG_LEVEL = new ArrayList<String>() {{
		// CAUTION! Order matters!!
		add("Debug");
		add("Pipeline Messages Prepared For Diff");
		add("Pipeline Messages");
		add("Wrong Pipeline Messages");
		add("Step Passed/Failed");
		add("Test Properties");
		add("Scenario Passed/Failed");
		add("Scenario Failed");
		add("Total");
		add("Errors");
	}};

	/**
	 * @return Returns all messages above selected log level.
	 */
	public synchronized static JSONArray getMessages() {
		return getMessages(0);
	}


	public synchronized static JSONArray getMessages(long timestamp) {
		return getMessagesFromList(MessageListener.messages, timestamp);
	}

	/**
	 * Returns messages after the given timestamp and above the selected log level.
	 * @param messages the linked list of messages to be filtered. Note: the list should be sorted timestamps.
	 * @param timestamp for filtering messages based on time.
	 * @return list of messages that fit the criteria.
	 */
	private synchronized static JSONArray getMessagesFromList(LinkedList<Message> messages, long timestamp){
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

	public synchronized static void debugMessage(String testName, String message) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("DEBUG"), System.currentTimeMillis());
		messages.add(m);
		System.out.println(message);
		logger.error(message);
	}

	private synchronized static void singleMessage(String testName, String message, String debugLevel) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf(debugLevel), System.currentTimeMillis());
		messages.add(m);
	}

	static void testInitiationMessage(String testName, String directory) {
		Map<String, String> map = new HashMap<>();
		map.put("directory", directory);
		messages.add(new Message(testName, map, LOG_LEVEL.indexOf("Test Properties"), System.currentTimeMillis()));
	}

	static void testStatusMessage(String testName, String status) {
		Map<String, String> map = new HashMap<>();
		map.put("status", status);
		messages.add(new Message(testName, map, LOG_LEVEL.indexOf("Test Properties"), System.currentTimeMillis()));
	}

	public synchronized static void debugPipelineMessage(String testName, String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	static void debugPipelineMessagePreparedForDiff(String testName, String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	public synchronized static void wrongPipelineMessage(String testName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(2);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	static void wrongPipelineMessage(String testName, String stepDisplayName, String message, String pipelineMessage, String pipelineMessageExpected) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		map.put("pipelineMessageExpected", pipelineMessage);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Wrong Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	static void wrongPipelineMessagePreparedForDiff(String testName, String stepDisplayName, String pipelineMessagePreparedForDiff, String pipelineMessageExpectedPreparedForDiff) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Pipeline Message", pipelineMessagePreparedForDiff);
		map.put("Pipeline Message Expected", pipelineMessageExpectedPreparedForDiff);
		Message m = new Message(testName, map, LOG_LEVEL.indexOf("Wrong Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	private synchronized static String writeCommands(String testName, String target, boolean textArea, String customCommand) {
		String commands = "";

		commands += "<div class='commands'>";
		commands += "<span class='widthCommands'><a href='javascript:void(0);' class='" + target + "|widthDown'>-</a><a href='javascript:void(0);' class='" + target + "|widthExact'>width</a><a href='javascript:void(0);' class='" + target + "|widthUp'>+</a></span>";
		commands += "<span class='heightCommands'><a href='javascript:void(0);' class='" + target + "|heightDown'>-</a><a href='javascript:void(0);' class='" + target + "|heightExact'>height</a><a href='javascript:void(0);' class='" + target + "|heightUp'>+</a></span>";
		if (textArea) {
			commands += "<a href='javascript:void(0);' class='" + target + "|copy'>copy</a> ";
			commands += "<a href='javascript:void(0);' class='" + target + "|xmlFormat'>indent</a>";
		}
		if (customCommand != null) {
			commands += " " + customCommand;
		}
		commands += "</div>";


		return commands;
	}

	static void stepMessage(String testName, String message) {
		singleMessage(testName, message , "Step Passed/Failed");
	}

	static void scenarioMessage(String testName, String message) {
		singleMessage(testName, message, "Scenario Passed/Failed");
	}

	static void scenarioFailedMessage(String testName, String message) {
		singleMessage(testName, message, "Scenario Failed");
	}

	static void scenariosTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	static void scenariosPassedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	static void scenariosAutosavedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	static void scenariosFailedTotalMessage(String message) {
		singleMessage("General", message, "Total");
	}

	public synchronized static void errorMessage(String testName, String message) {
		singleMessage(testName, message, "Errors");
	}

	public synchronized static void errorMessage(String testName, String message, Exception exception) {
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

	public synchronized static String getSelectedLogLevel() {
		return LOG_LEVEL.get(selectedLogLevel);
	}

	public synchronized static void setSelectedLogLevel(String selectedLogLevel) throws Exception {
		if (!LOG_LEVEL.contains(selectedLogLevel))
			throw new Exception("Given log level does not exist!");
		MessageListener.selectedLogLevel = LOG_LEVEL.indexOf(selectedLogLevel);
	}

	public synchronized static List<String> getLogLevels() {
		return LOG_LEVEL;
	}

	public synchronized static int getLevelOfLog(String logLevel) {
		return LOG_LEVEL.indexOf(logLevel);
	}

	public synchronized static void cleanLogs(boolean archive) {
		if(archive) {
			MessageListener.archive = MessageListener.messages;
		}
		MessageListener.messages = new LinkedList<>();
	}

	public synchronized static JSONArray getArchive() {
		return getMessagesFromList(MessageListener.archive, 0);
	}
}
