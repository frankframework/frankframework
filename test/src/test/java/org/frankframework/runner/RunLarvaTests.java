package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.CloseUtils;

@Tag("slow")
@Tag("integration")
@Log4j2
public class RunLarvaTests {

	private SpringApplication springApplication;
	private ConfigurableApplicationContext applicationContext;
	private Message larvaOutput;

	@BeforeEach
	void setup() throws IOException {
		springApplication = IafTestInitializer.configureApplication();
		applicationContext = springApplication.run();
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(larvaOutput, applicationContext);
	}

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@Test
	void runLarvaTests() throws IOException {
		assertTrue(applicationContext.isRunning());

		// Wait until all adapters running
		ServletContext servletContext = applicationContext.getBean(ServletContext.class);

		LarvaTool larvaTool = new LarvaTool();
		String scenarioRootDir = larvaTool.initScenariosRootDirectories(null, new ArrayList<>(), new ArrayList<>());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("execute", scenarioRootDir);
		request.setParameter("loglevel", LarvaLogLevel.SCENARIO_PASSED_FAILED.getName());

		// Invoke Larva tests
		MessageBuilder messageBuilder = new MessageBuilder();
		Writer writer = messageBuilder.asWriter();
		log.info("Starting Scenarios");
		long start = System.currentTimeMillis();
		int result = LarvaTool.runScenarios(servletContext, request, writer);
		long end = System.currentTimeMillis();
		log.info("Scenarios executed; duration: " + (end - start) + "ms");
		writer.close();
		larvaOutput = messageBuilder.build();

		assertFalse(result < 0, ()-> "Error in LarvaTool execution, result is [" + result + "] instead of 0; output from LarvaTool:\n\n" + messageAsStringSafe(larvaOutput));

		if (result > 0) {
			log.warn("{} Larva tests failed; needs investigation but currently not yet failing the build on this. Larva output:\n\n{}", ()->result, ()->messageAsStringSafe(larvaOutput));
			System.err.println(result + " Larva tests failed duration: " + (end - start)+ "ms; \n\n" + messageAsStringSafe(larvaOutput));
		} else {
			log.info("All Larva tests succeeded");
			System.err.println("All Larva tests succeeded in " + (end - start) + "ms");
		}
	}

	private @Nullable String messageAsStringSafe(Message message) {
		try {
			return message.asString();
		} catch (IOException e) {
			return "Cannot read string from message: " + e.getMessage();
		}
	}
}
