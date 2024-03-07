/*
   Copyright 2019-2023 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import org.frankframework.receivers.RawMessageWrapper;
import org.junit.jupiter.api.Test;

public abstract class FileSystemListenerExtraTest<F,FS extends IWritableFileSystem<F>> extends FileSystemListenerTest<F, FS> {

	@Override
	protected abstract IFileSystemTestHelperFullControl getFileSystemTestHelper();

	private void setFileDate(String folder, String filename, Date date) throws Exception {
		((IFileSystemTestHelperFullControl)helper).setFileDate(folder, filename, date);
	}

	@Test
	public void fileListenerTestGetRawMessageDelayed() throws Exception {
		int stabilityTimeUnit=1000; // ms
		fileSystemListener.setMinStableTime(2*stabilityTimeUnit);
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);

		// file just created, assume that stability time has not yet passed
		RawMessageWrapper<F> rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not yet stable for "+(2*stabilityTimeUnit)+"ms");

		// simulate that the file is older
		setFileDate(null, filename, new Date(new Date().getTime()-3*stabilityTimeUnit));
		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when stable for "+(3*stabilityTimeUnit)+"ms");
	}

}
