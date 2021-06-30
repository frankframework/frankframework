package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;

public abstract class BasicFileSystemTestBase<F, FS extends IBasicFileSystem<F>> {
	protected Logger log = LogUtil.getLogger(this);

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	protected FS fileSystem;

	/**
	 * Returns the file system 
	 * @return fileSystem
	 * @throws ConfigurationException
	 */
	protected abstract FS createFileSystem() throws ConfigurationException;


	@Before
	public void setUp() throws IOException, ConfigurationException, FileSystemException {
		log.debug("setUp start");
		fileSystem = createFileSystem();
		log.debug("filesystem created, configure()");
		fileSystem.configure();
		log.debug("filesystem configured, open()");
		fileSystem.open();
		log.debug("setUp finished");
	}
	
	@After 
	public void tearDown() throws Exception {
		log.debug("tearDown start");
		fileSystem.close();
		log.debug("tearDown finished");
	}



	protected void equalsCheck(String content, String actual) {
		assertEquals(content, actual);
	}

	@Test
	public void fileSystemTestConfigureAndOpen() throws Exception {
		// just perform the setup()
	}

	protected F getFirstFileFromFolder(String folder) throws Exception {
		try (DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			Iterator<F> it = ds.iterator();
			if (it == null) {
				return null;
			}
			if (it.hasNext()) {
				return it.next();
			}
			return null;
		}
	}
	
	/**
	 * asserts a number of files to be present in folder.
	 */
	public void fileSystemTestListFile(int numFilesExpected, String folder) throws Exception {
		
		Set<F> files = new HashSet<F>();
		Set<String> filenames = new HashSet<String>();
		int count = 0;
		try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			Iterator<F> it = ds.iterator();
			// Count files
			while (it.hasNext()) {
				F f=it.next();
				String name=fileSystem.getName(f);
				log.debug("found item ["+name+"]");
				files.add(f);
				filenames.add(name);
				count++;
			}

			assertEquals("number of files found by listFiles()", numFilesExpected, count);
			assertEquals("Size of set of files", numFilesExpected, files.size());
			assertEquals("Size of set of filenames", numFilesExpected, filenames.size());
			
			for (String filename:filenames) {
				F f=fileSystem.toFile(folder, filename);
				assertNotNull("file must be found by filename ["+filename+"]",f);
				assertTrue("file must exist when referred to by filename ["+filename+"]",fileSystem.exists(f));
			}
			
			// read each the files
			for(F f: files) {
				Message in=fileSystem.readFile(f, null); 
				log.debug("reading file ["+fileSystem.getName(f)+"]");
				String contentsString= in.asString();
				log.debug("contents ["+contentsString+"]");
				long len=fileSystem.getFileSize(f);
				log.debug("length of contents ["+contentsString.length()+"], reported length ["+len+"]");
				String canonicalname=fileSystem.getCanonicalName(f);
				log.debug("canonicalname ["+canonicalname+"]");
				Date modificationTime=fileSystem.getModificationTime(f);
				log.debug("modificationTime ["+(modificationTime==null?null:DateUtils.format(modificationTime))+"]");
	
				Map<String,Object> properties=fileSystem.getAdditionalFileProperties(f);
				for (Entry<String,Object>entry:properties.entrySet()) {
					String key=entry.getKey();
					Object value=entry.getValue();
					if (value==null) {
						log.debug("property ["+key+"] value is null");
					} else if (value instanceof String){
						log.debug("property ["+key+"] value ["+value+"]");
					} else if (value instanceof List) {
						List list=(List)value;
						if (list.isEmpty()) {
							log.debug("property ["+key+"] value is empty list");
						} else {
							Object valueList=list.get(0);
							for (int i=1;i<list.size();i++) {
								valueList+=", "+list.get(i);
							}
							log.debug("property ["+key+"] value list ["+valueList+"]");
						}	
					} else if (value instanceof Map) {
						Map<Object,Object> map=(Map)value;
						if (map.isEmpty()) {
							log.debug("property ["+key+"] value is empty Map");
						} else {
							for (Entry subentry:map.entrySet()) {
								log.debug("property ["+key+"."+subentry.getKey()+"] value ["+subentry.getValue()+"]");
							}
						}	
					} else if (value instanceof Date) {
						log.debug("property ["+key+"] date value ["+value+"]");
					} else {
						log.debug("property ["+key+"] type ["+value.getClass().getName()+"] value ["+ToStringBuilder.reflectionToString(value)+"]");
					}
				}
			}
		}

	}

	public void fileSystemTestRandomFileShouldNotExist(String randomFileName) throws Exception {
		F f=fileSystem.toFile(randomFileName);
		assertFalse("RandomFileShouldNotExist",fileSystem.exists(f));
	}

}