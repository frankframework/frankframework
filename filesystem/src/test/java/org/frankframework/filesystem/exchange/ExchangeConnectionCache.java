package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.MsalClientAdapter;
import org.frankframework.filesystem.MsalClientAdapter.GraphClient;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.senders.ExchangeFileSystemSender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.PropertyLoader;

@Log4j2
public class ExchangeConnectionCache {

	private static ExchangeConnectionCache EXCHANGE_CONNECTION_CACHE;

	private String mailAddress;
	private String clientId;
	private String clientSecret;
	private String tenantId;
	private String baseFolder;

	private GraphClient graphClient;
	private ExchangeFileSystemTestHelper exchangeFileSystemHelper;
	private TestConfiguration configuration;

	public ExchangeConnectionCache() {
		try {
			PropertyLoader properties = new PropertyLoader("azure-credentials.properties");

			mailAddress = properties.getProperty("mailAddress");
			clientId = properties.getProperty("clientId");
			clientSecret = properties.getProperty("clientSecret");
			tenantId = properties.getProperty("tenantId");

			// Should ideally never be `inbox` as it removes all mail items!
			baseFolder = properties.getProperty("baseFolder", ExchangeFileSystemTestHelper.DEFAULT_BASE_FOLDER);
		} catch (Exception e) {
			// file not found
		}

		if (!hasAllCredentials()) {
			log.warn("not all credentials for exchange are available, skipping tests");
			return;
		}

		init();
	}

	private void init() {
		try {
			log.debug("Creating new GraphClient and FS test helper");
			configuration = new TestConfiguration();
			MsalClientAdapter adapter = configuration.createBean();
			adapter.setTimeout(30_000);
			adapter.configure();
			adapter.start();

			CredentialFactory credentials = new CredentialFactory(null, clientId, clientSecret);
			graphClient = adapter.createGraphClient(tenantId, credentials);

			exchangeFileSystemHelper = createHelper();
		} catch (Exception e) {
			log.fatal("unable to create GraphClient", e);
			fail("unable to create GraphClient");
		}
	}

	private void doClose() {
		log.debug("running after-all cleanup");
		if (exchangeFileSystemHelper != null) {
			exchangeFileSystemHelper.afterAllCleanup();
		}
		CloseUtils.closeSilently(configuration, graphClient);
	}

	private static ExchangeConnectionCache getInstance() {
		if (EXCHANGE_CONNECTION_CACHE == null) {
			EXCHANGE_CONNECTION_CACHE = new ExchangeConnectionCache();
		}

		return EXCHANGE_CONNECTION_CACHE;
	}

	private boolean hasAllCredentials() {
		return StringUtils.isNoneEmpty(mailAddress, clientId, clientSecret, tenantId);
	}

	private ExchangeFileSystemTestHelper createHelper() {
		return new ExchangeFileSystemTestHelper(clientId, clientSecret, tenantId, mailAddress, baseFolder);
	}

	private ExchangeFileSystem createFileSystem() {
		ExchangeFileSystem exchange = spy(ExchangeFileSystem.class);

		try {
			doReturn(EXCHANGE_CONNECTION_CACHE.graphClient).when(exchange).getGraphClient();
		} catch (IOException e) {
			throw new IllegalStateException("unable to create GraphClient", e);
		}

		exchange.setTenantId(tenantId);
		exchange.setMailAddress(mailAddress);
		exchange.setBaseFolder(baseFolder);
		configuration.autowireByName(exchange);

		return exchange;
	}

	private ExchangeMailListener createListener() {
		ExchangeMailListener exchange = spy(ExchangeMailListener.class);
		doReturn(createFileSystem()).when(exchange).getFileSystem();

		exchange.setClientId(clientId);
		exchange.setClientSecret(clientSecret);
		exchange.setTenantId(tenantId);
		exchange.setMailAddress(mailAddress);
		exchange.setBaseFolder(baseFolder);
		configuration.autowireByName(exchange);

		return exchange;
	}

	private ExchangeFileSystemSender createSender() {
		ExchangeFileSystemSender exchange = spy(ExchangeFileSystemSender.class);
		doReturn(createFileSystem()).when(exchange).getFileSystem();

		exchange.setClientId(clientId);
		exchange.setClientSecret(clientSecret);
		exchange.setTenantId(tenantId);
		exchange.setMailAddress(mailAddress);
		exchange.setBaseFolder(baseFolder);
		configuration.autowireByName(exchange);

		return exchange;
	}

	public static boolean validateCredentials() {
		return getInstance().hasAllCredentials();
	}

	public static IFileSystemTestHelper getExchangeFileSystemTestHelper() {
		assumeTrue(validateCredentials());
		return getInstance().exchangeFileSystemHelper;
	}

	public static ExchangeFileSystem getExchangeFileSystem() {
		assumeTrue(validateCredentials());

		return getInstance().createFileSystem();
	}

	public static ExchangeMailListener getExchangeMailListener() {
		assumeTrue(validateCredentials());

		return getInstance().createListener();
	}

	public static ExchangeFileSystemSender getExchangeFileSystemSender() {
		assumeTrue(validateCredentials());

		return EXCHANGE_CONNECTION_CACHE.createSender();
	}

	public static void close() {
		if (EXCHANGE_CONNECTION_CACHE != null) {
			EXCHANGE_CONNECTION_CACHE.doClose();
		}
		EXCHANGE_CONNECTION_CACHE = null;
	}
}
