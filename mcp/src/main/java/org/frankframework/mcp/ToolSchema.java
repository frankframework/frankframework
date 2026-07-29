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
package org.frankframework.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small fluent builder for a JSON-schema describing an MCP tool's arguments. It produces the plain {@code Map}
 * representation the MCP SDK expects, which keeps the tool definitions readable and avoids hand-written JSON.
 */
public final class ToolSchema {

	private final Map<String, Object> properties = new LinkedHashMap<>();
	private final List<String> required = new ArrayList<>();

	private ToolSchema() {
		// use object()
	}

	public static ToolSchema object() {
		return new ToolSchema();
	}

	public ToolSchema string(String name, String description) {
		return add(name, "string", description, false);
	}

	public ToolSchema requiredString(String name, String description) {
		return add(name, "string", description, true);
	}

	public ToolSchema bool(String name, String description) {
		return add(name, "boolean", description, false);
	}

	public ToolSchema integer(String name, String description) {
		return add(name, "integer", description, false);
	}

	private ToolSchema add(String name, String type, String description, boolean isRequired) {
		properties.put(name, Map.of("type", type, "description", description));
		if (isRequired) {
			required.add(name);
		}
		return this;
	}

	public Map<String, Object> build() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		return schema;
	}
}
