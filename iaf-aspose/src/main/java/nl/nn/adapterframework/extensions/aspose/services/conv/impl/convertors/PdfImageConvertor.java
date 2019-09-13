/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.imaging.extensions.ImageExtensions;
import com.aspose.pdf.Document;
import com.aspose.pdf.Image;
import com.aspose.pdf.LoadOptions;
import com.aspose.pdf.Page;
import com.aspose.pdf.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * Converts the files which are required and supported by the aspose pdf
 * library.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class PdfImageConvertor extends AbstractConvertor {

	private static final float NO_SCALE_FACTOR = 1.0f;

	private static final int NUMBER_OF_MARGINS = 2;

	private static final String IMAGE = "image";

	private static final String TIFF = "tiff";

	private static final Logger LOGGER = Logger.getLogger(PdfImageConvertor.class);

	// contains mapping from MediaType to the LoadOption for the aspose word
	// conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// The string value is defined in com.aspose.pdf.LoadOptions.
		map.put(new MediaType(IMAGE, "jpeg"), null);
		map.put(new MediaType(IMAGE, "png"), null);
		map.put(new MediaType(IMAGE, "gif"), null);
		map.put(new MediaType(IMAGE, TIFF), null);
		map.put(new MediaType(IMAGE, "x-ms-bmp"), null);

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	PdfImageConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation,
				MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));
	}

	@Override
	void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because
			// the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}

		// File tmpImageFile = null;
		com.aspose.imaging.Image image = null;
		Document doc = new Document();
		try {
			// Set borders on 0.5cm.
			float marginInCm = 0.0f;
			Page page = doc.getPages().add();
			page.getPageInfo().getMargin().setTop(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setBottom(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setLeft(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setRight(PageConvertUtil.convertCmToPoints(marginInCm));

			// Temporary file (because first we need to get image information (the size) and
			// than load it into
			// the pdf. The image itself can not be loaded into the pdf because it will be
			// blured with orange.
			// tmpImageFile = UniqueFileGenerator.getUniqueFile(getPdfOutputlocation(),
			// this.getClass().getSimpleName(),
			// mediaType.getSubtype());
			// Files.copy(inputStream, tmpImageFile.toPath());

			image = com.aspose.imaging.Image.load(file.getAbsolutePath());

			BufferedImage bufferedImage = ImageExtensions.toJava(image);
			LOGGER.debug("Image info height:" + bufferedImage.getHeight() + " width:" + bufferedImage.getWidth());

			// BufferedImage bufferedImage = ImageIO.read(inputStream);
			// LOGGER.debug("Image info height:" + bufferedImage.getHeight() + " width:" +
			// bufferedImage.getWidth());

			// page width in points is 595.0, height 842.0 ( page.getPageInfo().getHeight()
			// page.getPageInfo().getWidth())
			float maxImageWidthInPoints = PageConvertUtil
					.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - NUMBER_OF_MARGINS * marginInCm);
			float maxImageHeightInPoints = PageConvertUtil
					.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - NUMBER_OF_MARGINS * marginInCm);

			float scaleWidth = maxImageWidthInPoints / bufferedImage.getWidth();
			float scaleHeight = maxImageHeightInPoints / bufferedImage.getHeight();

			// Get the smallest scale factor so it will fit on the paper.
			float scaleFactor = Math.min(scaleWidth, scaleHeight);
			if (scaleFactor > NO_SCALE_FACTOR) {
				scaleFactor = NO_SCALE_FACTOR;
			}

			Image pdfImage = new Image();
			pdfImage.setFile(file.getAbsolutePath());
			// pdfImage.setBufferedImage(bufferedImage);

			// do not set scale if the image type is tiff
			if (!mediaType.getSubtype().equalsIgnoreCase(TIFF)) {
				pdfImage.setImageScale(scaleFactor);
			}

			page.getParagraphs().add(pdfImage);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			long startTime = new Date().getTime();
			doc.save(outputStream, SaveFormat.Pdf);
			long endTime = new Date().getTime();
			LOGGER.info("Conversion(save operation in convert method) takes  :::  " + (endTime - startTime) + " ms");
			InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
			result.setFileStream(inStream);
			outputStream.close();
			// result.setMetaData(new MetaData(getNumberOfPages(inStream)));

		} finally {
			doc.freeMemory();
			doc.dispose();
			doc.close();

			if (image != null) {
				image.close();
				image = null;
			}
			// Delete always the temporary file.
			// if (tmpImageFile != null) {
			// // Delete alwasy the temporary file.
			// Files.delete(tmpImageFile.toPath());
			// }
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return false;
	}

}
