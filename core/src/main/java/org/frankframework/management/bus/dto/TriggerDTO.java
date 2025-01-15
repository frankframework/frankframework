/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.management.bus.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.monitoring.ITrigger.TriggerType;
import org.frankframework.monitoring.Severity;
import org.frankframework.monitoring.SourceFiltering;

public class TriggerDTO {
	private @Getter @Setter List<String> events = null;
	private @Getter @Setter TriggerType type = null;
	private @Getter @Setter Severity severity = null;
	private @Getter @Setter Integer threshold = null;
	private @Getter @Setter Integer period = null;
	private @Getter @Setter SourceFiltering filter = null;
	private @Getter @Setter List<String> adapters = null;
	private @Getter @Setter Map<String, List<String>> sources = null;
}
