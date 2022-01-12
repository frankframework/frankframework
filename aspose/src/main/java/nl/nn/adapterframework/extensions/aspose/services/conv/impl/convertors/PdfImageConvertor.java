/*
   Copyright 2019 Integration Partners

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.imaging.extensions.ImageExtensions;
import com.aspose.imaging.fileformats.tiff.enums.TiffExpectedFormat;
import com.aspose.imaging.imageoptions.TiffOptions;
import com.aspose.pdf.Document;
import com.aspose.pdf.Image;
import com.aspose.pdf.LoadOptions;
import com.aspose.pdf.Page;
import com.aspose.pdf.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.util.LogUtil;

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

	private static final Logger LOGGER = LogUtil.getLogger(PdfImageConvertor.class);

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
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because
			// the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}

		File tmpImageFile = null;
		com.aspose.imaging.Image image = null;
		Document doc = new Document();
		float scaleFactor = 0;
		try {
			// Set borders on 0.5cm.
			float marginInCm = 0.0f;
			Page page = doc.getPages().add();
			page.getPageInfo().getMargin().setTop(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setBottom(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setLeft(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setRight(PageConvertUtil.convertCmToPoints(marginInCm));

			// Temporary file (because first we need to get image information (the size) and than load it into 
			// the pdf. The image itself can not be loaded into the pdf because it will be blured with orange.
			tmpImageFile = UniqueFileGenerator.getUniqueFile(getPdfOutputlocation(), this.getClass().getSimpleName(), mediaType.getSubtype());
			image =  com.aspose.imaging.Image.load(file.getAbsolutePath());
			if(mediaType.getSubtype().equalsIgnoreCase(TIFF)) {
				try(TiffOptions options = new TiffOptions(TiffExpectedFormat.TiffJpegRgb)){
					image.save(tmpImageFile.getAbsolutePath(), options);
				}
			}else {
				Files.copy(file.toPath(), tmpImageFile.toPath());
				BufferedImage bufferedImage = ImageExtensions.toJava(image);
				LOGGER.debug("Image info height:" + bufferedImage.getHeight() + " width:" + bufferedImage.getWidth());

				float maxImageWidthInPoints = PageConvertUtil
						.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - NUMBER_OF_MARGINS * marginInCm);
				float maxImageHeightInPoints = PageConvertUtil
						.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - NUMBER_OF_MARGINS * marginInCm);

				float scaleWidth = maxImageWidthInPoints / bufferedImage.getWidth();
				float scaleHeight = maxImageHeightInPoints / bufferedImage.getHeight();

				// Get the smallest scale factor so it will fit on the paper.
				scaleFactor = Math.min(scaleWidth, scaleHeight);
				if (scaleFactor > NO_SCALE_FACTOR) {
					scaleFactor = NO_SCALE_FACTOR;
				}
			}
			
			Image pdfImage = new Image();
			pdfImage.setFile(tmpImageFile.getAbsolutePath());
			
			// do not set scale if the image type is tiff
			if (!mediaType.getSubtype().equalsIgnoreCase(TIFF)) {
				pdfImage.setImageScale(scaleFactor);
			}

			page.getParagraphs().add(pdfImage);
			long startTime = new Date().getTime();
			doc.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.Pdf);
			long endTime = new Date().getTime();
			LOGGER.info(
					"Conversion(save operation in convert method) takes  :::  " + (endTime - startTime) + " ms");
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
			
		} finally {
			doc.freeMemory();
			doc.dispose();
			doc.close();

			if (image != null) {
				image.close();
				image = null;
			}
			// Delete always the temporary file.
			 if (tmpImageFile != null) {
				 Files.delete(tmpImageFile.toPath());
			 }
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return false;
	}

}
