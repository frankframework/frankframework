/*
   Copyright 2022 WeAreFrank!

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;

import org.frankframework.core.ProcessState;

public class ProcessStateDTO {
	private final @Getter String name;

	@JsonInclude(Include.NON_NULL)
	private @Getter Object numberOfMessages;

	public ProcessStateDTO(ProcessState ps) {
		this.name = ps.getName();
	}

	public void setMessageCount(Object messageCount) {
		this.numberOfMessages = messageCount;
	}
}
