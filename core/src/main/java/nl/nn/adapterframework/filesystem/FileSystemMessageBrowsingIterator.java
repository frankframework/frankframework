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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;

public class FileSystemMessageBrowsingIterator<F, FS extends IBasicFileSystem<F>> implements IMessageBrowsingIterator {

	private FS fileSystem;
	private DirectoryStream<F> directoryStream;
	private Iterator<F> iterator;
	private String messageIdPropertyKey;

	public FileSystemMessageBrowsingIterator(FS fileSystem, String folder, String messageIdPropertyKey) throws FileSystemException {
		this.fileSystem = fileSystem;
		directoryStream = fileSystem.listFiles(folder);
		iterator = directoryStream.iterator();
		this.messageIdPropertyKey = messageIdPropertyKey;
	}
	
	@Override
	public boolean hasNext() throws ListenerException {
		return iterator !=null && iterator.hasNext();
	}

	@Override
	public IMessageBrowsingIteratorItem next() throws ListenerException {
		return new FileSystemMessageBrowsingIteratorItem<F, FS>(fileSystem, iterator.next(), messageIdPropertyKey);
	}

	@Override
	public void close() throws ListenerException {
		try {
			directoryStream.close();
		} catch (IOException e) {
			throw new ListenerException(e);
		}
	}

}
