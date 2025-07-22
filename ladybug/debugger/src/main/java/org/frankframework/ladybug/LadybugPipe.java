/*
   Copyright 2019, 2020 Nationale-Nederlanden, 2024 WeAreFrank

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
package org.frankframework.ladybug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Forward;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

/**
 * Call Ladybug Test Tool to rerun the reports present in test storage (see Test tab in Ladybug)
 *
 * @author Jaco de Groot
 *
 */
@Forward(name = "success", description = "no errors and all tests passed")
@Forward(name = "failure", description = "errors or failed tests")
public class LadybugPipe extends FixedForwardPipe {
	private static final String FAILURE_FORWARD_NAME = "failure";
	private PipeForward failureForward;
	private boolean writeToLog = false;
	private boolean writeToSystemOut = false;
	private boolean checkRoles = false;
	private boolean enableReportGenerator = false;
	private TestTool testTool;
	private CrudStorage testStorage;
	private Storage debugStorage;
	private ReportXmlTransformer reportXmlTransformer;
	private String exclude;
	private Pattern excludeRegexPattern;
	private ReportNameComparator reportNameComparator;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		failureForward = findForward(FAILURE_FORWARD_NAME);
		if (failureForward == null) {
			failureForward = getSuccessForward();
		}
		if (StringUtils.isNotEmpty(exclude)) {
			excludeRegexPattern = Pattern.compile(exclude);
		}
		reportNameComparator = new ReportNameComparator();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		XmlBuilder results = new XmlBuilder("Results");
		int reportsPassed = 0;

		List<Report> reports = new ArrayList<>();
		try {
			testStorage.getReport(-1); // Workaround XmlStorage.getStorageIds() returning empty list while waiting for fix in Ladybug
			List<Integer> storageIds = testStorage.getStorageIds();
			for (Integer storageId : storageIds) {
				Report report = testStorage.getReport(storageId);
				String fullReportPath = (report.getPath() != null ? report.getPath() : "") + report.getName();

				if(excludeRegexPattern == null || !excludeRegexPattern.matcher(fullReportPath).matches()) {
					reports.add(report);
				}
			}
		} catch (StorageException e) {
			addExceptionElement(results, e);
		}

		ReportRunner reportRunner = new ReportRunner();
		reportRunner.setTestTool(testTool);
		reportRunner.setDebugStorage(debugStorage);

		Collections.sort(reports, reportNameComparator);
		long startTime = System.currentTimeMillis();
		boolean reportGeneratorEnabledOldValue = testTool.isReportGeneratorEnabled();
		if(enableReportGenerator) {
			testTool.setReportGeneratorEnabled(true);
			testTool.sendReportGeneratorStatusUpdate();
		}

		runReports(reportRunner, reports);

		if(enableReportGenerator) {
			testTool.setReportGeneratorEnabled(reportGeneratorEnabledOldValue);
			testTool.sendReportGeneratorStatusUpdate();
		}
		long endTime = System.currentTimeMillis();

		for (Report report : reports) {
			RunResult runResult = reportRunner.getResults().get(report.getStorageId());
			long originalDuration = report.getEndTime() - report.getStartTime();
			long duration = -1;
			boolean equal = false;
			String error = "";

			if (runResult.errorMessage != null) {
				error = runResult.errorMessage;
			} else {
				Report runResultReport = null;
				try {
					runResultReport = reportRunner.getRunResultReport(runResult.correlationId);
				} catch (StorageException e) {
					addExceptionElement(results, e);
				}

				if (runResultReport == null) {
					error = "Result report not found. Report generator not enabled?";
				} else {
					duration = runResultReport.getEndTime() - runResultReport.getStartTime();
					report.setGlobalReportXmlTransformer(reportXmlTransformer);
					runResultReport.setGlobalReportXmlTransformer(reportXmlTransformer);
					runResultReport.setTransformation(report.getTransformation());
					runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
					if(report.toXml(reportRunner).equals(runResultReport.toXml(reportRunner))) {
						equal = true;
						reportsPassed++;
					}
				}
			}
			XmlBuilder result = new XmlBuilder("Result");
			results.addSubElement(result);
			result.addSubElement("Path", report.getPath());
			result.addSubElement("Name", report.getName());
			result.addSubElement("OriginalDuration", "" + originalDuration);
			if(duration > -1) result.addSubElement("Duration", "" + duration);
			result.addSubElement("Equal", "" + equal);
			if(!error.isEmpty()) result.addSubElement("Error", error);
			if (writeToLog || writeToSystemOut) {
				writeToLogOrSysOut("Path=\"" + report.getPath() + "\", "
						+ "Name=\"" + report.getName() + "\", "
						+ "OriginalDuration=\"" + originalDuration + "\", "
						+ (duration > -1 ? "Duration=\"" + duration + "\", " : "")
						+ "Equal=\"" + equal + "\""
						+ (!error.isEmpty() ? ", Error=\"" + error + "\"" : ""));
			}
		}

