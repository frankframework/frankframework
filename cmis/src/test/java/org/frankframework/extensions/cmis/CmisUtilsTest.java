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
	void processPropertiesDateTime() throws DomBuilderException {
		String cmisInput = """
				<cmis>
					<objectTypeId>Zoo</objectTypeId>
					<fileName>Penguins.jpg</fileName>
					<properties>
						<property name="noT" type="datetime">2026-10-22 16:04:11</property>
						<property name="generic" type="datetime">2026-10-22T16:04:11</property>
						<property name="almostIsoDefaultFormatter" type="datetime">2026-10-22T16:04:11.727+02:00</property>
						<property name="almostIsoCustomFormat" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss">2026-10-22T16:04:11.727+02:00</property>
						<property name="isoFormat" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSxxx">2026-10-22T16:04:11.727+02:00</property>
					</properties>
				</cmis>""";
		Properties cmisProps = CmisUtils.processProperties(XmlUtils.buildElement(cmisInput));

		validatePropertyData(cmisProps, "noT", 1792677851000L);
		validatePropertyData(cmisProps, "generic", 1792677851000L);
		validatePropertyData(cmisProps, "almostIsoDefaultFormatter", 1792677851727L);
		validatePropertyData(cmisProps, "almostIsoCustomFormat", 1792677851727L);
		validatePropertyData(cmisProps, "isoFormat", 1792677851727L);
	}

	private void validatePropertyData(Properties cmisProps, String name, long expectedEpochMilli) {
		PropertyData<?> propertyData = cmisProps.getProperties().get(name);
		assertNotNull(propertyData);
		Calendar value3 = assertInstanceOf(Calendar.class, propertyData.getFirstValue());
		assertEquals(expectedEpochMilli, value3.toInstant().toEpochMilli());
		assertEquals("""
						<property name="%s" type="datetime">2026-10-22T16:04:11</property>"""
				.formatted(name), CmisUtils.getPropertyXml(propertyData).asXmlString());
	}
}
