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

import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;

public class FileSystemMessageBrowsingIteratorItem<F, FS extends IBasicFileSystem<F>> implements IMessageBrowsingIteratorItem {

	private FS fileSystem;
	private F item;

	public FileSystemMessageBrowsingIteratorItem(FS fileSystem, F item) {
		this.fileSystem = fileSystem;
		this.item = item;
	}
	
	@Override
	public String getId() throws ListenerException {
		return fileSystem.getName(item);
	}

	@Override
	public String getOriginalId() throws ListenerException {
		try {
			return fileSystem.getCanonicalName(item);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public String getCorrelationId() throws ListenerException {
		return null;
	}

	@Override
	public Date getInsertDate() throws ListenerException {
		try {
			return fileSystem.getModificationTime(item);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public Date getExpiryDate() throws ListenerException {
		return null;
	}

	@Override
	public String getType() throws ListenerException {
		return null;
	}

	@Override
	public String getHost() throws ListenerException {
		return null;
	}

	@Override
	public String getCommentString() throws ListenerException {
		return null;
	}

	@Override
	public String getLabel() throws ListenerException {
		return null;
	}

	@Override
	public void release() {
	}

}
