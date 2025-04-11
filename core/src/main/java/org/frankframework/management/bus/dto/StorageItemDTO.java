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

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;

@JsonInclude(Include.NON_NULL)
public class StorageItemDTO {
	private final @Getter String id; //MessageId
	private final @Getter String originalId; //Made up Id?
	private final @Getter String correlationId;
	private final @Getter String type;
	private final @Getter String host;
	private final @Getter Date insertDate;
	private final @Getter Date expiryDate;
	private final @Getter String comment;
	private final @Getter String label;

	// Optional fields (with setters, should only be displayed when !NULL
	private @Getter @Setter Integer position;
	private @Getter @Setter String message;

	public StorageItemDTO(IMessageBrowsingIteratorItem item) throws ListenerException {
		id = item.getId();
		originalId = item.getOriginalId();
		correlationId = item.getCorrelationId();
		type = item.getType();
		host = item.getHost();
		insertDate = item.getInsertDate();
		expiryDate = item.getExpiryDate();
		comment = item.getCommentString();
		label = item.getLabel();
	}
}
