package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

public abstract class BasicFileSystemTestBase<F, FS extends IBasicFileSystem<F>> extends ConfiguredTestBase {

	protected FS fileSystem;

	/**
	 * Returns the file system
	 * @return fileSystem
	 * @throws ConfigurationException
	 */
	protected abstract FS createFileSystem() throws ConfigurationException;


	@Override
	@BeforeEach
	public void setUp() throws IOException, ConfigurationException, FileSystemException {
		log.debug("setUp start");
		fileSystem = createFileSystem();
		autowireByName(fileSystem);
		log.debug("filesystem created, configure()");
		fileSystem.configure();
		log.debug("filesystem configured, open()");
		fileSystem.open();
		log.debug("setUp finished");
	}

	@Override
	@AfterEach
	public void tearDown() {
		log.debug("tearDown start");
		try {
			fileSystem.close();
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
		log.debug("tearDown finished");
		super.tearDown();
	}

	@Test
	public void fileSystemTestConfigureAndOpen() {
		// just perform the setup()
	}

	protected F getFirstFileFromFolder(String folder) throws Exception {
		try (DirectoryStream<F> ds = fileSystem.list(folder, TypeFilter.FILES_ONLY)) {
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
		Set<F> files = new HashSet<>();
		Set<String> filenames = new HashSet<>();
		int count = 0;
		try(DirectoryStream<F> ds = fileSystem.list(folder, TypeFilter.FILES_ONLY)) {
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

			assertEquals(numFilesExpected, count, "number of files found by listFiles()");
			assertEquals(numFilesExpected, files.size(), "Size of set of files");
			assertEquals(numFilesExpected, filenames.size(), "Size of set of filenames");

			for (String filename:filenames) {
				F f=fileSystem.toFile(folder, filename);
				assertNotNull(f, "file must be found by filename ["+filename+"]");
				assertTrue(fileSystem.exists(f), "file must exist when referred to by filename ["+filename+"]");
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
				log.debug("modificationTime ["+(modificationTime==null?null: DateFormatUtils.format(modificationTime))+"]");

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
		assertFalse(fileSystem.exists(f), "RandomFileShouldNotExist");
	}

}
