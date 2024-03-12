package org.frankframework.ibistesttool.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;

import nl.nn.testtool.storage.StorageException;

import org.frankframework.ibistesttool.util.XmlTestStorage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class WhiteBoxTest {
	private static final XmlTestStorage xmlStorage = new XmlTestStorage();
	private static final String[] expectedResult = {"2#0", "2#1", "2#2", "2#3", "2#4", "2#5", "2#6", "2#7", "2#8", "2#9", "2#10",
			"2#11", "2#12", "2#13", "2#14", "2#15", "2#16", "2#17", "2#18", "2#19", "2#20", "2#21", "2#22", "2#23", "2#24",
			"2#25", "2#26", "2#27", "2#28", "2#29", "2#30", "2#31", "2#32", "2#33", "2#34", "2#35", "2#36", "2#37", "2#38",
			"2#39", "2#40", "2#41", "2#42", "2#43"};

	@Test
	public void shouldReturnAllCheckpoints() throws StorageException {
		File reportFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("testReport/Pipeline_HelloWorlds.report.xml")).getFile());
		Report report = xmlStorage.readReportFromFile(reportFile);
		List<Checkpoint> reportCheckpoints = report.getCheckpoints();
		for (int index = 0; index < reportCheckpoints.size(); index++) {
			assertEquals(expectedResult[index], reportCheckpoints.get(index).getUid());
		}
	}
}
