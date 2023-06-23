/*
   Copyright 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.LogUtil;

public class FileSystemMessageBrowser<F, FS extends IBasicFileSystem<F>> implements IMessageBrowser<F> {
	protected Logger log = LogUtil.getLogger(this);

	private FS fileSystem;
	private String folder;
	private String messageIdPropertyKey;

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
		return new FileSystemMessageBrowsingIteratorItem<F, FS>(fileSystem, browseMessage(storageKey), messageIdPropertyKey);
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
			result.getContext().put("key", storageKey);
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
		int count = 0;
		try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			if (ds==null) {
				return -1;
			}
			Iterator<F> it = ds.iterator();
			if (it==null) {
				return 0;
			}
			while (it.hasNext()) {
				count++;
				it.next();
			}
		} catch (IOException | FileSystemException e) {
			throw new ListenerException(e);
		}
		return count;
	}

}
