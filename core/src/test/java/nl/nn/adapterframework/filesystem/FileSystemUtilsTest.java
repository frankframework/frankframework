package nl.nn.adapterframework.filesystem;

import org.junit.Before;
import org.junit.Test;

public abstract class FileSystemUtilsTest<F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FS fileSystem;

	protected abstract FS createFileSystem();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystem = createFileSystem();
		fileSystem.configure();
		fileSystem.open();
	}
	
	public void testBackupByNumber(String folder) throws Exception {
		String filename = "backupTest" + FILE1;
		String contents = "text content:";
		int numOfBackups=3;
		int numOfFilesPresentAtStart=5;
		
		if (folder!=null && !_folderExists(folder)) {
			_createFolder(folder);
		}

		if (_fileExists(folder, filename)) {
			_deleteFile(folder, filename);
		}

		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			createFile(folder, filename+"."+i,contents+i);
		}
		createFile(folder, filename,contents+"0");
		
		assertFileExistsWithContents(folder, filename, contents.trim()+"0");
		assertFileExistsWithContents(folder, filename+"."+numOfFilesPresentAtStart, contents.trim()+(numOfFilesPresentAtStart));
		
		// execute rollover
		FileSystemUtils.rolloverByNumber(fileSystem, folder, filename, numOfBackups);
		
		// assert that the file has been backed up, and backups have been rotated
		assertFileDoesNotExist(folder, filename);
		for (int i=1;i<=numOfBackups;i++) {
			assertFileExistsWithContents(folder, filename+"."+i, contents.trim()+(i-1));
		}
		assertFileDoesNotExist(folder, filename+(numOfBackups+1));
		assertFileDoesNotExist(folder, filename+(numOfBackups+2));
	}

	@Test
	public void testBackupByNumberInRoot() throws Exception {
		testBackupByNumber(null);
	}
	@Test
	public void testBackupByNumberInFolder() throws Exception {
		testBackupByNumber("folder");
	}
	
	public void testRolloverBySize(String folder) throws Exception {
		String filename = "rolloverBySize" + FILE1;
		String contents = "-abcd-";
		int numOfBackups = 3;
		int rotateSize = 8;
		int numOfFilesPresentAtStart=5;
		
		if (_fileExists(filename)) {
			_deleteFile(folder, filename);
		}

		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			createFile(folder, filename+"."+i,contents+i);
		}
		createFile(folder, filename,contents+"0");
		
		// test for rollover for the small file, it should do nothing now
		FileSystemUtils.rolloverBySize(fileSystem, fileSystem.toFile(folder, filename), folder, rotateSize, numOfBackups);

		// assert that nothing has changed yet, because the file is smaller than the rotate size.
		assertFileExistsWithContents(folder, filename, contents.trim()+"0");
		assertFileExistsWithContents(folder, filename+"."+numOfFilesPresentAtStart, contents.trim()+(numOfFilesPresentAtStart));
		
		
		// create a bigger file
		_deleteFile(folder, filename);
		createFile(folder, filename,contents+contents+"0");
		
		// test rollover for bigger file
		FileSystemUtils.rolloverBySize(fileSystem, fileSystem.toFile(folder, filename), folder, rotateSize, numOfBackups);
		
		// assert that the file has been backed up, and backups have been rotated
		assertFileDoesNotExist(folder, filename);
		assertFileExistsWithContents(folder, filename+".1", contents+contents+"0");
		for (int i=2;i<=numOfBackups;i++) {
			assertFileExistsWithContents(folder, filename+"."+i, contents.trim()+(i-1));
		}
		assertFileDoesNotExist(folder, filename+(numOfBackups+1));
		assertFileDoesNotExist(folder, filename+(numOfBackups+2));
	}

	@Test
	public void testRolloverBySizeInRoot() throws Exception {
		testRolloverBySize(null);
	}
	
	@Test
	public void testRolloverBySizeInFolder() throws Exception {
		testRolloverBySize("folder");
	}
	
	
	@Test
	public void testMoveWithBackup() throws Exception {
		String filename = "backupTest" + FILE1;
		String contents = "text content:";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";
		int numOfBackups=3;
		int numOfFilesPresentAtStart=5;
		
		if (_fileExists(filename)) {
			_deleteFile(dstFolder, filename);
		}

		if (dstFolder!=null) {
			if  (_folderExists(dstFolder)) {
				_createFolder(dstFolder);
			}
		}
		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			createFile(dstFolder, filename+"."+i,contents+i);
		}
		createFile(dstFolder, filename,contents+"0");
		createFile(srcFolder, filename,contents+"new");
		
		F file = fileSystem.toFile(srcFolder, filename);
		
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"0");
		assertFileExistsWithContents(dstFolder, filename+"."+numOfFilesPresentAtStart, contents.trim()+(numOfFilesPresentAtStart));
		
		// execute move
		FileSystemUtils.moveFile(fileSystem, file, dstFolder, false, numOfBackups, false);
		
		// assert that the file has been backed up, and backups have been rotated
		assertFileDoesNotExist(srcFolder, filename);
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"new");
		for (int i=1;i<=numOfBackups;i++) {
			assertFileExistsWithContents(dstFolder, filename+"."+i, contents.trim()+(i-1));
		}
		assertFileDoesNotExist(dstFolder, filename+(numOfBackups+1));
		assertFileDoesNotExist(dstFolder, filename+(numOfBackups+2));
	}

	@Test
	public void testCopyWithBackup() throws Exception {
		String filename = "backupTest" + FILE1;
		String contents = "text content:";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";
		int numOfBackups=3;
		int numOfFilesPresentAtStart=5;
		
		if (_fileExists(filename)) {
			_deleteFile(dstFolder, filename);
		}

		if (dstFolder!=null) {
			if  (_folderExists(dstFolder)) {
				_createFolder(dstFolder);
			}
		}
		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			createFile(dstFolder, filename+"."+i,contents+i);
		}
		createFile(dstFolder, filename,contents+"0");
		createFile(srcFolder, filename,contents+"new");
		
		F file = fileSystem.toFile(srcFolder, filename);
		
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"0");
		assertFileExistsWithContents(dstFolder, filename+"."+numOfFilesPresentAtStart, contents.trim()+(numOfFilesPresentAtStart));
		
		// execute move
		FileSystemUtils.copyFile(fileSystem, file, dstFolder, false, numOfBackups, false);
		
		// assert that the file has been backed up, and backups have been rotated
		assertFileExistsWithContents(srcFolder, filename, contents.trim()+"new");
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"new");
		for (int i=1;i<=numOfBackups;i++) {
			assertFileExistsWithContents(dstFolder, filename+"."+i, contents.trim()+(i-1));
		}
		assertFileDoesNotExist(dstFolder, filename+(numOfBackups+1));
		assertFileDoesNotExist(dstFolder, filename+(numOfBackups+2));
	}

	@Test
	public void testMoveWithOverWrite() throws Exception {
		String filename = "backupTest" + FILE1;
		String contents = "text content:";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";
		int numOfBackups=3;
		int numOfFilesPresentAtStart=5;
		
		if (dstFolder!=null && !_folderExists(dstFolder)) {
			_createFolder(dstFolder);
		}

		if (_fileExists(dstFolder, filename)) {
			_deleteFile(dstFolder, filename);
		}

		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			createFile(dstFolder, filename+"."+i,contents+i);
		}
		createFile(dstFolder, filename,contents+"0");
		createFile(srcFolder, filename,contents+"new");
		
		F file = fileSystem.toFile(srcFolder, filename);
		
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"0");
		assertFileExistsWithContents(dstFolder, filename+"."+numOfFilesPresentAtStart, contents.trim()+(numOfFilesPresentAtStart));
		
		// execute move
		FileSystemUtils.moveFile(fileSystem, file, dstFolder, true, numOfBackups, false);
		
		// assert that the file has been overwritten, and no backups have been rotated
		assertFileDoesNotExist(srcFolder, filename);
		assertFileExistsWithContents(dstFolder, filename, contents.trim()+"new");
		for (int i=1;i<=numOfFilesPresentAtStart;i++) {
			assertFileExistsWithContents(dstFolder, filename+"."+i, contents.trim()+i);
		}
	}

}
