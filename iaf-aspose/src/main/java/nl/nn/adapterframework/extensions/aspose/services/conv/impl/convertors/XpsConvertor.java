package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.InputStream;

import org.apache.tika.mime.MediaType;

import com.aspose.xps.XpsDocument;
import com.aspose.xps.XpsLoadOptions;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

public class XpsConvertor extends AbstractConvertor {

	XpsConvertor(String pdfOutputlocation) {
		super(pdfOutputlocation, new MediaType("application", "vnd.ms-xpsdocument"),
				new MediaType("application", "x-tika-ooxml"));
	}

	@Override
	void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult result,
			ConversionOption conversionOption) throws Exception {
		// PdfSaveOptions saveOption = new PdfSaveOptions();
		XpsLoadOptions load = new XpsLoadOptions();
		XpsDocument xps = new XpsDocument(inputStream, load);
		// add glyph to the document
		// com.aspose.xps.XpsGlyphs glyphs = xps.addGlyphs("Arial", 12,
		// XpsFontStyle.Regular, 300f, 450f, "Hello World!");
		// glyphs.setFill(xps.createSolidColorBrush(Color.BLACK));
		// save result
		xps.save(fileDest.getAbsolutePath());

		// result.setMetaData(new MetaData(getNumberOfPages(fileDest)));

	}

	@Override
	protected boolean isPasswordException(Exception e) {
		// TODO Auto-generated method stub
		return false;
	}

}
