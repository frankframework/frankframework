/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva;

import java.io.File;
import java.util.Objects;

import lombok.Getter;

/**
 * Data class to hold scenario data.
 *
 * TODO: Decide if ScenarioProperties should also be loaded as part of this already, or not
 * because of memory consumption reasons.
 * Keeping them in memory here would save some time but not much.
 */
public class Scenario {
	private final @Getter ID id;
	private final @Getter File scenarioFile;
	private final @Getter String name;
	private final @Getter String description;

	public Scenario(File scenarioFile, String name, String description) {
		this.id = new ID(scenarioFile);
		this.scenarioFile = scenarioFile;
		this.name = name;
		this.description = description;
	}

	public String getLongName() {
		return scenarioFile.getAbsolutePath();
	}

	public static class ID {
		private final String scenarioId;

		public ID(String scenarioFilePath) {
			this(new File(scenarioFilePath));
		}

		public ID(File scenarioFile) {
			this.scenarioId = scenarioFile.getAbsolutePath().replaceAll("([\\\\/:])", "-");
		}

		@Override
		public String toString() {
			return scenarioId;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ID id1 = (ID) o;
			return Objects.equals(scenarioId, id1.scenarioId);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(scenarioId);
		}
	}
}
