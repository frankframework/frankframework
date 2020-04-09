package nl.nn.adapterframework.jdbc.dbms.translator;

public class MssqlTranslator extends ITranslator {
	public MssqlTranslator(ITranslator target) {
		super(target);
	}

	@Override
	protected void populateMaps() {
		regexes.put("NEXTVAL", toPattern("NEXT VALUE FOR ([a-z,A-Z]+)"));
		replacements.put("NEXTVAL", "(NEXT VALUE FOR $1)");

		regexes.put("CURRVAL", toPattern("SELECT CURRENT_VALUE FROM SYS.SEQUENCES WHERE NAME\\s+=\\s+([a-z,A-Z]+)"));
		replacements.put("CURRVAL", "(SELECT CURRENT_VALUE FROM SYS.SEQUENCES WHERE NAME = $1)");

		replacements.put("EMPTY_BLOB", "null");
		replacements.put("EMPTY_CLOB", "null");

		regexes.put("SYSDATE", toPattern("GETDATE()"));
		replacements.put("SYSDATE", "GETDATE()");

		regexes.put("SYSTIMESTAMP", toPattern("CURRENT_TIMESTAMP"));
		replacements.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP");

		regexes.put("FOR UPDATE", toPattern("(.*) FOR UPDATE (/*) FROM (.*)(\\s+WHERE.*)?"));
		replacements.put("FOR UPDATE", "$1 $2 FROM $3 WITH (UPDLOCK, ROWLOCK) $4");

		regexes.put("LIMIT", toPattern("LIMIT [0-9]+"));
		replacements.put("LIMIT", "LIMIT $1");
	}
}
