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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import org.frankframework.mcp.AbstractToolProviderTest;

class TestPipelineToolProviderTest extends AbstractToolProviderTest {

	@Test
	void testPipelineSendsUploadWithPayloadAndReturnsState() {
		// Arrange
		gateway.setResponsePayload("<result>ok</result>");
		gateway.setResponseHeader("state", "SUCCESS");
		SyncToolSpecification tool = findTool(new TestPipelineToolProvider(sender).getTools(), "test_pipeline");

		// Act
		CallToolResult result = call(tool, Map.of(
				"configuration", "myConfig",
				"adapter", "myAdapter",
				"message", "<input>hello</input>"));

		// Assert
		assertFalse(result.isError());
		assertThat(transportHeader("topic"), is("TEST_PIPELINE"));
		assertThat(transportHeader("action"), is("UPLOAD"));
		assertThat(metaHeader("configuration"), is("myConfig"));
		assertThat(metaHeader("adapter"), is("myAdapter"));
		assertThat(gateway.getLastSentMessage().getPayload(), is("<input>hello</input>"));
		assertThat(textOf(result), containsString("SUCCESS"));
		assertThat(textOf(result), containsString("<result>ok</result>"));
	}
}
