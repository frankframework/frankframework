/*
   Copyright 2018 Nationale-Nederlanden

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

import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.TestPreparer;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;

import java.io.StringWriter;

/**
 * Call Larva Test Tool
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedForwardPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfThreads(int) numberOfThreads}</td><td>threads</td><td>1</td></tr>
 * <tr><td>{@link #setWaitBeforeCleanup(String) waitBeforeCleanup}</td><td>ms</td><td>100</td></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>the larva log level: one of [debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]</td><td>wrong pipeline messages</td></tr>
 * <tr><td>{@link #setWriteToLog(boolean) writeToLog}</td><td></td><td>false</td></tr>
 * <tr><td>{@link #setWriteToSystemOut(boolean) writeToSystemOut}</td><td></td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the larva timeout</td>30000</tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>no errors and all scenarios passed</td></tr>
 * <tr><td>"fail"</td><td>errors or failed scenarios</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * 
 * @author Jaco de Groot
 *
 */
public class LarvaPipe extends FixedForwardPipe {
	
	@Autowired
	IbisContext ibisContext;
	
	private final String DEFAULT_LOG_LEVEL = "Wrong Pipeline Messages";
	private final String FORWARD_FAIL="fail";
	
	private boolean writeToLog = false;
	private boolean writeToSystemOut = false;
	private String execute;
	private String logLevel=DEFAULT_LOG_LEVEL;
	private String waitBeforeCleanup="100";
	private int numberOfThreads = 1;
	private int timeout=30000;
	
	private PipeForward failForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getLogLevel()==null) {
			log("Warn","No log level detected, using [" + DEFAULT_LOG_LEVEL + "]");
			setLogLevel(DEFAULT_LOG_LEVEL);
		}
		try {
			log("Debug", "Setting log level to [" + getLogLevel() + "]");
			MessageListener.setSelectedLogLevel(getLogLevel());
		} catch (Exception e) {
			throw new ConfigurationException("illegal log level ["+getLogLevel()+"]");
		}

		failForward=findForward(FORWARD_FAIL);
		if (failForward==null) {
			failForward=getForward();
		}
	}
	
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		//IbisContext ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
		AppConstants appConstants = AppConstants.getInstance();
		// Property webapp.realpath is not available in appConstants which was
		// created with AppConstants.getInstance(ClassLoader classLoader), this
		// should be fixed but for now use AppConstants.getInstance().
		String realPath = AppConstants.getInstance().getResolvedProperty("webapp.realpath") + "larva/";
		String currentScenariosRootDirectory = TestPreparer.initScenariosRootDirectories(realPath, null, appConstants);
		String paramExecute = currentScenariosRootDirectory;
		if (StringUtils.isNotEmpty(execute)) {
			paramExecute = paramExecute + execute;
		}
		int numScenariosFailed=TestTool.runScenarios(paramExecute, parseWaitBeforeCleanup(), currentScenariosRootDirectory, getNumberOfThreads(), getTimeout());
		PipeForward forward=numScenariosFailed==0? getForward(): failForward;
		return new PipeRunResult(forward, MessageListener.getLastTotalMessage());
	}

	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}
	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}
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
	private int parseWaitBeforeCleanup() {
		int waitBeforeCleanup = 100;
		try {
			String paramWaitBeforeCleanUp = getWaitBeforeCleanup();
			waitBeforeCleanup = Integer.parseInt(paramWaitBeforeCleanUp);
		}catch (NumberFormatException e) {
			log("Warn", "Could not parse wait before cleanup. Using default 100ms.");
		}
		log("Debug", "Using " + waitBeforeCleanup + "ms as a wait before cleanup parameter.");
		return waitBeforeCleanup;
	}

	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}

	private void log(String level, String message) {
		if(writeToLog) {
			switch (level) {
				case "Debug":
					log.debug(message);
					break;
				case "Warn":
					log.warn(message);
					break;
				case "Error":
					log.error(message);
					break;
			}
		}
		if(writeToSystemOut) {
			System.out.println(level + ": " + message);
		}
	}
}
