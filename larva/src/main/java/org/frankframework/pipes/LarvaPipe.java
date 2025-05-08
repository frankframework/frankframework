/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Forward;
import org.frankframework.larva.LarvaConfig;
import org.frankframework.larva.LarvaException;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestRunStatus;
import org.frankframework.stream.Message;

/**
 * Call Larva Test Tool
 *
 * @author Jaco de Groot
 *
 */
@Forward(name = "success", description = "no errors and all tests passed")
@Forward(name = "failure", description = "errors or failed tests")
public class LarvaPipe extends FixedForwardPipe {

	public static final LarvaLogLevel DEFAULT_LOG_LEVEL = LarvaLogLevel.WRONG_PIPELINE_MESSAGES;
	public static final String FORWARD_FAILURE="failure";

	private @Getter boolean writeToLog = false;
	private @Getter boolean writeToSystemOut = false;
	private @Getter String execute;
	private @Getter LarvaLogLevel logLevel;
	private @Getter String waitBeforeCleanup="100";
	private @Getter int timeout = 10_000;

	private PipeForward failureForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getLogLevel() == null) {
			log.warn("no log level specified, setting to default [{}]", DEFAULT_LOG_LEVEL.getName());
			setLogLevel(DEFAULT_LOG_LEVEL);
		}
		failureForward=findForward(FORWARD_FAILURE);
		if(failureForward == null && (failureForward = findForward("fail")) != null) {
			ConfigurationWarnings.add(this, log, "forward 'fail' has been deprecated, use forward 'failure' instead");
		}
		if (failureForward==null) {
			failureForward=getSuccessForward();
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) {
		ApplicationContext applicationContext = getAdapter().getConfiguration().getApplicationContext();
		LogWriter out = new LogWriter(log, isWriteToLog(), isWriteToSystemOut());
		LarvaConfig larvaConfig = new LarvaConfig();
		larvaConfig.setLogLevel(getLogLevel());
		if (StringUtils.isNotBlank(getWaitBeforeCleanup())) {
			larvaConfig.setWaitBeforeCleanup(Integer.parseInt(getWaitBeforeCleanup()));
		}
		if (getTimeout() > 0) {
			larvaConfig.setTimeout(getTimeout());
		}

		LarvaTool larvaTool = LarvaTool.createInstance(applicationContext, out);
		String paramExecute = larvaTool.getTestRunStatus().initScenarioDirectories();
		if (StringUtils.isNotEmpty(getExecute())) {
			paramExecute = paramExecute + getExecute();
		}
		PipeForward forward;
		try {
			TestRunStatus testRunStatus = larvaTool.runScenarios(paramExecute);
			int numScenariosFailed = testRunStatus.getScenariosFailedCount();
			forward = numScenariosFailed==0 ? getSuccessForward() : failureForward;
		} catch (LarvaException e) {
			forward = failureForward;
		}
		return new PipeRunResult(forward, out.toString());
	}

	/**
	 * @ff.default false
	 */
	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}

	/**
	 * @ff.default false
	 */
	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}

	/** The scenario sub directory to execute */
	public void setExecute(String execute) {
		this.execute = execute;
	}

	/**
	 * the larva log level: one of [debug], [pipeline messages prepared for diff], [pipeline messages], [wrong pipeline messages prepared for diff], [wrong pipeline messages], [step passed/failed], [scenario passed/failed], [scenario failed], [totals], [error]
	 * @ff.default wrong pipeline messages
	 */
	public void setLogLevel(LarvaLogLevel logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * @ff.default 100ms
	 */
	public void setWaitBeforeCleanup(String waitBeforeCleanup) {
		this.waitBeforeCleanup = waitBeforeCleanup;
	}

	 /** the larva timeout in milliseconds
	 * @ff.default 10000
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}

@AllArgsConstructor
class LogWriter extends StringWriter {
	private Logger log;
	private boolean writeToLog;
	private boolean writeToSystemOut;

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
