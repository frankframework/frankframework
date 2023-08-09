package nl.nn.adapterframework.jdbc;

import java.sql.SQLType;

import lombok.Getter;

class StoredProcedureParamDef {
	@Getter final int position;
	@Getter final SQLType type;
	@Getter final String name;

	StoredProcedureParamDef(int position, SQLType type) {
		this.position = position;
		this.type = type;
		this.name = String.valueOf(position);
	}

	StoredProcedureParamDef(int position, SQLType type, String name) {
		this.position = position;
		this.type = type;
		this.name = name;
	}
}
