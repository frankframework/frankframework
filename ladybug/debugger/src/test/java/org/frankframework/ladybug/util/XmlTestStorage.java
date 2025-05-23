package org.frankframework.ladybug.util;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.xml.XmlStorage;

import java.io.File;

public class XmlTestStorage extends XmlStorage {
	// Change protected to public for JUnit tests
	public Report readReportFromFile(File file) throws StorageException {
		return super.readReportFromFile(file, this);
	}
}
