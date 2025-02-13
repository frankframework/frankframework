package nl.nn.ibistesttool.util;

import java.io.File;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.xml.XmlStorage;

public class XmlTestStorage extends XmlStorage {

	/**
	 * Needed to change the visibility of the constructor to public
	 */
	@Override
	public Report readReportFromFile(File file) throws StorageException {
		return super.readReportFromFile(file);
	}
}
