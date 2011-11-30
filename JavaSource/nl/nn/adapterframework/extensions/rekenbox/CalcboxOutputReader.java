/* $Log: CalcboxOutputReader.java,v $
/* Revision 1.3  2011-11-30 13:52:03  europe\m168309
/* adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
/*
/* Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* Upgraded from WebSphere v5.1 to WebSphere v6.1
/*
/* Revision 1.1  2008/11/25 10:17:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
/* first version
/* */
package nl.nn.adapterframework.extensions.rekenbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  This is the reader of the Calcbox output. This class
 *   uses the XMLReader interface, so it reads the 
 *   Calcbox output in the same way it reads XML
 *
 *  Change History
 *  Author                  Date         Version    Details
 *  Colin Wilmans           18-04-2002   1.0        First version
 *  Tim N. van der Leeuw    30-07-2002   1.1        Better handling of
 *                                                  labels with sequence
 *                                                  numbers.
 *                                                  Split-method taken out
 *                                                  and put in Util-object.
 *  Boris O Maltha			23-01-2003	 1.2		Added logging
 *
*/
public class CalcboxOutputReader implements XMLReader
{  
	ContentHandler handler;
	    
	// We're not doing namespaces. 
	String nsu = "";							// NamespaceURI
	String rootElement = "CALCBOXMESSAGE";
	String indent = "\n    ";					// for readability!
	        
	/** Parse the input (CalcBox Message Format) */
	public void parse(InputSource input) throws IOException, SAXException 
    {
		try 
		{
			//If we have no handler we can stop
            if (handler==null) 
            {
				throw new SAXException("No content handler");
            }

            // Note: 
            // We're ignoring setDocumentLocator(), as well
            Attributes atts = new AttributesImpl();
            handler.startDocument();      
            handler.startElement(nsu, rootElement, rootElement, atts);      

            // Get an efficient reader for the file
            java.io.Reader r = input.getCharacterStream();
            BufferedReader br = new RekenboxLineReader(r);
          
            // Read the file and output it's contents.
            String line = "";
            while (null != (line = br.readLine())) 
            {
				  
				//get everything before :
                int colon = line.indexOf(":");
                if (colon == -1)
                {
                    continue;
                }
				String calcboxtag = line.substring(0, colon).trim();
				output(calcboxtag, calcboxtag, line);
              
            }

			//XXX 
			handler.ignorableWhitespace("\n".toCharArray(), 0, 1);  
			  
			//Place last end tags...
			for (int i=tagMemory.length;i>0;i--)
			{
                String strippedTag = striptrailingnumbers(tagMemory[i-1]);
                handler.endElement(nsu, strippedTag, strippedTag);
			}
			handler.endElement(nsu, rootElement, rootElement);
			handler.endDocument();      
 
        }
        catch (Exception e)
        {
			//throw new SAXException(e);
        }
	}    

	//static int to memorize details of last tags
	String[] tagMemory = {};

