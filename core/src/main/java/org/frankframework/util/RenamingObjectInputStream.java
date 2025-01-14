/*
   Copyright 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;

import org.apache.commons.lang3.reflect.FieldUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassNameRewriter;

@Log4j2
public class RenamingObjectInputStream extends ObjectInputStream {

	public RenamingObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

	// Support old package names, already serialized in the database
	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

		String className = resultClassDescriptor.getName();
		if (className.startsWith(ClassNameRewriter.LEGACY_PACKAGE_NAME)) {
			String newClassName = className.replace(ClassNameRewriter.LEGACY_PACKAGE_NAME, ClassNameRewriter.ORG_FRANKFRAMEWORK_PACKAGE_NAME);
			try {
				// Do not return a new ObjectStreamClass looked up from new class definitions, but modify the descriptor read from the stream.
				// Otherwise some flags could be wrong, like the flag "hasWriteObjectData" when reading objects from 7.6/7.7.
				Field nameField = ObjectStreamClass.class.getDeclaredField("name");
				nameField.setAccessible(true);
				FieldUtils.writeField(nameField, resultClassDescriptor, newClassName);
			} catch (IllegalAccessException | NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
		}
		return resultClassDescriptor;
	}
}
