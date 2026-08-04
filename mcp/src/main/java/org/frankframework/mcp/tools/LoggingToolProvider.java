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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools to inspect log files and logging settings, mirroring the {@code Logging} and {@code FileViewer} controllers of
 * the Frank!Console. These are the tools to reach for when hunting down errors.
 */
public class LoggingToolProvider extends AbstractToolProvider {

	public LoggingToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				listLogFiles(),
				getLogFile(),
				getLogConfiguration(),
				getLogDefinitions());
	}

	private SyncToolSpecification listLogFiles() {
		return tool("list_log_files",
				"List the available log files (name, size and last modified) in the log directory.",
				ToolSchema.object()
						.string("directory", "an optional sub directory to list instead of the log directory")
						.string("wildcard", "an optional filename wildcard to filter the results, e.g. *.log")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.LOGGING, BusAction.GET);
					builder.addHeader("directory", stringArg(request, "directory"));
					builder.addHeader("wildcard", stringArg(request, "wildcard"));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getLogFile() {
		return tool("get_log_file",
				"Get the contents of a single log file, so errors and stacktraces can be found. Use list_log_files first to find the file name.",
				ToolSchema.object()
						.requiredString("fileName", "the name (or path) of the log file to read, as returned by list_log_files")
						.string("resultType", "the desired result type: plain (default), html or xml")
						.build(),
				request -> {
					String resultType = stringArg(request, "resultType");
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.FILE_VIEWER, BusAction.GET);
					builder.addHeader("fileName", requiredStringArg(request, "fileName"));
					builder.addHeader("resultType", StringUtils.isNotBlank(resultType) ? resultType : "plain");
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getLogConfiguration() {
		return tool("get_log_configuration",
				"Get the current logging configuration (log level, whether intermediary results are logged, max message length).",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.LOG_CONFIGURATION, BusAction.GET)));
	}

	private SyncToolSpecification getLogDefinitions() {
		return tool("get_log_definitions",
				"Get the configured log levels per logger/package.",
				ToolSchema.object()
						.string("filter", "an optional filter on the logger/package name")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.LOG_DEFINITIONS, BusAction.GET);
					builder.addHeader("filter", stringArg(request, "filter"));
					return sendSyncForString(builder);
				});
	}
}