	/** Create output of the input */ 
    void output(String name, String prefix, String line) throws SAXException 
    {
        String value;

		// Tags with '#SAMENGESTELD' are not interesting for us, 
		//  because next tag gives this information (redundant protocol error:)
		if(!(line.indexOf("#SAMENGESTELD")== -1)) return;

		//place tag in arraylist
		String[] arrayTagString = split(name, "."); 

		//Figure out wich elements changed...
		int tagChangeLevel = tagMemory.length + 1;
		for(int i=tagMemory.length;i>0;i--)
		{
		  	//If new tag has more tag parts or a tag part has changed
		  	if(i>arrayTagString.length||!tagMemory[i-1].equals(arrayTagString[i-1]))
            {
		  		tagChangeLevel = i;
            }
		}

		//And place end elements for these tags
		for (int i=tagMemory.length;i>=tagChangeLevel;i--)
		{
            String strippedTag = striptrailingnumbers(tagMemory[i-1]);
		  	handler.endElement(nsu, strippedTag, strippedTag);
		}

		//Determine where the ':' is, and take this as the startposition
		int startIndex = line.indexOf(":") + 1; // 1=length of ":" after the name
		handler.ignorableWhitespace(indent.toCharArray(), 0, indent.length());
			  
		//Place start elements
		for(int i=tagChangeLevel;i<=arrayTagString.length;i++)
		{
            String label = arrayTagString[i-1];
            int splitPos = trailingNumberSplitPos(label);
            String tag = label.substring(0, splitPos);
            String number = label.substring(splitPos);
            
            AttributesImpl atts = new AttributesImpl();
            
            if (number.length() > 0)
            {
                atts.addAttribute(nsu, "volgnummer", "volgnummer", "", number);
            }
		  	handler.startElement(nsu, tag, tag, atts);
		}

		//don't forget the tagarray for the next time
		tagMemory = arrayTagString;
			  
		//Place the element tag
        value = line.substring(startIndex).trim();
//		BUGFIX: WE MOGEN VERWACHTEN GEEN ; TERUG TE KRIJGEN
//		handler.characters(value.toCharArray(), 0, value.length()-1);
		handler.characters(value.toCharArray(), 0, value.length());

	}
  
    /** Allow an application to register a content event handler. */
    public void setContentHandler(ContentHandler handler) 
    {
		this.handler = handler;
    } 

    /** Return the current content handler. */
    public ContentHandler getContentHandler() 
    {
		return this.handler;
    } 
    
    //=============================================
    // IMPLEMENT THESE FOR A ROBUST APP
    //=============================================
    /** Allow an application to register an error event handler. */
    public void setErrorHandler(ErrorHandler handler)
    { }
    
    /** Return the current error handler. */
    public ErrorHandler getErrorHandler()
    { return null; }

    //=============================================
    // IGNORE THESE
    //=============================================   
    /** Parse an XML document from a system identifier (URI). */
    public void parse(String systemId)
    throws IOException, SAXException 
    { }
     
    /** Return the current DTD handler. */
    public DTDHandler getDTDHandler()
    { return null; }
  
    /** Return the current entity resolver. */
    public EntityResolver getEntityResolver()
    { return null; }
  
    /** Allow an application to register an entity resolver. */
    public void setEntityResolver(EntityResolver resolver)
    { }
  
    /** Allow an application to register a DTD event handler. */
    public void setDTDHandler(DTDHandler handler)
    { }
  
    /** Look up the value of a property. */
    public Object getProperty(java.lang.String name)
    { return null; }
    
    /** Set the value of a property. */
    public void setProperty(java.lang.String name, java.lang.Object value)
    { } 

    /** Set the state of a feature. */
    public void setFeature(java.lang.String name, boolean value)
    { }
    
    /** Look up the value of a feature. */
    public boolean getFeature(java.lang.String name)
    { return false; }  

	/** strip function to strip trailing numbers */
	private static String striptrailingnumbers(String str)
	{
		 //XXXXX Strip numbers
			                    
		boolean containstrailingnumbers = false;
		int j=0;
		for(j=str.length();j>0;j--)
		{
			if(Character.isDigit(str.charAt(j-1)))
			{
				containstrailingnumbers = true;
			}
			else
			{
				break;
			}
		}
		if(containstrailingnumbers)
		{
			return str.substring(0,j);
		}
		else
		{
			return str;
		}
	}
    
    private static int trailingNumberSplitPos(String str)
    {
        for(int j=str.length(); j>0; --j)
        {
            if(!Character.isDigit(str.charAt(j-1)))
            {
                return j;
            }
        }
        return 0;
    }

    private String[] split(String name, String separators) {
        StringTokenizer st = new StringTokenizer(name, separators);
	List list = new ArrayList();
	while (st.hasMoreTokens()) {
		list.add(st.nextToken());
	}
  String[] array = new String[list.size()];
  for (int i = 0; i < array.length; i++) {
    array[i] = (String)list.get(i);
  }
	return array;
    }
}
