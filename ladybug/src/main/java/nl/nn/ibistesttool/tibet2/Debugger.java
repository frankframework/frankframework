package nl.nn.ibistesttool.tibet2;

import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;

/**
 * @author Jaco de Groot
 */
public class Debugger extends nl.nn.ibistesttool.Debugger {
	private static final String RESEND_ADAPTER = "ResendFromExceptionLog";

	public List getStubStrategies() {
		List stubStrategies = new ArrayList();
		stubStrategies.add(STUB_STRATEY_NEVER);
		return stubStrategies;
	}

	public String getDefaultStubStrategy() {
		return STUB_STRATEY_NEVER;
	}

	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext) {
		String errorMessage = null;
		if ("Table EXCEPTIONLOG".equals(originalReport.getName())) {
			List checkpoints = originalReport.getCheckpoints();
			Checkpoint checkpoint = (Checkpoint)checkpoints.get(0);
			String message = checkpoint.getMessage();
			IAdapter adapter = ibisManager.getRegisteredAdapter(RESEND_ADAPTER);
			if (adapter != null) {
				IPipeLineSession pipeLineSession = new PipeLineSessionBase();
				synchronized(inRerun) {
					inRerun.add(correlationId);
				}
				try {
					pipeLineSession.put("principal", securityContext.getUserPrincipal());
					PipeLineResult processResult = adapter.processMessage(correlationId, message, pipeLineSession);
					if (!(processResult.getState().equalsIgnoreCase("success")
							&& processResult.getResult().equalsIgnoreCase("<ok/>"))) {
						errorMessage = "Rerun failed. Result of adapter "
								+ RESEND_ADAPTER + ": "
								+ processResult.getResult();
					}
				} finally {
					synchronized(inRerun) {
						inRerun.remove(correlationId);
					}
				}
			} else {
				errorMessage = "Adapter '" + RESEND_ADAPTER + "' not found";
			}
		} else {
			errorMessage = super.rerun(correlationId, originalReport, securityContext);
		}
		return errorMessage;
	}

}
