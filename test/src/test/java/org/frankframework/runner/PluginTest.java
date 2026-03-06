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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.ResponseUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SpringUtils;

@Tag("slow")
class PluginTest {

	@SuppressWarnings({ "NullAway.Init", "java:S2637" })
	private static ConfigurableApplicationContext applicationContext = null;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		Path projectDir = FrankInitializer.getProjectDir();
		Path pluginDirectory = projectDir.resolve("../core/src/test/resources/Plugins/").normalize().toAbsolutePath();
		assertTrue(Files.exists(pluginDirectory));

		System.setProperty("plugins.directory", pluginDirectory.toString());
		System.setProperty("plugins.enabled", "true");

		Path configurationDir = projectDir.resolve("src/main/configurations/").toAbsolutePath();
		System.setProperty("configurations.directory", configurationDir.toString());

		System.setProperty("configurations.names", "CompositeComponents");
		System.setProperty("configurations.CompositeComponents.classLoaderType", "DirectoryClassLoader");
		System.setProperty("configurations.CompositeComponents.configurationFile", "Configuration.xml");
		System.setProperty("configurations.CompositeComponents.basePath", "CompositeComponents");

		SpringApplication springApplication = FrankInitializer.createSpringApplication();

		applicationContext = springApplication.run();

		assertTrue(applicationContext.isRunning());

		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(() -> verifyAppIsHealthy());
	}

	@AfterAll
	static void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}

		// Make sure to clear the app constants as well
		AppConstants.removeInstance();
	}

	@Test
	void testPluginExecution() {
		OutboundGateway gateway = SpringUtils.createBean(applicationContext, LocalGateway.class);

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

	private static boolean verifyAppIsHealthy() {
		OutboundGateway gateway = SpringUtils.createBean(applicationContext, LocalGateway.class);
		return verifyAppIsHealthy(gateway);
	}

	private static boolean verifyAppIsHealthy(OutboundGateway gateway) {
		try {
			Message<Object> response = gateway.sendSyncMessage(RequestMessageBuilder.create(BusTopic.HEALTH).build(null));
			return "200".equals(response.getHeaders().get(BusMessageUtils.HEADER_PREFIX+MessageBase.STATUS_KEY));
		} catch (Exception e) {
			return false;
		}
	}
}
