package org.frankframework.ladybug.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.StorageException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import org.frankframework.ladybug.util.XmlTestStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GrayBoxTest {
	private static final XmlTestStorage xmlStorage = new XmlTestStorage();
	private static final String[] expectedResult = {"2#0", "2#24", "2#36", "2#43"};

	@Test
	public void shouldReturnCorrectCheckpointTest() throws StorageException {
		File reportFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("testReport/Pipeline_HelloWorlds.report.xml")).getFile());
		Report report = xmlStorage.readReportFromFile(reportFile);
		GrayBox grayBox = new GrayBox();
		List<Checkpoint> checkpointsAfterFilter = new ArrayList<>();
		for (Checkpoint checkpoint : report.getCheckpoints()) {
			if (grayBox.match(report, checkpoint)) {
				checkpointsAfterFilter.add(checkpoint);
			}
		}
		assertEquals(expectedResult.length, checkpointsAfterFilter.size());
		for (int index = 0; index < checkpointsAfterFilter.size(); index++) {
			assertEquals(expectedResult[index], checkpointsAfterFilter.get(index).getUid());
		}
	}
}
