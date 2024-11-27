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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.receivers.RawMessageWrapper;

public class FileSystemMessageBrowsingIterator<F, FS extends IBasicFileSystem<F>> implements IMessageBrowsingIterator {

	private final FS fileSystem;
	private final DirectoryStream<F> directoryStream;
	private final Iterator<F> iterator;
	private final String messageIdPropertyKey;

	public FileSystemMessageBrowsingIterator(FS fileSystem, String folder, String messageIdPropertyKey) throws FileSystemException {
		this.fileSystem = fileSystem;
		directoryStream = fileSystem.list(folder, TypeFilter.FILES_ONLY);
		iterator = directoryStream != null ? directoryStream.iterator() : null;
		this.messageIdPropertyKey = messageIdPropertyKey;
	}

	@Override
	public boolean hasNext() throws ListenerException {
		return iterator !=null && iterator.hasNext();
	}

	@Override
	public IMessageBrowsingIteratorItem next() throws ListenerException {
		return new FileSystemMessageBrowsingIteratorItem<>(fileSystem, new RawMessageWrapper<>(iterator.next()), messageIdPropertyKey);
	}

	@Override
	public void close() throws ListenerException {
		try {
			if (directoryStream!=null) {
				directoryStream.close();
			}
		} catch (IOException e) {
			throw new ListenerException(e);
		}
	}

}
