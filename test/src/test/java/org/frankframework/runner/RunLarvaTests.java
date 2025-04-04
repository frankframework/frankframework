package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;

@Tag("slow")
@Tag("integration")
public class RunLarvaTests {

	public static final LarvaLogLevel LARVA_LOG_LEVEL = LarvaLogLevel.STEP_PASSED_FAILED;

	private static SpringApplication springApplication;
	private static ConfigurableApplicationContext applicationContext;
	private static IbisContext ibisContext;
	private static LarvaTool larvaTool;
	private static TestConfig testConfig;
	private static ScenarioRunner scenarioRunner;
	private static Message larvaOutput; // TODO: kill?
	private static AppConstants appConstants;
	private static String scenarioRootDir;
	private static Writer output;

	@BeforeAll
	static void setup() throws IOException {
		springApplication = IafTestInitializer.configureApplication();
		applicationContext = springApplication.run();
		ServletContext servletContext = applicationContext.getBean(ServletContext.class);
		ibisContext = FrankApplicationInitializer.getIbisContext(servletContext);
		appConstants = AppConstants.getInstance();

		output = new StringWriter();

		larvaTool = new LarvaTool();
		testConfig = larvaTool.getConfig();
		testConfig.setTimeout(2000);
		testConfig.setSilent(false);
		testConfig.setLogLevel(LARVA_LOG_LEVEL);
		testConfig.setAutoScroll(false);
		testConfig.setMultiThreaded(false);
		testConfig.setSilentOut(output);
		testConfig.setOut(new HtmlTagStrippingWriter(new OutputStreamWriter(System.out)));

		scenarioRunner = new ScenarioRunner(larvaTool, ibisContext, testConfig, appConstants, 100, LARVA_LOG_LEVEL);
		scenarioRootDir = larvaTool.initScenariosRootDirectories(null, new ArrayList<>(), new ArrayList<>());
	}

	@AfterAll
	static void tearDown() {
		CloseUtils.closeSilently(larvaOutput, applicationContext, output);
	}

	@TestFactory
	List<DynamicTest> larvaTests() {
		List<File> allScenarioFiles = larvaTool.readScenarioFiles(appConstants, scenarioRootDir);

		return allScenarioFiles.stream()
				.map(this::convertLarvaScenarioToTest)
				.toList();
	}

	private DynamicTest convertLarvaScenarioToTest(File scenarioFile) {
		String scenarioName = scenarioFile.getAbsolutePath().substring(scenarioRootDir.length());
		return DynamicTest.dynamicTest(
				scenarioName, scenarioFile.toURI(), () -> {
					int scenarioPassed = scenarioRunner.runOneFile(scenarioFile, scenarioRootDir, false);

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

		LarvaTool larvaTool = new LarvaTool();
		String scenarioRootDir = larvaTool.initScenariosRootDirectories(null, new ArrayList<>(), new ArrayList<>());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("execute", scenarioRootDir);
		request.setParameter("loglevel", LARVA_LOG_LEVEL.getName());

		// Invoke Larva tests
		MessageBuilder messageBuilder = new MessageBuilder();
		Writer writer = messageBuilder.asWriter();
		System.err.println("Starting Scenarios");
		long start = System.currentTimeMillis();
		int result = LarvaTool.runScenarios(servletContext, request, writer);
		long end = System.currentTimeMillis();
		System.err.println("Scenarios executed; duration: " + (end - start) + "ms");
		writer.close();
		larvaOutput = messageBuilder.build();

		assertFalse(result < 0, () -> "Error in LarvaTool execution, result is [" + result + "] instead of 0; output from LarvaTool:\n\n" + messageAsStringStripped(larvaOutput));

		if (result > 0) {
			System.err.println(result + " Larva tests failed duration: " + (end - start) + "ms; \n\n" + messageAsStringStripped(larvaOutput));
		} else {
			System.err.println("All Larva tests succeeded in " + (end - start) + "ms");
		}
	}

	private @Nonnull String messageAsStringStripped(Message message) {
		try {
			String result = message.asString();
			if (result == null) {
				return "";
			}
			return result
					.replaceAll("(?s)<form.*?</form>\n*", "")
					.replaceAll("<div class='(odd|even)'></div>\n", "")
					.replaceAll("</div>\n", "")
					;
		} catch (IOException e) {
			return "Cannot read string from message: " + e.getMessage();
		}
	}

	static class HtmlTagStrippingWriter extends FilterWriter {

		private boolean writingTag = false;
		private boolean lastCharWasNewLine = false;
		private boolean writingHtmlEntity = false;
		private StringBuffer htmlEntityBuffer = new StringBuffer();

		/**
		 * Create a new filtered writer.
		 *
		 * @param out a Writer object to provide the underlying stream.
		 * @throws NullPointerException if {@code out} is {@code null}
		 */
		protected HtmlTagStrippingWriter(Writer out) {
			super(out);
		}

		@Override
		public void write(int c) throws IOException {
			if (c == '<') {
				writingTag = true;
				lastCharWasNewLine = false;
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
				if (lastCharWasNewLine && isNewLine) {
					// skip, don't print repeated newlines
				} else {
					super.write(c);
				}
				lastCharWasNewLine = isNewLine;
			}
		}

		private void writeHtmlEntity() throws IOException {
			String entity = StringEscapeUtils.unescapeHtml4(htmlEntityBuffer.toString());
			super.write(entity, 0, entity.length());
			htmlEntityBuffer.setLength(0);
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				write(cbuf[i]);
			}
		}

		@Override
		public void write(String str, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				write(str.charAt(i));
			}
		}
	}
}
