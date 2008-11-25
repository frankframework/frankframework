/* $Log: RekenboxLineReader.java,v $
/* Revision 1.1  2008-11-25 10:17:43  m168309
/* first version
/* */
package nl.nn.adapterframework.extensions.rekenbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This subclass of BufferedReader modifies the meaning of
 * readLine such that lines break on '\n' OR ';' (OR ";\n").
 * This is nessecary to read files from the calcbox, which can be
 * sent without linebreaks but which always end with a ';'.
 * 
 * @author leeuwt
 *
 * Change History
 * Author                   Date        Version     Details
 * Tim N. van der Leeuw     30-07-2002  1.0         Initial release
 * 
 * 
 */
class RekenboxLineReader extends BufferedReader
{
    private int m_pushbackBuffer = -1;
    private boolean m_pushbackValid = false;
    
    /**
     * Constructor for RekenboxLineReader.
     * @param in    Underlying Reader
     * @param sz    Size of read-buffer.
     * 
     */
    public RekenboxLineReader(Reader in, int sz)
    {
        super(in, sz);
    }

    /**
     * Constructor for RekenboxLineReader.
     * @param in    Underlying Reader
     * 
     */
    public RekenboxLineReader(Reader in)
    {
        super(in);
    }

    /**
     * @see BufferedReader#readLine()
     */
    public String readLine() throws IOException
    {
        StringBuffer str;
        boolean eos;
        
        str = new StringBuffer(1024);
        eos = false;
        
        if (havePushback())
        {
            char c = (char)getPushback();
            if (c == -1)
            {
                return null;
            }
            str.append(c);
        }
        
        while(true)
        {
            int b = read();
            if (b == -1)
            {
				eos = true;
                break;
            }
            else if (b == '\n')
            {
                break;
            }
//          BUGFIX: ACHTER DE if GEZET
//          str.append((char)b);
            if (b == ';')
            {
                int b2 = read();
                if (b2 != '\n')
                {
                    pushback(b2);
                }
                break;
            }
            else
            {
                str.append((char)b);
            }
        }
        if (eos && str.length() == 0)
        {
            return null;
        }
        return str.toString();
    }

	/**
	 * Method havePushback. Checks if the pushback-buffer is in use.
	 * @return boolean  <code>true</code> if there is a character in
     *                  the pushback-buffer, <code>false</code> if not.
	 */
    protected boolean havePushback()
    {
        return m_pushbackValid;
    }
    
	/**
	 * Method getPushback. Returns the value of the pushback-buffer.
     * 
	 * @return int     The character which was in the pushback-buffer.
	 */
    protected int getPushback()
    {
        if (m_pushbackValid)
        {
            m_pushbackValid = false;
            int result = m_pushbackBuffer;
            m_pushbackBuffer = -1;
            return result;
        }
        throw new IllegalStateException("Attempting to read pushback " +
            "character from stream when it's not available.");
    }
    
	/**
	 * Method pushback.
	 * @param b    Integer to push back
	 */
    protected void pushback(int b)
    {
        if (m_pushbackValid)
        {
            throw new IllegalStateException("Attempting to push multiple " +
                "characters back into stream.");
        }
        m_pushbackValid = true;
        m_pushbackBuffer = b;
    }
}
