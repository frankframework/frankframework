/*
   Copyright 2018 Nationale-Nederlanden, 2022-2026 WeAreFrank!

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
package org.frankframework.ladybug.tibet2;

import java.io.IOException;
import java.util.List;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.SecurityContext;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.ladybug.LadybugDebugger;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

/**
 * @author Jaco de Groot
 */
public class Tibet2Debugger extends LadybugDebugger {
	private static final String RESEND_ADAPTER_NAME = "ResendFromExceptionLog";
	private static final String RESEND_ADAPTER_CONFIG = "main";

	@Override
	public List<String> getStubStrategies() {
		return List.of(TestTool.STUB_STRATEGY_NEVER);
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
		synchronized(inRerun) {
			inRerun.add(correlationId);
		}
		try (PipeLineSession pipeLineSession = new PipeLineSession()) {
			pipeLineSession.put("principal", BusMessageUtils.getUserPrincipalName());
			pipeLineSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
			// Analog to test a pipeline that is using: "testmessage" + Misc.createSimpleUUID();
			String messageId = MessageUtils.generateMessageId("tibet2-resend");

			PipeLineResult processResult = adapter.processMessageDirect(messageId, new Message(inputMessage), pipeLineSession);
			String stringResult = processResult.getResult().asString();
			if (!(processResult.isSuccessful() && "<ok/>".equalsIgnoreCase(stringResult))) {
				return "Rerun failed. Result of adapter " + RESEND_ADAPTER_NAME + ": " + stringResult;
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
