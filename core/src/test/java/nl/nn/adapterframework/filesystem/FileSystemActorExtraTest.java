package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.DateUtils;

public abstract class FileSystemActorExtraTest<F,FS extends IWritableFileSystem<F>> extends FileSystemActorTest<F, FS> {

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
		Date currentDate = new Date();
		Date firstDate;
		long millisPerDay = 1000L * 60L * 60L * 24L;
		
		if(_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, "thanos car ");
		setFileDate(null, filename, firstDate = new Date(currentDate.getTime() - (millisPerDay * numOfWrites)));
		
		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();
		
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("appendActionwString");
		params.add(p);
		params.configure();
		
		actor.setAction("append");
		actor.setRotateDays(numOfBackups);
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message = new Message(filename);
		for(int i=0; i<numOfWrites; i++) {
			setFileDate(null, filename, new Date(firstDate.getTime() + (millisPerDay * i)));
			
			session.put("appendActionwString", contents+i);
			ParameterValueList pvl = params.getValues(message, session);
			String result = (String)actor.doAction(message, pvl, null);
			
			TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		}
		
		for (int i=1; i<=numOfWrites-1; i++) {
			String formattedDate = DateUtils.format(new Date(firstDate.getTime() + (millisPerDay * i)), DateUtils.shortIsoFormat);
			
			String actualContentsi = readFile(null, filename+"."+formattedDate);
			assertEquals((contents+(i-1)).trim(), actualContentsi.trim());
		}
	}
}
