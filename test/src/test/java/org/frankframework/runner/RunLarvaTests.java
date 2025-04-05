package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.ScenarioRunner;
import org.frankframework.larva.TestConfig;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;

@Tag("integration")
public class RunLarvaTests {

	public static final LarvaLogLevel LARVA_LOG_LEVEL = LarvaLogLevel.STEP_PASSED_FAILED;

	private static ConfigurableApplicationContext applicationContext;
	private static LarvaTool larvaTool;
	private static ScenarioRunner scenarioRunner;
	private static AppConstants appConstants;
	private static String scenarioRootDir;

	@BeforeAll
	static void setup() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();
		applicationContext = springApplication.run();
		ServletContext servletContext = applicationContext.getBean(ServletContext.class);
		IbisContext ibisContext = FrankApplicationInitializer.getIbisContext(servletContext);
		appConstants = AppConstants.getInstance();

		larvaTool = new LarvaTool();
		TestConfig testConfig = larvaTool.getConfig();
		testConfig.setTimeout(2000);
		testConfig.setSilent(false);
		testConfig.setLogLevel(LARVA_LOG_LEVEL);
		testConfig.setAutoScroll(false);
		testConfig.setMultiThreaded(false);
		testConfig.setOut(new HtmlTagStrippingWriter(System.out));

