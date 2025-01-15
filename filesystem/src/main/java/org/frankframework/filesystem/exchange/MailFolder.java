/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.filesystem.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailFolder extends MailItemId {

	@JsonProperty("displayName")
	private String name;

	private int childFolderCount;
	private int unreadItemCount;
	private int totalItemCount;
	private int sizeInBytes;

	@Override
	public String toString() {
		return "MailFolder [%s] with id [%s]".formatted(name, getId());
	}
}