		boolean allReportsPassed = reportsPassed == reports.size();
		if (writeToLog || writeToSystemOut) {
			writeToLogOrSysOut("Total=\"" + reports.size() + "\", "
					+ "Passed=\"" + reportsPassed + "\", "
					+ "Failed=\"" + (reports.size() - reportsPassed) + "\", "
					+ "TotalDuration=\"" + (endTime - startTime) + "\", "
					+ "Equal=\"" + allReportsPassed + "\"");
		}
		PipeForward forward = allReportsPassed ? getSuccessForward() : failureForward;
		return new PipeRunResult(forward, results.asMessage());
	}

	private void runReports(ReportRunner reportRunner, List<Report> reports) {
		if (checkRoles) {
			reportRunner.run(reports, false, true);
		} else {
			SecurityContext context = SecurityContextHolder.getContext();
			Authentication originalAuthentication = context.getAuthentication();
			try {
				Authentication allowAllAuthentication = new AnonymousAuthenticationToken("LadybugPipe", "allow-all", Collections.singletonList(new SimpleGrantedAuthority("ROLE_IbisTester")));
				context.setAuthentication(allowAllAuthentication);
				reportRunner.run(reports, false, true);
			} finally {
				context.setAuthentication(originalAuthentication);
			}
		}
	}

	private void writeToLogOrSysOut(String message) {
		if (writeToLog) {
			log.info(message);
		}
		if (writeToSystemOut) {
			System.out.println(message);
		}
	}

	private void addExceptionElement(XmlBuilder results, Exception e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		XmlBuilder exception = new XmlBuilder("Exception");
		exception.addSubElement("Message", e.getMessage());
		exception.addSubElement("Stacktrace", stringWriter.toString());
		results.addSubElement(exception);
	}

	/**
	 * whether or not to write results to the logfile (testtool4&lt;instance.name&gt;)
	 * @ff.default false
	 */
	public void setWriteToLog(boolean writeToLog) {
		this.writeToLog = writeToLog;
	}

	/**
	 * whether or not to write results to system out
	 * @ff.default false
	 */
	public void setWriteToSystemOut(boolean writeToSystemOut) {
		this.writeToSystemOut = writeToSystemOut;
	}

	/**
	 * Set this to {@code true} if you wish to enforce the
	 * user roles (e.g. when using an HTTP based listener).
	 * When set to false the Ladybug is run anonymously.
	 * @ff.default false
	 */
	public void setCheckRoles(boolean checkRoles) {
		this.checkRoles = checkRoles;
	}

	/**
	 * Set to <code>true</code> to enable Ladybug's report generator for the duration of the scheduled report runs
	 * then revert it to its original setting
	 * @ff.default false
	 */
	public void setEnableReportGenerator(boolean enabled) {
		enableReportGenerator = enabled;
	}

	/** When set, reports with a full path (path + name) that matches with the specified regular expression are skipped. For example, \"/Unscheduled/.*\" or \".*SKIP\". */
	public void setExclude(String exclude) {
		this.exclude = exclude;
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setTestStorage(CrudStorage testStorage) {
		this.testStorage = testStorage;
	}

	public void setDebugStorage(Storage debugStorage) {
		this.debugStorage = debugStorage;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

}

class ReportNameComparator implements Comparator<Report> {

	@Override
	public int compare(Report o1, Report o2) {
		return o1.getFullPath().compareTo(o2.getFullPath());
	}
}