		scenarioRunner = new ScenarioRunner(larvaTool, ibisContext, testConfig, appConstants, 100, LARVA_LOG_LEVEL);
		scenarioRootDir = larvaTool.initScenariosRootDirectories(null, new ArrayList<>(), new ArrayList<>());
	}

	@AfterAll
	static void tearDown() {
		CloseUtils.closeSilently(applicationContext);
	}

	@TestFactory
	Stream<DynamicNode> larvaTests() {
		List<File> allScenarioFiles = larvaTool.readScenarioFiles(appConstants, scenarioRootDir);

		return createScenarios(scenarioRootDir, "", allScenarioFiles);
	}

	private @Nonnull Stream<DynamicNode> createScenarioContainer(@Nonnull String baseFolder, @Nonnull Map.Entry<String, List<File>> scenarioFolder) {
		String scenarioFolderName = scenarioFolder.getKey();
		if (StringUtils.isBlank(scenarioFolderName)) {
			return createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue());
		}
		return Stream.of(DynamicContainer.dynamicContainer(scenarioFolderName, new File(baseFolder, scenarioFolderName).toURI(), createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue())));
	}

	private @Nonnull Stream<DynamicNode> createScenarios(@Nonnull String baseFolder, @Nonnull String subFolder, @Nonnull List<File> scenarioFiles) {
		String commonFolder = StringUtils.isBlank(subFolder) ? baseFolder : Paths.get(baseFolder, subFolder).toString();
		Map<String, List<File>> scenariosByFolder = ScenarioRunner.groupFilesByFolder(scenarioFiles, commonFolder);

		if (scenariosByFolder.size() == 1) {
			return scenarioFiles.stream()
					.map(this::convertLarvaScenarioToTest);
		} else {
			return scenariosByFolder.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.flatMap((Map.Entry<String, List<File>> nestedSubFolder) -> createScenarioContainer(commonFolder, nestedSubFolder));
		}
	}

	private DynamicTest convertLarvaScenarioToTest(File scenarioFile) {
		// Scenario name always computed from the scenario root dir to be understandable without context of immediate parent
		String scenarioName = scenarioFile.getAbsolutePath().substring(scenarioRootDir.length());
		return DynamicTest.dynamicTest(
				scenarioName, scenarioFile.toURI(), () -> {
					System.out.println("Running scenario: [" + scenarioName + "]");
					int scenarioPassed = 0;
					try {
						scenarioPassed = scenarioRunner.runOneFile(scenarioFile, scenarioRootDir, true);
					} catch (Exception e) {
						e.printStackTrace(System.out);
						fail("Exception in scenario execution: " + e.getMessage());
					}

					assertEquals(LarvaTool.RESULT_OK, scenarioPassed);
				}
		);
	}

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@Test
	@Disabled("This version fails only 15 scenarios, figure out why the other fails half the scenarios")
	void runLarvaTests() throws IOException {
		assertTrue(applicationContext.isRunning());

		// Wait until all adapters running
		ServletContext servletContext = applicationContext.getBean(ServletContext.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("execute", scenarioRootDir);
		request.setParameter("loglevel", LarvaLogLevel.SCENARIO_FAILED.getName());

		// Invoke Larva tests
		Writer writer = new StringWriter();
		System.err.println("Starting Scenarios");
		long start = System.currentTimeMillis();
		int result = LarvaTool.runScenarios(servletContext, request, writer);
		long end = System.currentTimeMillis();
		System.err.println("Scenarios executed; duration: " + (end - start) + "ms");
		writer.close();

		String larvaOutput = stripLarvaOutput(writer.toString());
		assertFalse(result < 0, () -> "Error in LarvaTool execution, result is [" + result + "] instead of 0; output from LarvaTool:\n\n" + larvaOutput);

		if (result > 0) {
			System.err.println(result + " Larva tests failed duration: " + (end - start) + "ms; \n\n" + larvaOutput);
		} else {
			System.err.println("All Larva tests succeeded in " + (end - start) + "ms");
		}
	}

	private @Nonnull String stripLarvaOutput(@Nonnull String input) {

		String cleaned = input.replaceAll("(?s)<form.*?</form>", ""); // Strip out the huge forms
		try {
			// Now try to strip out remaining HTML tags while preserving the text data
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			HtmlTagStrippingWriter writer = new HtmlTagStrippingWriter(baos);
			writer.write(cleaned);
			return baos.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return cleaned;
		}
	}

	static class HtmlTagStrippingWriter extends Writer {
		private final OutputStream out;
		private boolean writingTag = false;
		private boolean lastCharWasNewLine = false;
		private boolean writingHtmlEntity = false;
		private final StringBuffer htmlEntityBuffer = new StringBuffer();

		/**
		 * Create a new filtered writer.
		 *
		 * @param out a Writer object to provide the underlying stream.
		 * @throws NullPointerException if {@code out} is {@code null}
		 */
		protected HtmlTagStrippingWriter(@Nonnull OutputStream out) {
			this.out = out;
		}

		@Override
		public void write(int c) throws IOException {
			if (c == '<') {
				writingTag = true;
			} else if (c == '>' && writingTag) {
				writingTag = false;
			} else if (writingHtmlEntity) {
				htmlEntityBuffer.append((char) c);
				if (c == ';') {
					writingHtmlEntity = false;
					writeHtmlEntity();
				}
			} else if (c == '&') {
				writingHtmlEntity = true;
				htmlEntityBuffer.append((char) c);
			} else if (!writingTag) {
				boolean isNewLine = (c == '\n' || c == '\r');
				if (isNewLine) {
					if (!lastCharWasNewLine) {
						out.write(c); // Since we strip tags we might end up with lots of empty lines, try to avoid that.
					}
				} else {
					out.write(c);
				}
				lastCharWasNewLine = isNewLine;
			}
		}

		private void writeHtmlEntity() throws IOException {
			String entity = StringEscapeUtils.unescapeHtml4(htmlEntityBuffer.toString());
			out.write(entity.getBytes(StandardCharsets.UTF_8));
			htmlEntityBuffer.setLength(0);
		}

		@Override
		public void write(@Nonnull char[] cbuf, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				write(cbuf[i]);
			}
		}

		@Override
		public void write(@Nonnull char[] cbuf) throws IOException {
			for (int i = 0; i < cbuf.length; i++) {
				write(cbuf[i]);
			}
		}

		@Override
		public void write(@Nonnull String str) throws IOException {
			for (int i = 0; i < str.length(); i++) {
				write(str.charAt(i));
			}
		}

		@Override
		public void write(@Nonnull String str, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				write(str.charAt(i));
			}
		}

		@Override
		public void flush() {
			// No-op
		}

		@Override
		public void close() {
			// No-op
		}
	}
}
