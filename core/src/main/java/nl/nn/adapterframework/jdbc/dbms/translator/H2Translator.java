package nl.nn.adapterframework.jdbc.dbms.translator;

public class H2Translator extends ITranslator {
	public H2Translator(ITranslator target) {
		super(target);
	}

	@Override
	protected void populateMaps() {
		regexes.put("NEXTVAL", toPattern("NEXT VALUE FOR ([a-z,A-Z]+)"));
		replacements.put("NEXTVAL", "(NEXT VALUE FOR $1)");

		regexes.put("CURRVAL", toPattern("CURRENT VALUE FOR ([a-z,A-Z]+)"));
		replacements.put("CURRVAL", "(CURRENT VALUE FOR $1)");

		replacements.put("EMPTY_BLOB", "NULL");
		replacements.put("EMPTY_CLOB", "NULL");

		regexes.put("SYSDATE", toPattern("CURRENT_DATE"));
		replacements.put("SYSDATE", "CURRENT_DATE");

		regexes.put("SYSTIMESTAMP", toPattern("CURRENT_TIMESTAMP"));
		replacements.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP");

		regexes.put("LIMIT", toPattern("LIMIT ([0-9]+)"));
		replacements.put("LIMIT", "LIMIT $1");
	}
}
