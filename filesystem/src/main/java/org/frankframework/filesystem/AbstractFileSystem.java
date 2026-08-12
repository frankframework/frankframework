/*
   Copyright 2020-2025 WeAreFrank!

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


import org.frankframework.core.DestinationType;

/**
 * Baseclass for {@link IBasicFileSystem FileSystems}.
 *
 * @author Gerrit van Brakel
 */
@DestinationType(DestinationType.Type.FILE_SYSTEM)
public abstract class AbstractFileSystem<F> implements IBasicFileSystem<F> {
	private int maxNumberOfMessagesToList=-1;

	private boolean open;

	@Override
	public void open() throws FileSystemException {
		open = true;
	}
	@Override
	public void close() throws FileSystemException {
		open = false;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	/**
	 * The maximum number of messages to be retrieved from a folder.
	 * @ff.default -1 (unlimited)
	 */
	public void setMaxNumberOfMessagesToList(int maxNumberOfMessagesToList) {
		this.maxNumberOfMessagesToList = maxNumberOfMessagesToList;
	}
	public int getMaxNumberOfMessagesToList() {
		return maxNumberOfMessagesToList;
	}
}
