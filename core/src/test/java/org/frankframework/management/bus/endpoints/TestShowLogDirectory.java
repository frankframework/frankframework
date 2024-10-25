package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.lifecycle.ShowLogDirectory;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.JsonDirectoryInfoTest;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestShowLogDirectory extends BusTestBase {

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void getLogDirectory() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOGGING, BusAction.GET);

		URL base = JsonDirectoryInfoTest.class.getResource("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(base, "cannot find root directory");
		String logDirectory = Paths.get(base.toURI()).toString();
		request.setHeader("directory", logDirectory);

		ShowLogDirectory showLogDir = new ShowLogDirectory();
		String json = showLogDir.getLogDirectory(request.build()).getPayload();

		String showLogDirectory = TestFileUtils.getTestFile("/Management/showLogDirectory.json");
		MatchUtils.assertJsonEquals(showLogDirectory, applyIgnores(logDirectory, json));
	}

	private String applyIgnores(String base, String message) {
		String normalizedPath = FilenameUtils.normalize(base, true);
		int i = normalizedPath.indexOf("/core/target/");
		String workDir = normalizedPath.substring(0, i);
		return message.replaceAll(workDir, "IGNORE").replaceAll("\\d{8,}", "\"IGNORE\"");
	}
}
