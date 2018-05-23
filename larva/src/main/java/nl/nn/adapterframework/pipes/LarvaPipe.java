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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Call Larva Test Tool
 * 
 * @author Jaco de Groot
 *
 */
public class LarvaPipe extends FixedForwardPipe {
	private boolean writeToLog = false;
	private boolean writeToSystemOut = false;
	private String execute;

	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}

	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}

	public void setExecute(String execute) {
		this.execute = execute;
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		IbisContext ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
		AppConstants appConstants = TestTool.getAppConstants(ibisContext);
		// Property webapp.realpath is not available in appConstants which was
		// created with AppConstants.getInstance(ClassLoader classLoader), this
		// should be fixed but for now use AppConstants.getInstance().
		String realPath = AppConstants.getInstance().getResolvedProperty("webapp.realpath") + "larva/";
		List scenariosRootDirectories = new ArrayList();
		List scenariosRootDescriptions = new ArrayList();
		String currentScenariosRootDirectory = TestTool.initScenariosRootDirectories(
				appConstants, realPath,
				null, scenariosRootDirectories,
				scenariosRootDescriptions, null);
		String paramScenariosRootDirectory = currentScenariosRootDirectory;
		String paramExecute = currentScenariosRootDirectory;
		if (StringUtils.isNotEmpty(execute)) {
			paramExecute = paramExecute + execute;
		}
		String paramLogLevel = "wrong pipeline messages";
		String paramAutoScroll = "true";
		String paramWaitBeforeCleanUp = "100";
		LogWriter out = new LogWriter();
		out.setLogger(log);
		out.setWriteToLog(writeToLog);
		out.setWriteToSystemOut(writeToSystemOut);
		boolean silent = true;
		TestTool.runScenarios(ibisContext, appConstants, paramLogLevel,
				paramAutoScroll, paramExecute,
				paramWaitBeforeCleanUp, realPath,
				paramScenariosRootDirectory,
				out, silent);
		return new PipeRunResult(getForward(), out.toString());
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
