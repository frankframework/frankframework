/*
   Copyright 2018 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.ibistesttool.tibet2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
public class Debugger extends org.frankframework.ibistesttool.Debugger {
	private static final String RESEND_ADAPTER_NAME = "ResendFromExceptionLog";
	private static final String RESEND_ADAPTER_CONFIG = "main";

	@Override
	public List<String> getStubStrategies() {
		List<String> stubStrategies = new ArrayList<>();
		stubStrategies.add(TestTool.STUB_STRATEGY_NEVER);
		return stubStrategies;
	}

	@Override
	public String getDefaultStubStrategy() {
		return TestTool.STUB_STRATEGY_NEVER;
	}

	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		if (!"Table EXCEPTIONLOG".equals(originalReport.getName())) {
			return super.rerun(correlationId, originalReport, securityContext, reportRunner);
		}

		List<Checkpoint> checkpoints = originalReport.getCheckpoints();
		Checkpoint checkpoint = checkpoints.get(0);
		String inputMessage = checkpoint.getMessageWithResolvedVariables(reportRunner);
		Configuration config = ibisManager.getConfiguration(RESEND_ADAPTER_CONFIG);
		if (config == null) {
			return "Configuration '" + RESEND_ADAPTER_CONFIG + "' not found";
		}
		Adapter adapter = config.getRegisteredAdapter(RESEND_ADAPTER_NAME);
		if (adapter == null) {
			return "Adapter '" + RESEND_ADAPTER_NAME + "' not found";
		}
		PipeLineSession pipeLineSession = new PipeLineSession();
		synchronized(inRerun) {
			inRerun.add(correlationId);
		}
		try {
			if(securityContext.getUserPrincipal() != null) {
				pipeLineSession.put("principal", securityContext.getUserPrincipal().getName());
			}
			PipeLineResult processResult = adapter.processMessageDirect(correlationId, new Message(inputMessage), pipeLineSession);
			if (!(processResult.isSuccessful() && "<ok/>".equalsIgnoreCase(processResult.getResult().asString()))) {
				return "Rerun failed. Result of adapter " + RESEND_ADAPTER_NAME + ": " + processResult.getResult().asString();
			}
			return null;
		} catch (IOException e) {
			return "Rerun failed. Exception in adapter " + RESEND_ADAPTER_NAME + ": (" + e.getClass().getName()+") " + e.getMessage();
		} finally {
			synchronized(inRerun) {
				inRerun.remove(correlationId);
			}
		}
	}
}
