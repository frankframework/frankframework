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
package nl.nn.adapterframework.jdbc;

import java.sql.SQLType;

import lombok.Getter;

class StoredProcedureParamDef {
	@Getter final int position;
	@Getter final SQLType type;
	@Getter final String name;

	StoredProcedureParamDef(int position, SQLType type) {
		this(position, type, String.valueOf(position));
	}

	StoredProcedureParamDef(int position, SQLType type, String name) {
		this.position = position;
		this.type = type;
		this.name = name;
	}
}
