/*
   Copyright 2018, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Call Larva Test Tool
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedForwardPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWaitBeforeCleanup(String) waitBeforeCleanup}</td><td>ms</td><td>100</td></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>the larva log level: one of [debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]</td><td>wrong pipeline messages</td></tr>
 * <tr><td>{@link #setWriteToLog(boolean) writeToLog}</td><td></td><td>false</td></tr>
 * <tr><td>{@link #setWriteToSystemOut(boolean) writeToSystemOut}</td><td></td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the larva timeout</td>30000</tr>
 * </table>
 * </p>
 * 
 * @ff.forward success no errors and all tests passed
 * @ff.forward fail errors or failed tests
 * 
 * @author Jaco de Groot
 *
 */
public class LarvaPipe extends FixedForwardPipe {
	
	public final String DEFAULT_LOG_LEVEL = "wrong pipeline messages";
	public final String FORWARD_FAIL="fail";
	
	private boolean writeToLog = false;
	private boolean writeToSystemOut = false;
	private String execute;
	private String logLevel=DEFAULT_LOG_LEVEL;
	private String waitBeforeCleanup="100";
	private int timeout=30000;
	
	private PipeForward failForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getLogLevel()==null) {
			log.warn("no log level specified, setting to default ["+DEFAULT_LOG_LEVEL+"]");
			setLogLevel(DEFAULT_LOG_LEVEL);
		} else {
			String[] LOG_LEVELS = TestTool.LOG_LEVEL_ORDER.split(",\\s*");
			if (!Arrays.asList(LOG_LEVELS).contains("["+getLogLevel()+"]")) {
				throw new ConfigurationException("illegal log level ["+getLogLevel()+"]");
			}
		}
		failForward=findForward(FORWARD_FAIL);
		if (failForward==null) {
			failForward=getSuccessForward();
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		IbisContext ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
		AppConstants appConstants = TestTool.getAppConstants(ibisContext);
		// Property webapp.realpath is not available in appConstants which was
		// created with AppConstants.getInstance(ClassLoader classLoader), this
		// should be fixed but for now use AppConstants.getInstance().
		String realPath = AppConstants.getInstance().getResolvedProperty("webapp.realpath") + "larva/";
		List<String> scenariosRootDirectories = new ArrayList<String>();
		List<String> scenariosRootDescriptions = new ArrayList<String>();
		String currentScenariosRootDirectory = TestTool.initScenariosRootDirectories(
				appConstants, realPath,
				null, scenariosRootDirectories,
				scenariosRootDescriptions, null);
		String paramScenariosRootDirectory = currentScenariosRootDirectory;
		String paramExecute = currentScenariosRootDirectory;
		if (StringUtils.isNotEmpty(execute)) {
			paramExecute = paramExecute + execute;
		}
		String paramLogLevel = getLogLevel();
		String paramAutoScroll = "true";
		String paramWaitBeforeCleanUp = getWaitBeforeCleanup();
		LogWriter out = new LogWriter();
		out.setLogger(log);
		out.setWriteToLog(writeToLog);
		out.setWriteToSystemOut(writeToSystemOut);
		boolean silent = true;
		TestTool.setTimeout(getTimeout());
		int numScenariosFailed=TestTool.runScenarios(ibisContext, appConstants, paramLogLevel,
								paramAutoScroll, paramExecute,
								paramWaitBeforeCleanUp, getTimeout(), realPath,
								paramScenariosRootDirectory,
								out, silent);
		PipeForward forward=numScenariosFailed==0? getSuccessForward(): failForward;
		return new PipeRunResult(forward, out.toString());
	}

	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}

	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}

	@IbisDoc("The scenario sub directory to execute")
	public void setExecute(String execute) {
		this.execute = execute;
	}

	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}


	public String getWaitBeforeCleanup() {
		return waitBeforeCleanup;
	}
	public void setWaitBeforeCleanup(String waitBeforeCleanup) {
		this.waitBeforeCleanup = waitBeforeCleanup;
	}


	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}

class LogWriter extends StringWriter {
	private Logger log;
	private boolean writeToLog = false;
	private boolean writeToSystemOut = false;

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}

	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}

	@Override
	public void write(String str) {
		if (writeToLog) {
			log.debug(str);
		}
		if (writeToSystemOut) {
			System.out.println(str);
		}
		super.write(str + "\n");
	}
}
