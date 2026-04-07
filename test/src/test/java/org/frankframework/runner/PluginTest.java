package org.frankframework.runner;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import org.frankframework.console.util.ResponseUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.management.bus.message.RequestMessageBuilder;

@Tag("slow")
class PluginTest {

	@SuppressWarnings({ "NullAway.Init", "java:S2637" })
	private static FrankApplication frankApplication = null;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		frankApplication = IafTestInitializer.configureApplication();
		Path projectDir = frankApplication.getProjectDir();

		Path pluginDirectory = projectDir.resolve("src/main/plugins/").normalize().toAbsolutePath();
		assertTrue(Files.exists(pluginDirectory));
		System.setProperty("plugins.directory", pluginDirectory.toString());
		System.setProperty("plugins.enabled", "true");

// TODO Ideally only start the CompositeComponents configuration here...
//		Path configurationDir = projectDir.resolve("src/main/configurations/").toAbsolutePath();
//		System.setProperty("configurations.directory", configurationDir.toString());

		System.setProperty("configurations.CompositeComponents.classLoaderType", "DirectoryClassLoader");
		System.setProperty("configurations.CompositeComponents.configurationFile", "Configuration.xml");
		System.setProperty("configurations.CompositeComponents.basePath", "CompositeComponents");

		frankApplication.run();
		assertTrue(frankApplication.isRunning());

		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(frankApplication::hasStarted);
	}

	@AfterAll
	static void tearDown() {
		System.clearProperty("plugins.directory");
		System.setProperty("plugins.enabled", "false");

		FrankApplication.exit(frankApplication);
	}

	@Test
	void testPluginExecution() {
		LocalGateway gateway = frankApplication.createBean();

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, "CompositeComponents");
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, "testPipelinePartFromPlugin");

		SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(new AnonymousAuthenticationToken("anonymous", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_IbisTester"))));
		securityContextHolderStrategy.setContext(context);

		Message<Object> response = gateway.sendSyncMessage(builder.build(null));

		assertEquals("SUCCESS", BusMessageUtils.getHeader(response, MessageBase.STATE_KEY));
		assertEquals("Niels was here!", ResponseUtils.parseAsString(response));
	}
}
