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
package nl.nn.ibistesttool.tibet2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
public class Debugger extends nl.nn.ibistesttool.Debugger {
	private static final String RESEND_ADAPTER = "ResendFromExceptionLog";

	@Override
	public List<String> getStubStrategies() {
		List<String> stubStrategies = new ArrayList<>();
		stubStrategies.add(STUB_STRATEY_NEVER);
		return stubStrategies;
	}

	@Override
	public String getDefaultStubStrategy() {
		return STUB_STRATEY_NEVER;
	}

	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		String errorMessage = null;
		if ("Table EXCEPTIONLOG".equals(originalReport.getName())) {
			List<Checkpoint> checkpoints = originalReport.getCheckpoints();
			Checkpoint checkpoint = checkpoints.get(0);
			String inputMessage = checkpoint.getMessageWithResolvedVariables(reportRunner);
			IAdapter adapter = ibisManager.getRegisteredAdapter(RESEND_ADAPTER);
			if (adapter != null) {
				IPipeLineSession pipeLineSession = new PipeLineSessionBase();
				synchronized(inRerun) {
					inRerun.add(correlationId);
				}
				try {
					if(securityContext.getUserPrincipal() != null)
						pipeLineSession.put("principal", securityContext.getUserPrincipal().getName());
					PipeLineResult processResult = adapter.processMessage(correlationId, new Message(inputMessage), pipeLineSession);
					if (!(processResult.getState().equalsIgnoreCase("success") && processResult.getResult().asString().equalsIgnoreCase("<ok/>"))) {
						errorMessage = "Rerun failed. Result of adapter " + RESEND_ADAPTER + ": " + processResult.getResult().asString();
					}
				} catch (IOException e) {
					errorMessage = "Rerun failed. Exception in adapter " + RESEND_ADAPTER + ": (" + e.getClass().getName()+") " + e.getMessage();
				} finally {
					synchronized(inRerun) {
						inRerun.remove(correlationId);
					}
				}
			} else {
				errorMessage = "Adapter '" + RESEND_ADAPTER + "' not found";
			}
		} else {
			errorMessage = super.rerun(correlationId, originalReport, securityContext, reportRunner);
		}
		return errorMessage;
	}
}
