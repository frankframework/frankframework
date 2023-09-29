package nl.nn.adapterframework.dbms;


public class DbmsException extends JdbcException {
	public DbmsException() {
		super();
	}

	public DbmsException(String arg1) {
		super(arg1);
	}

	public DbmsException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}

	public DbmsException(Throwable arg1) {
		super(arg1);
	}
}
