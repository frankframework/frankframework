package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public interface Schema {

	public InputStream getInputStream() throws IOException;

	public Reader getReader() throws IOException;

	public String getSystemId();

}
