/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.filesystem;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.receivers.RawMessageWrapper;

public class FileSystemMessageBrowsingIteratorItem<F, FS extends IBasicFileSystem<F>> implements IMessageBrowsingIteratorItem {

	private final FS fileSystem;
	private final RawMessageWrapper<F> item;
	private final String messageIdPropertyKey;
	private final String comment;

	public FileSystemMessageBrowsingIteratorItem(FS fileSystem, RawMessageWrapper<F> item, String messageIdPropertyKey) throws FileSystemException {
		this.fileSystem = fileSystem;
		this.item = item;
		this.messageIdPropertyKey = messageIdPropertyKey;
		if (fileSystem instanceof ISupportsCustomFileAttributes<?>) {
			@SuppressWarnings("unchecked")
			ISupportsCustomFileAttributes<F> supportsCustomFileAttributes = (ISupportsCustomFileAttributes<F>) fileSystem;
			comment = supportsCustomFileAttributes.getCustomFileAttribute(item.getRawMessage(), "comment");
		} else {
			comment = null;
		}
	}

	@Override
	public String getId() throws ListenerException {
		return fileSystem.getName(item.getRawMessage());
	}

	@Override
	public String getOriginalId() throws ListenerException {
		try {
			if (StringUtils.isNotEmpty(messageIdPropertyKey)) {
				Map<String,Object> properties = fileSystem.getAdditionalFileProperties(item.getRawMessage());
				return (String)properties.get(messageIdPropertyKey);
			}
			return fileSystem.getName(item.getRawMessage());
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public String getCorrelationId() {
		return null;
	}

	@Override
	public Date getInsertDate() throws ListenerException {
		try {
			return fileSystem.getModificationTime(item.getRawMessage());
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public Date getExpiryDate() {
		return null;
	}

	@Override
	public String getType() {
		return null;
	}

	@Override
	public String getHost() {
		return null;
	}

	@Override
	public String getCommentString() throws ListenerException {
		return comment;
	}

	@Override
	public String getLabel() {
		return null;
	}

	@Override
	public void close() {
		// nothing special in this case
	}

}
