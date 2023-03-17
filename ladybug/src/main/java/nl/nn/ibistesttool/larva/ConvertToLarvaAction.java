/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.ibistesttool.larva;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.extensions.CustomReportAction;
import nl.nn.testtool.extensions.CustomReportActionResult;

import nu.studer.java.util.OrderedProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class ConvertToLarvaAction implements CustomReportAction {

	String dir;

	@Override
	public String getButtonText() {
		return "Convert to Larva";
	}

	public void setReportsFolder(String dir) {
		this.dir = dir;
	}

	@Override
	public CustomReportActionResult handleReports(List<Report> reports) {
		CustomReportActionResult customReportActionResult = new CustomReportActionResult();
		customReportActionResult.setSuccessMessage("Generated larva test scenario(s) for reports: " + reports);

		for (Report report : reports) {
			if (report == null) {
				customReportActionResult.setErrorMessage("Couldn't get corresponding report object for row.");
				return customReportActionResult;
			}
			if (!report.getName().startsWith("Pipeline ")) {
				customReportActionResult.setErrorMessage("Report [" + report.getName() + "] is not a pipeline report. Test generation isn't implemented for this type of report.");
				return customReportActionResult;
			}
			String reportName = report.getName();
			String adapterName = reportName.substring(9);
			Path testDir = Paths.get(dir, adapterName);
			try {
				Files.createDirectories(testDir);
			} catch (IOException e) {
				customReportActionResult.setErrorMessage("Error occurred when creating test directory [" + testDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
				return customReportActionResult;
			}
			String scenarioSuffix = "01";
			try (Stream<Path> files = Files.list(testDir).filter(path -> path.getFileName().toString().matches("scenario\\d+.properties"))) {
				OptionalInt maxSuffix = files.mapToInt(path -> {
					String fileName = path.getFileName().toString();
					return Integer.parseInt(fileName.substring(8,fileName.indexOf(".")));
				}).max();
				if (maxSuffix.isPresent()) {
					scenarioSuffix = String.format("%0" + 2 + "d", maxSuffix.getAsInt() + 1);
				}
			} catch (IOException e) {
				customReportActionResult.setErrorMessage("Error occurred counting existing scenarios in test directory [" + testDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
				return customReportActionResult;
			}
			Path scenarioDir = testDir.resolve(scenarioSuffix);
			Path scenarioFile = testDir.resolve("scenario" + scenarioSuffix + ".properties");
			Path commonFile = testDir.resolve("common.properties");
			if (!Files.exists(scenarioDir)) {
				try {
					Files.createDirectory(scenarioDir);
				} catch (IOException e) {
					customReportActionResult.setErrorMessage("Error occurred when creating scenario directory [" + scenarioDir.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
					return customReportActionResult;
				}
			} else {
				try(Stream<Path> files = Files.list(scenarioDir)) {
					if (files.findAny().isPresent()) {
						customReportActionResult.setErrorMessage("Error: scenario directory [" + scenarioDir.toAbsolutePath() + "] already exists and is not empty. Not converting report [" + reportName + "]");
						return customReportActionResult;
					}
				} catch (IOException e) {
					customReportActionResult.setErrorMessage("Error: could not read from the filesystem. Not converting report [" + reportName + "]");
					return customReportActionResult;
				}
			}
			if (!Files.exists(commonFile)) {
				try {
					Files.createFile(commonFile);
				} catch (IOException e) {
					customReportActionResult.setErrorMessage("Error occurred when creating common file [" + commonFile.toAbsolutePath() + "] for report [" + reportName + "]: " + e);
					return customReportActionResult;
				}
			}
			new Scenario(report, scenarioDir, scenarioFile, commonFile, adapterName, customReportActionResult);
		}

		return customReportActionResult;
	}

	public static String stepPadding(int i) {
		return String.format("%0" + 2 + "d", i);
	}

	private static class Scenario {
		private static final HashSet<String> ignoredSenders = new HashSet(Arrays.asList(
				"nl.nn.adapterframework.jdbc.ResultSet2FileSender",
				"nl.nn.adapterframework.jdbc.DirectQuerySender",
				"nl.nn.adapterframework.jdbc.FixedQuerySender",
				"nl.nn.adapterframework.jdbc.XmlQuerySender",
				"nl.nn.adapterframework.senders.DelaySender",
				"nl.nn.adapterframework.senders.EchoSender",
				"nl.nn.adapterframework.senders.IbisLocalSender",
				"nl.nn.adapterframework.senders.LogSender",
				"nl.nn.adapterframework.senders.ParallelSenders",
				"nl.nn.adapterframework.senders.SenderSeries",
				"nl.nn.adapterframework.senders.SenderWrapper",
				"nl.nn.adapterframework.senders.XsltSender",
				"nl.nn.adapterframework.senders.CommandSender",
				"nl.nn.adapterframework.senders.FixedResultSender",
				"nl.nn.adapterframework.senders.JavascriptSender",
				"nl.nn.adapterframework.jdbc.MessageStoreSender",
				"nl.nn.adapterframework.senders.ReloadSender",
				"nl.nn.adapterframework.compression.ZipWriterSender",
				"nl.nn.adapterframework.senders.LocalFileSystemSender"
		));

		private static final HashSet<String> ignoredSessionKeys = new HashSet(Arrays.asList(
				"cid",
				"id",
				"key",
				"messageId",
				"originalMessage",
				"tcid",
				"tsReceived",
				"conversationId",
				"timestamp",
				"tsSent",
				"xPathLogKeys",
				"replyTo",
				"JmsSession"
		));
		Path resultFolder;
		private String suffix = "01", reportName;
		OrderedProperties scenarioProperties, commonProperties;
		Map<String, String> existingStubs;
		CustomReportActionResult customReportActionResult;

		public Scenario(Report report, Path scenarioDir, Path scenario, Path common, String adapterName, CustomReportActionResult customReportActionResult) {
			this.reportName = report.getName();
			this.customReportActionResult = customReportActionResult;
			String scenarioDirPrefix = scenarioDir.getFileName().toString() + "/";
			File scenarioFile = scenario.toFile();
			File commonFile = common.toFile();
			OutputStreamWriter scenarioWriter, commonWriter;

			OrderedProperties.OrderedPropertiesBuilder commonBuilder = new OrderedProperties.OrderedPropertiesBuilder();
			commonBuilder.withOrdering(new CommonPropertiesComparator());
			commonBuilder.withSuppressDateInComment(true);
			commonProperties = commonBuilder.build();

			OrderedProperties.OrderedPropertiesBuilder scenarioBuilder = new OrderedProperties.OrderedPropertiesBuilder();
			scenarioBuilder.withOrdering(new ScenarioPropertiesComparator());
			scenarioBuilder.withSuppressDateInComment(true);
			scenarioProperties = scenarioBuilder.build();

			scenarioProperties.setProperty("scenario.description", "Test scenario for adapter " + adapterName + ", automatically generated based on a ladybug report");
			scenarioProperties.setProperty("include", "common.properties");
			String adapterProperty = "adapter." + adapterName;
			int paramI = 1;
			int current_step_nr = 0;
			List<Checkpoint> checkpoints = report.getCheckpoints();
			scenarioProperties.setProperty("step" + ++current_step_nr + "." + adapterProperty + ".write", scenarioDirPrefix + stepPadding(current_step_nr) + "-" + adapterName + "-in.xml");
			createInputOutputFile(scenarioDir, current_step_nr, "adapter", adapterName, true, checkpoints.get(0).getMessage());
			commonProperties.setProperty(adapterProperty + ".className", "nl.nn.adapterframework.senders.IbisJavaSender");
			commonProperties.setProperty(adapterProperty + ".serviceName", "testtool-" + adapterName);
			commonProperties.setProperty(adapterProperty + ".convertExceptionToMessage", "true");

			boolean skipUntilEndOfSender = false;
			String skipUntilEndOfSenderName = "";
			int skipUntilEndOfSenderLevel = -1;

			for (Checkpoint checkpoint : checkpoints) {
				if (skipUntilEndOfSender) {
					//If we're currently stubbing a sender, and we haven't reached the end of it yet
					if (checkpoint.getLevel() == skipUntilEndOfSenderLevel && checkpoint.getType() == Checkpoint.TYPE_ENDPOINT && checkpoint.getName().equals(skipUntilEndOfSenderName)) {
						createInputOutputFile(scenarioDir, current_step_nr, "stub", checkpoint.getName().substring(7), false, checkpoint.getMessage());
						skipUntilEndOfSender = false;
					}
				} else if (checkpoint.getType() < 3 && checkpoint.getName().startsWith("Sender ")) {
					if (!ignoredSenders.contains(checkpoint.getSourceClassName())) {
						//If sender should be stubbed:
						String senderName = checkpoint.getName().substring(7);
						String senderProperty = "stub." + senderName;
						scenarioProperties.setProperty("step" + ++current_step_nr + "." + senderProperty + ".read", scenarioDirPrefix + stepPadding(current_step_nr) + "-" + senderName + "-in.xml");
						createInputOutputFile(scenarioDir, current_step_nr, "stub", senderName, true, checkpoint.getMessage());
						scenarioProperties.setProperty("step" + ++current_step_nr + "." + senderProperty + ".write", scenarioDirPrefix + stepPadding(current_step_nr) + "-" + senderName + "-out.xml");

						String serviceName = "testtool-Call" + senderName;
						String existingStubName = existingStubs.get(serviceName.toLowerCase());
						if (!senderProperty.equals(existingStubName)) {
							existingStubs.put(serviceName.toLowerCase(), senderProperty);
							commonProperties.setProperty(senderProperty + ".className", "nl.nn.adapterframework.receivers.JavaListener");
							commonProperties.setProperty(senderProperty + ".serviceName", serviceName);

							if (existingStubName != null) {
								commonProperties.removeProperty(existingStubName + ".className");
								commonProperties.removeProperty(existingStubName + ".serviceName");
								try {
									replaceStubName(resultFolder, existingStubName, senderProperty);
								} catch (IOException e) {
									customReportActionResult.setErrorMessage(customReportActionResult.getErrorMessage() + "\nError occured when replacing old stub name [" + existingStubName + "] with new stub name [" + senderProperty + "] for report [" + reportName + "]");
									return;
								}
							}
						}

						skipUntilEndOfSender = true;
						skipUntilEndOfSenderName = senderName;
						skipUntilEndOfSenderLevel = checkpoint.getLevel();
					}
				} else if (checkpoint.getLevel() == 1 && checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT) {
					//SessionKey for listener found
					String sessionKeyName = checkpoint.getName().substring(11);
					if (!ignoredSessionKeys.contains(sessionKeyName)) {
						scenarioProperties.setProperty(adapterProperty + ".param" + paramI + ".name", sessionKeyName);
						scenarioProperties.setProperty(adapterProperty + ".param" + paramI + ".value", checkpoint.getMessage());
						paramI++;
					}
				}
			}
			scenarioProperties.setProperty("step" + ++current_step_nr + "." + adapterProperty + ".read", scenarioDirPrefix + stepPadding(current_step_nr) + "-" + adapterName + "-out.xml");
			createInputOutputFile(scenarioDir, current_step_nr, "adapter", adapterName, false, checkpoints.get(checkpoints.size() - 1).getMessage());

			System.out.println("Scenario file: " + scenario.toAbsolutePath());
			System.out.println("Common file: " + common.toAbsolutePath());
			System.out.println("Scenario dir: " + scenarioDir.toAbsolutePath());

			try {
				scenarioWriter = new OutputStreamWriter(Files.newOutputStream(scenarioFile.toPath()), StandardCharsets.UTF_8);;
				scenarioProperties.store(scenarioWriter, null);
			} catch (IOException e) {
				customReportActionResult.setErrorMessage("Failed to write properties to file [" + scenarioFile + "] for report [" + reportName + "]");
				return;
			}
			try {
				commonWriter = new OutputStreamWriter(Files.newOutputStream(commonFile.toPath()), StandardCharsets.UTF_8);;
				commonProperties.store(commonWriter, null);
			} catch (IOException e) {
				customReportActionResult.setErrorMessage("Failed to write properties to file [" + commonFile + "] for report [" + reportName + "]");
				return;
			}
		}

		private static void replaceStubName(Path folder, String oldStubName, String newStubName) throws IOException {
			if (oldStubName.equals(newStubName)) {
				return;
			}
			File[] scenarios = folder.toFile().listFiles((dir, name) -> name.matches("scenario\\d*\\.properties"));
			for (File scenario : scenarios) {
				List<String> result = Files.readAllLines(scenario.toPath());
				boolean changed = false;
				for (int i = 0; i < result.size(); i++) {
					String line = result.get(i);
					if (line.contains(oldStubName + ".read") || line.contains(oldStubName + ".write")) {
						result.set(i, line.replace(oldStubName, newStubName));
						changed = true;
					}
				}
				if (changed) Files.write(scenario.toPath(), result);
			}
		}

		private void createInputOutputFile(Path folder, int step, String type, String name, boolean startpoint, String message) {
			String filename = String.format("%s-%s-%s-%s.xml", stepPadding(step), type, name, startpoint ? "in" : "out");
			File messageFile = folder.resolve(filename).toFile();
			try {
				if (messageFile.createNewFile()) {
					Files.write(messageFile.toPath(), message.getBytes(StandardCharsets.UTF_8));
				}
			} catch (IOException e) {
				customReportActionResult.setErrorMessage(customReportActionResult.getErrorMessage() + "\nFailed to create file for message: [" + messageFile + "] for report [" + reportName + "]");
			}
		}
	}

}
