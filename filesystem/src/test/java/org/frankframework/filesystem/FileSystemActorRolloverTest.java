package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.TimeProvider;

public abstract class FileSystemActorRolloverTest<F,S extends IWritableFileSystem<F>> extends FileSystemActorCustomFileAttributesTest<F, S> {

	@Override
	protected abstract IFileSystemTestHelperFullControl getFileSystemTestHelper();

	private void setFileDate(String folder, String filename, Date date) throws Exception {
		((IFileSystemTestHelperFullControl)helper).setFileDate(folder, filename, date);
	}

	@Test
	public void fileSystemActorAppendActionWithDailyRollover() throws Exception {
		String filename = "rolloverDaily" + FILE1;
		String contents = "thanos car ";
		int numOfBackups = 12;
		int numOfWrites = 10;
		Date currentDate = TimeProvider.nowAsDate();
		Date firstDate;
		long millisPerDay = 1000L * 60L * 60L * 24L;

		if(_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, "thanos car ");
		setFileDate(null, filename, firstDate = new Date(currentDate.getTime() - (millisPerDay * numOfWrites)));

		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();

		params.add(ParameterBuilder.create().withName("contents").withSessionKey("appendActionwString"));
		params.configure();

		actor.setAction(FileSystemAction.APPEND);
		actor.setRotateDays(numOfBackups);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		for(int i=0; i<numOfWrites; i++) {
			setFileDate(null, filename, new Date(firstDate.getTime() + (millisPerDay * i)));

			session.put("appendActionwString", contents+i);
			ParameterValueList pvl = params.getValues(message, session);

			Message result = actor.doAction(message, pvl, session);
			String stringResult = result.asString();

			TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

			result.close();
		}

		for (int i=1; i<=numOfWrites-1; i++) {
			String formattedDate = DateFormatUtils.format(firstDate.getTime() + (millisPerDay * i), DateFormatUtils.ISO_DATE_FORMATTER);

			String actualContentsi = readFile(null, filename+"."+formattedDate);
			assertEquals((contents+(i-1)).trim(), actualContentsi.trim());
		}
	}
}
