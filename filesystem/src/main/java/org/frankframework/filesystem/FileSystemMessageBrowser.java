/*
   Copyright 2020, 2022-2023 WeAreFrank!

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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.core.MessageBrowserField;
import org.frankframework.core.PipeLineSession;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.LogUtil;

public class FileSystemMessageBrowser<F, FS extends IBasicFileSystem<F>> implements IMessageBrowser<F> {
	protected Logger log = LogUtil.getLogger(this);

	private final FS fileSystem;
	private final String folder;
	private final String messageIdPropertyKey;

	private @Getter @Setter String hideRegex = null;
	private @Getter @Setter HideMethod hideMethod = HideMethod.ALL;


	public FileSystemMessageBrowser(FS fileSystem, String folder, String messageIdPropertyKey) {
		this.fileSystem = fileSystem;
		this.folder = folder;
		this.messageIdPropertyKey = messageIdPropertyKey;
	}

	@Override
	public boolean isTransacted() {
		return false;
	}

	@Override
	public IMessageBrowsingIterator getIterator() throws ListenerException {
		try {
			return new FileSystemMessageBrowsingIterator<F, FS>(fileSystem, folder, messageIdPropertyKey);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException {
		return getIterator(); // TODO: implement filter and sort order
	}

	@Override
	public IMessageBrowsingIteratorItem getContext(String storageKey) throws ListenerException {
		try {
			return new FileSystemMessageBrowsingIteratorItem<>(fileSystem, browseMessage(storageKey), messageIdPropertyKey);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	protected boolean contains(String value, ThrowingFunction<IMessageBrowsingIteratorItem,String,ListenerException> field) throws ListenerException {
		try (IMessageBrowsingIterator it=getIterator()){
			while (it.hasNext()) {
				IMessageBrowsingIteratorItem item = it.next();
				if (value.equals(field.apply(item))) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public boolean containsMessageId(String originalMessageId) throws ListenerException {
		return contains(originalMessageId, IMessageBrowsingIteratorItem::getOriginalId);
	}

	@Override
	public boolean containsCorrelationId(String correlationId) throws ListenerException {
		return contains(correlationId, IMessageBrowsingIteratorItem::getCorrelationId); // N.B. getCorrelationId currently always returns null
	}

	@Override
	public RawMessageWrapper<F> browseMessage(String storageKey) throws ListenerException {
		try {
			F file = fileSystem.toFile(folder, storageKey);
			RawMessageWrapper<F> result = new RawMessageWrapper<>(file, storageKey, null);
			result.getContext().put(PipeLineSession.STORAGE_ID_KEY, storageKey);
			return result;
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void deleteMessage(String storageKey) throws ListenerException {
		try {
			F file = fileSystem.toFile(folder, storageKey);
			fileSystem.deleteFile(file);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public int getMessageCount() throws ListenerException {
		try(DirectoryStream<F> ds = fileSystem.list(folder, TypeFilter.FILES_ONLY)) {
			if (ds==null) {
				return -1;
			}
			return (int) StreamSupport.stream(ds.spliterator(), false).count();
		} catch (IOException | FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public List<MessageBrowserField> getStorageFields() {
		return List.of(
			new MessageBrowserField(null, "id", "Storage ID", "string"),
			new MessageBrowserField(null, "originalId", "Original ID", "string"),
			new MessageBrowserField(null, "insertDate", "Timestamp", "date"),
			new MessageBrowserField(null, "comment", "Comment", "string")
		);
	}
}
