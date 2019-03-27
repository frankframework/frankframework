package nl.nn.adapterframework.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class MockFile {

	private String name;
	private MockFolder owner;
	private byte[] contents;
	private Date lastModified=new Date();
	
	public MockFile(String name, MockFolder owner) {
		super();
		this.name = name;
		this.owner=owner;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public MockFolder getOwner() {
		return owner;
	}
	public void setOwner(MockFolder owner) {
		this.owner = owner;
	}
	
	public OutputStream getOutputStream(boolean truncate) throws IOException {

		OutputStream result=new ByteArrayOutputStream() {

			@Override
			public void close() throws IOException {
				super.close();
				contents=toByteArray();
				lastModified=new Date();
			}

			@Override
			public void flush() throws IOException {
				super.flush();
				contents=toByteArray();
				lastModified=new Date();
			}
			
		};
		if (truncate) {
			contents=null;
		} else {
			if (contents!=null) {
				result.write(contents);
			}
		}
		return result;
	}
	
	public InputStream getInputStream() {
		return new ByteArrayInputStream(contents==null? new byte[0]:contents);
	}

	public Date getLastModified() {
		return lastModified;
	}
	
	public byte[] getContents() {
		return contents;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}


}
