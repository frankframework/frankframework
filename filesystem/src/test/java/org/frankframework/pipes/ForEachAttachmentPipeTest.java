package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.ForEachAttachmentPipe;

public class ForEachAttachmentPipeTest {


	/**
	 * Method: addItemsToList(Collection<String> collection, String list, String
	 * collectionDescription, boolean lowercase)
	 */
	@Test
	public void testOnlyPropertiesToList() {
		String list = "a,b,C";
		ForEachAttachmentPipe pipe = new ForEachAttachmentPipe();
		pipe.setOnlyProperties(list);

		List<String> collection = new ArrayList<>(pipe.getOnlyPropertiesSet());
		assertThat(collection, containsInAnyOrder("a", "b", "C"));
	}

	@Test
	public void testExcludePropertiesToList() {
		String list = "a,b,C,";
		ForEachAttachmentPipe pipe = new ForEachAttachmentPipe();
		pipe.setExcludeProperties(list);

		List<String> collection = new ArrayList<>(pipe.getExcludePropertiesSet());
		assertThat(collection, containsInAnyOrder("a", "b", "C", ""));
	}
}
