package org.frankframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

public class JacksonUtilsTest {

	public enum TestEnum { Aa, Bb }

	@JsonInclude(Include.NON_NULL)
	public static class DummyDTO {
		private @Getter @Setter List<String> dummyList = new ArrayList<>();
		private @Getter @Setter String nullField;
		private @Getter @Setter String emptyField = "";
		private @Getter @Setter TestEnum testEnum;
	}

	@Test
	public void testObjectToJson() {
		// Arrange
		List<String> dummyList = new ArrayList<>();
		dummyList.add("one");
		dummyList.add("two");
		dummyList.add("three");

		// Act + Assert
		assertEquals("[\"one\",\"two\",\"three\"]", JacksonUtils.convertToJson(dummyList));
	}

	@Test
	public void testDtoToJson() {
		// Arrange
		DummyDTO dto = new DummyDTO();
		dto.getDummyList().add("one");
		dto.getDummyList().add("two");
		dto.getDummyList().add("three");

		// Act + Assert
		assertEquals("{\"dummyList\":[\"one\",\"two\",\"three\"],\"emptyField\":\"\"}", JacksonUtils.convertToJson(dto));
	}

	@Test
	public void testJsonToDto() {
		// Arrange
		String jsonInput = "{\"dummyList\":[\"one\",\"two\",\"three\"],\"emptyField\":\"\"}";

		// Act
		DummyDTO dto = JacksonUtils.convertToDTO(jsonInput, DummyDTO.class);

		// Assert
		assertAll(
				() -> assertEquals("", dto.getEmptyField()),
				() -> assertNull(dto.getNullField()),
				() -> assertNull(dto.getTestEnum()),
				() -> assertThat(dto.getDummyList(), containsInAnyOrder("one", "two", "three"))
		);
	}

	@Test
	public void testEnumCapitalization() {
		// Arrange
		String jsonInput = "{\"dummyList\":[],\"emptyField\":\"\",\"testEnum\":\"aa\"}";

		// Act
		DummyDTO dto = JacksonUtils.convertToDTO(jsonInput, DummyDTO.class);

		// Assert
		assertAll(
				() -> assertEquals("", dto.getEmptyField()),
				() -> assertNull(dto.getNullField()),
				() -> assertEquals(TestEnum.Aa, dto.getTestEnum()),
				() -> assertTrue(dto.getDummyList().isEmpty())
		);
	}

	@Test
	public void testInvalidEnumValue() {
		// Arrange
		String jsonInput = "{\"dummyList\":[],\"testEnum\":\"foobar\"}";

		// Act
		DummyDTO dto = JacksonUtils.convertToDTO(jsonInput, DummyDTO.class);

		// Assert
		assertAll(
				() -> assertEquals("", dto.getEmptyField()),
				() -> assertNull(dto.getTestEnum()),
				() -> assertTrue(dto.getDummyList().isEmpty())
		);
	}

	@Test
	public void testUnknownJsonElements() {
		// Arrange
		String jsonInput = "{\"dummyList\":[],\"woopwoop\":\"\",\"tralalalala\":\"aa\"}";

		// Act
		DummyDTO dto = JacksonUtils.convertToDTO(jsonInput, DummyDTO.class);

		// Assert
		assertAll(
				() -> assertEquals("", dto.getEmptyField()),
				() -> assertNull(dto.getNullField()),
				() -> assertTrue(dto.getDummyList().isEmpty())
		);
	}
}
