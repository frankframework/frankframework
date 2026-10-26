package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Calendar;

import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.junit.jupiter.api.Test;

import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;

class CmisUtilsTest {

	@Test
	void processProperties() throws DomBuilderException {
		String cmisInput = """
				<cmis>
					<objectTypeId>Zoo</objectTypeId>
					<fileName>Penguins.jpg</fileName>
					<properties>
						<property name="almostIsoFormat" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss">2026-10-22T16:04:11.727+02:00</property>
					</properties>
				</cmis>""";
		Properties cmisProps = CmisUtils.processProperties(XmlUtils.buildElement(cmisInput));
		PropertyData<?> propertyData = cmisProps.getProperties().get("almostIsoFormat");
		assertNotNull(propertyData);

		Calendar value = assertInstanceOf(Calendar.class, propertyData.getFirstValue());
		assertEquals(1792677851727L, value.toInstant().toEpochMilli());
	}
}
