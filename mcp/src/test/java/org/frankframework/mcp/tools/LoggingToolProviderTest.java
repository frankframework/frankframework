/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.mcp.tools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.mcp.AbstractToolProviderTest;

class LoggingToolProviderTest extends AbstractToolProviderTest {

	private List<SyncToolSpecification> tools() {
		return new LoggingToolProvider(sender).getTools();
	}

	@Test
	void listLogFilesSendsLoggingGet() {
		SyncToolSpecification tool = findTool(tools(), "list_log_files");

		call(tool, Map.of("wildcard", "*.log"));

		assertThat(transportHeader("topic"), is("LOGGING"));
		assertThat(transportHeader("action"), is("GET"));
		assertThat(metaHeader("wildcard"), is("*.log"));
	}

	@Test
	void getLogFileSendsFileViewerGetWithDefaultResultType() {
		SyncToolSpecification tool = findTool(tools(), "get_log_file");

		call(tool, Map.of("fileName", "ff.log"));

		assertThat(transportHeader("topic"), is("FILE_VIEWER"));
		assertThat(transportHeader("action"), is("GET"));
		assertThat(metaHeader("fileName"), is("ff.log"));
		assertThat(metaHeader("resultType"), is("plain"));
	}

	@Test
	void getLogConfigurationSendsLogConfigurationGet() {
		SyncToolSpecification tool = findTool(tools(), "get_log_configuration");

		call(tool, Map.of());

		assertThat(transportHeader("topic"), is("LOG_CONFIGURATION"));
		assertThat(transportHeader("action"), is("GET"));
	}
}
