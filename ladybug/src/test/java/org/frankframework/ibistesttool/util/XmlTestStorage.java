package org.frankframework.ibistesttool.util;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.xml.XmlStorage;

import java.io.File;

public class XmlTestStorage extends XmlStorage {
	public Report readReportFromFile(File file) throws StorageException {
		return super.readReportFromFile(file);
	}
}
