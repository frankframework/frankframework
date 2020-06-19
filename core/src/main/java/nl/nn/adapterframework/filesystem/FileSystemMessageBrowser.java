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
package nl.nn.adapterframework.filesystem;

import java.util.Date;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.LogUtil;

public class FileSystemMessageBrowser<F, FS extends IBasicFileSystem<F>> implements IMessageBrowser<F> {
	protected Logger log = LogUtil.getLogger(this);

	private FS fileSystem;
	private String folder;

	private String hideRegex = null;
	private String hideMethod = "all";


	public FileSystemMessageBrowser(FS fileSystem, String folder) {
		this.fileSystem = fileSystem;
		this.folder = folder;
	}
	
	@Override
	public boolean isTransacted() {
		return false;
	}

	@Override
	public IMessageBrowsingIterator getIterator() throws ListenerException {
		try {
			return new FileSystemMessageBrowsingIterator<F, FS>(fileSystem, folder);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException {
		return getIterator(); // TODO: implement filter and sort order
	}

	@Override
	public IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException {
		return new FileSystemMessageBrowsingIteratorItem<F, FS>(fileSystem, browseMessage(messageId));
	}

	@Override
	public boolean containsMessageId(String originalMessageId) throws ListenerException {
		try {
			return fileSystem.exists(fileSystem.toFile(folder, originalMessageId));
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public boolean containsCorrelationId(String correlationId) throws ListenerException {
		return false;
	}

	@Override
	public F browseMessage(String messageId) throws ListenerException {
		try {
			return fileSystem.toFile(folder, messageId);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void deleteMessage(String messageId) throws ListenerException {
		try {
			F file = fileSystem.toFile(folder, messageId);
			fileSystem.deleteFile(file);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public int getMessageCount() throws ListenerException {
		try {
			int count = 0;
			for (Iterator<F> it = fileSystem.listFiles(folder); it.hasNext(); it.next()) {
				count++;
			}
			return count;
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	@Override
	public String getHideRegex() {
		return hideRegex;
	}

	@Override
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}
	@Override
	public String getHideMethod() {
		return hideMethod;
	}

}
