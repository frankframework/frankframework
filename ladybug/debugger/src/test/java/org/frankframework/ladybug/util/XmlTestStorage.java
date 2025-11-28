package org.frankframework.ladybug.util;

import java.io.File;

import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.storage.xml.XmlStorage;

public class XmlTestStorage extends XmlStorage {
	// Change protected to public for JUnit tests
	public Report readReportFromFile(File file) throws StorageException {
		return super.readReportFromFile(file, this);
	}
}
