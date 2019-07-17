package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.InputStream;

import org.apache.tika.mime.MediaType;

import com.aspose.html.HTMLDocument;
import com.aspose.html.saving.HTMLSaveOptions;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * This class is implemented to see if converting html file to pdf can be done
 * here. Pdf library also supports html conversion so use pdf instead and remove
 * this
 * 
 * @author alisihab
 *
 */
public class HtmlConvertor extends AbstractConvertor {

	HtmlConvertor(String pdfOutputlocation) {
		super(pdfOutputlocation, new MediaType("text", "html"), new MediaType("application", "xhtml+xml"));
	}

	@Override
	void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult builder,
			ConversionOption conversionOption) throws Exception {

		HTMLDocument doc = new HTMLDocument(inputStream, "https://en.wikipedia.org/wiki/Aspose_API");
		HTMLSaveOptions saveOption = new HTMLSaveOptions();
		doc.save(fileDest.getAbsolutePath(), saveOption);
		if (doc != null) {
			doc.dispose();

		}

		// HtmlLoadOptions htmloptions = new HtmlLoadOptions();
		// // Load HTML file
		// Document doc = new com.aspose.pdf.Document(inputStream, htmloptions);
		// // Save HTML file
		// doc.save(fileDest.getAbsolutePath());

	}

	@Override
	protected boolean isPasswordException(Exception e) {
		// TODO Auto-generated method stub
		return false;
	}

}
