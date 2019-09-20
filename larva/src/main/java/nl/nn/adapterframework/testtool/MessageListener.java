/**
 * 
 */
package nl.nn.adapterframework.testtool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import edu.emory.mathcs.backport.java.util.Arrays;
import net.sf.saxon.expr.flwor.SingularityPull;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author Murat Kaan Meral
 *
 * Listener for all the activity in Larva Test Tool.
 * It contains each activity as a message object.
 *
 */
public class MessageListener {
	
	//public static final String LOG_LEVEL_ORDER = "[debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]";
	private static Logger logger = LogUtil.getLogger(TestTool.class);
	private static LinkedList<Message> messages  = new LinkedList<Message>();
	private static int selectedLogLevel = 0;
	private static final List<String> LOG_LEVEL = new ArrayList<String>() {{
		// CAUTION! Order matters!!
		add("Debug");
		add("Pipeline Messages Prepared For Diff");
		add("Pipeline Messages");
		add("Wrong Pipeline Messages");
		add("Step Passed/Failed");
		add("Scenario Passed/Failed");
		add("Scenario Failed");
		add("Total");
		add("Errors");
	}};
	
	/**
	 * @return Returns all messages above selected log level.
	 */
	public static List<Message> getMessages() {
		return getMessages(0);
	}
	
	/**
	 * Returns messages after the given timestamp and above the selected log level.
	 * @param timestamp for filtering messages based on time.
	 * @return list of messages that fit the criteria.
	 */
	public static List<Message> getMessages(long timestamp){
		List<Message> result = new ArrayList<>();
		
		for(Message m : messages) {
			if(m.timestamp >= timestamp && m.logLevel >= selectedLogLevel)
				result.add(m);
			
			// Since it is linked list, it will be ordered by their insertion time (in reverse order)
			// So the moment message timestamp is before what we want, we can say no other message will be later than timestamp
			if(m.timestamp < timestamp)
				break;
		}
		
		return result;
	}
	
	public static void debugMessage(String message) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(map, LOG_LEVEL.indexOf("DEBUG"), System.currentTimeMillis());
		messages.add(m);
		System.out.println(message);
		logger.error(message);
	}

	public static void singleMessage(String message, String debugLevel) {
		Map<String, String> map = new HashMap<>(1);
		map.put("Message", message);
		Message m = new Message(map, LOG_LEVEL.indexOf(debugLevel), System.currentTimeMillis());
		messages.add(m);		
	}
	
	public static void debugPipelineMessage(String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(map, LOG_LEVEL.indexOf("Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}

	public static void debugPipelineMessagePreparedForDiff(String stepDisplayName, String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	public static void wrongPipelineMessage(String message, String pipelineMessage) {
		Map<String, String> map = new HashMap<>(2);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		Message m = new Message(map, LOG_LEVEL.indexOf("Pipeline Messages Prepared For Diff"), System.currentTimeMillis());
		messages.add(m);
	}

	public static void wrongPipelineMessage(String stepDisplayName, String message, String pipelineMessage, String pipelineMessageExpected) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Message", message);
		map.put("Pipeline Message", pipelineMessage);
		map.put("pipelineMessageExpected", pipelineMessage);
		Message m = new Message(map, LOG_LEVEL.indexOf("Wrong Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}
	
	public static void wrongPipelineMessagePreparedForDiff(String stepDisplayName, String pipelineMessagePreparedForDiff, String pipelineMessageExpectedPreparedForDiff) {
		Map<String, String> map = new HashMap<>(3);
		map.put("Step Display Name", stepDisplayName);
		map.put("Pipeline Message", pipelineMessagePreparedForDiff);
		map.put("Pipeline Message Expected", pipelineMessageExpectedPreparedForDiff);
		Message m = new Message(map, LOG_LEVEL.indexOf("Wrong Pipeline Messages"), System.currentTimeMillis());
		messages.add(m);
	}
		
	private static String writeCommands(String target, boolean textArea, String customCommand) {
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

	public static void stepMessage(String message) {
		singleMessage(message, "Step Passed/Failed");
	}

	public static void scenarioMessage(String message) {
		singleMessage(message, "Scenario Passed/Failed");
	}

	public static void scenarioFailedMessage(String message) {
		singleMessage(message, "Scenario Failed");
	}

	public static void scenariosTotalMessage(String message) {
		singleMessage(message, "Total");
	}

	public static void scenariosPassedTotalMessage(String message) {
		singleMessage(message, "Total");
	}

	public static void scenariosAutosavedTotalMessage(String message) {
		singleMessage(message, "Total");
	}

	public static void scenariosFailedTotalMessage(String message) {
		singleMessage(message, "Total");
	}

	public static void errorMessage(String message) {
		singleMessage(message, "Errors");
	}

	public static void errorMessage(String message, Exception exception) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("Message", message);

		String stacktrace = "";
		Throwable throwable = exception;
		while (throwable != null) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			printWriter.close();
			stacktrace += stringWriter.toString();
			throwable = throwable.getCause();
		}
		
		map.put("Stack Trace", stacktrace);
		Message m = new Message(map, LOG_LEVEL.indexOf("Errors"), System.currentTimeMillis());
		messages.add(m);
	}
	
	public static int getSelectedLogLevel() {
		return selectedLogLevel;
	}

	public static void setSelectedLogLevel(String selectedLogLevel) {
		MessageListener.selectedLogLevel = LOG_LEVEL.indexOf(selectedLogLevel);
	}

	public static List<String> getLogLevels() {
		return LOG_LEVEL;
	}
	
	public static int getLevelOfLog(String logLevel) {
		return LOG_LEVEL.indexOf(logLevel);
	}
}
