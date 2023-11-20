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
package nl.nn.adapterframework.management.bus.dto;

import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageBrowsingFilter;

public class StorageItemsDTO {

	private final Logger log = LogUtil.getLogger(this);
	private final @Getter int totalMessages;
	private final @Getter int skipMessages;
	private final @Getter int messageCount;
	private final @Getter int recordsFiltered;
	private final @Getter List<StorageItemDTO> messages;

	@JsonInclude(Include.NON_EMPTY)
	private @Getter @Setter Map<ProcessState, ProcessStateDTO> targetStates = new EnumMap<>(ProcessState.class);

	public StorageItemsDTO(IMessageBrowser<?> transactionalStorage, MessageBrowsingFilter filter) {
		int total;
		try {
			total = transactionalStorage.getMessageCount();
		} catch (Exception e) {
			log.warn("unable to get messagecount from storage", e);
			total = -1;
		}

		this.totalMessages = total;
		this.skipMessages = filter.getSkipMessages();
		this.messageCount = (total - skipMessages);

		Date startDate = filter.getStartDate();
		Date endDate = filter.getEndDate();
		try (IMessageBrowsingIterator iterator = transactionalStorage.getIterator(startDate, endDate, filter.getSortOrder())) {
			int count;
			List<StorageItemDTO> messages = new LinkedList<>();

			for (count=0; iterator.hasNext(); ) {
				try (IMessageBrowsingIteratorItem iterItem = iterator.next()) {
					if(!filter.matchAny(iterItem))
						continue;

					count++;
					if (count > filter.getSkipMessages() && messages.size() < filter.getMaxMessages()) {
						StorageItemDTO dto = new StorageItemDTO(iterItem);
						dto.setPosition(count);
						messages.add(dto);
					}
				}
			}

			recordsFiltered = count;
			this.messages = messages;
		} catch (ListenerException | IOException e) {
			throw new BusException("unable to list storage messages", e);
		}
	}
}
