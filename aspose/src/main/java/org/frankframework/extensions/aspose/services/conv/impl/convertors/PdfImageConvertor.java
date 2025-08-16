/*
   Copyright 2019, 2021-2022 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

import com.aspose.imaging.extensions.ImageExtensions;
import com.aspose.imaging.fileformats.tiff.TiffFrame;
import com.aspose.imaging.fileformats.tiff.TiffImage;
import com.aspose.imaging.imageoptions.PngOptions;
import com.aspose.pdf.Document;
import com.aspose.pdf.Image;
import com.aspose.pdf.LoadOptions;
import com.aspose.pdf.Page;
import com.aspose.pdf.SaveFormat;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.LogUtil;

/**
 * Converts the files which are required and supported by the Aspose image library.
 * @author Gerard van der Hoorn
 */
public class PdfImageConvertor extends AbstractConvertor {

	private static final Logger LOGGER = LogUtil.getLogger(PdfImageConvertor.class);

	private static final float NO_SCALE_FACTOR = 1.0f;
	private static final int NUMBER_OF_MARGINS = 2;
	private static final String IMAGE = "image";
	private static final String TIFF = "tiff";

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
		map.put(new MediaType(IMAGE, "bmp"), null);
		map.put(new MediaType(IMAGE, "x-ms-bmp"), null);

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	protected PdfImageConvertor(CisConfiguration configuration) {
		// Give the supported media types.
		super(configuration, MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet());
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		message.preserve();

		try (Document doc = new Document()) {
			Page page = doc.getPages().add(); // Don't close this!?

			// Set borders on 0.5cm.
			float marginInCm = 0.0f;
			page.getPageInfo().getMargin().setTop(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setBottom(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setLeft(PageConvertUtil.convertCmToPoints(marginInCm));
			page.getPageInfo().getMargin().setRight(PageConvertUtil.convertCmToPoints(marginInCm));

			com.aspose.imaging.Image image = null;
			try(InputStream is = message.asInputStream()) {
				image = com.aspose.imaging.Image.load(is);
				if(mediaType.getSubtype().equalsIgnoreCase(TIFF)) {
					handleTiff((TiffImage) image, page);
				} else {
					handleImage(message, image, page, marginInCm);
				}
			} finally {
				if (image != null) {
					image.close();
					image = null;
				}
			}

			long startTime = new Date().getTime();
			doc.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.Pdf);
			long endTime = new Date().getTime();
			LOGGER.info("Conversion(save operation in convert method) takes  ::: {} ms", () -> (endTime - startTime));
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
		}
	}

	private void handleImage(Message message, com.aspose.imaging.Image image, Page page, float marginInCm) throws IOException {
		BufferedImage bufferedImage = ImageExtensions.toJava(image);
		LOGGER.debug("Image info height:{} width:{}", bufferedImage::getHeight, bufferedImage::getWidth);

		float maxImageWidthInPoints = PageConvertUtil.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - NUMBER_OF_MARGINS * marginInCm);
		float maxImageHeightInPoints = PageConvertUtil.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - NUMBER_OF_MARGINS * marginInCm);

		float scaleWidth = maxImageWidthInPoints / bufferedImage.getWidth();
		float scaleHeight = maxImageHeightInPoints / bufferedImage.getHeight();

		// Get the smallest scale factor so it will fit on the paper.
		float scaleFactor = Math.min(scaleWidth, scaleHeight);
		if (scaleFactor > NO_SCALE_FACTOR) {
			scaleFactor = NO_SCALE_FACTOR;
		}

		Image pdfImage = new Image();
		try (InputStream is = message.asInputStream()) {
			pdfImage.setImageStream(is);
		}

		pdfImage.setImageScale(scaleFactor);
		page.getParagraphs().add(pdfImage);
	}

	private void handleTiff(TiffImage tiffImage, Page page) throws IOException {
		TiffFrame[] frames = tiffImage.getFrames();
		try(PngOptions pngOptions = new PngOptions()) {
			for (TiffFrame tiffFrame : frames) {
				MessageBuilder messageBuilder = new MessageBuilder();
				try (OutputStream out = messageBuilder.asOutputStream()) {
					tiffFrame.save(out, pngOptions);
				}

				Image pdfImage = new Image();
				try (Message imgMessage = messageBuilder.build(); InputStream is = imgMessage.asInputStream()) {
					pdfImage.setImageStream(is);
					page.getParagraphs().add(pdfImage);
				}
			}
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return false;
	}

}
