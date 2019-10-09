package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;

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
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not yet stable for "+(2*stabilityTimeUnit)+"ms",rawMessage);

		// simulate that the file is older
		setFileDate(null, filename, new Date(new Date().getTime()-3*stabilityTimeUnit));
		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when stable for "+(3*stabilityTimeUnit)+"ms",rawMessage);
	}

}
