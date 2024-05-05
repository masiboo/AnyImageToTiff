package org.example;

import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import net.coobird.thumbnailator.Thumbnails;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class ImageConverter {

    private static final int LOGO_DPI = 266;

    private static final int WHEN_NO_METADATA_DEFAULT_HEIGHT = 600;

    private static final int WHEN_NO_METADATA_DEFAULT_WIDTH = 200;

    private static final int MAX_ALLOWED_SIZE = 20;

    private static final double ONE_INCH_TO_CENTIMETER = 2.54;

    private static int THUMBNAIL_HEIGHT_OR_WIDTH = 2090;

    private static final int MAX_IMAGE_SIZE_FOR_THUMBNAIL = 1250;

    private static String TIF_FORMAT = "TIF";

    private static final String TIFF_FORMAT = "TIFF";

    private static final File tempFile = new File("temp.tif");

    public enum classType {
        BWLOGO,
        COLOURLOGO
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file name:");
        String fileName = scanner.nextLine();
/*        String fileExtension = getExtension(fileName);
        if(fileExtension.equalsIgnoreCase(TIF_FORMAT) || fileExtension.equalsIgnoreCase(TIFF_FORMAT)){
            THUMBNAIL_HEIGHT_OR_WIDTH = 2090;
        }*/
  /*      System.out.println("Enter classType BWLOGO or COLOURLOGO:");
        String classTypeStr = scanner.nextLine();
        System.out.println("Enter only one image format as jpg, png, bmp, gif, tiff:");
        TIF_FORMAT = scanner.nextLine();
        classType type = null;
        if(classTypeStr.equalsIgnoreCase("c")){
            type = classType.COLOURLOGO;
        }else if(classTypeStr.equalsIgnoreCase("b")){
            type = classType.BWLOGO;
        }
*/
        File inutFile = new File(fileName);
        String absolutePath = null;
        if (inutFile.exists()) {
            absolutePath = inutFile.getAbsolutePath();
            System.out.println("Absolute path of input file: " + absolutePath);
        } else {
            System.out.println("File not found in the current directory.");
            return;
        }

        assert absolutePath != null;
        String outputFleName = getOutputFileName(absolutePath);

        try {
            // Read the input image
            BufferedImage inputImage = ImageIO.read(inutFile);
            if(!isMaxAllowedImageSize(inputImage)){
                System.out.println("Input image exceeded maximum allowed limit is 100 MB");
                return;
            }
            int dpi = getDpiFromFile(inutFile);
            if (dpi < 0) {
                throw new RuntimeException("Image DPI is missing in the metadata");
            }
            BufferedImage resizedBufferedImage = resizeImage(inputImage, String.valueOf(dpi));
            makeTifFromBufferedImage(resizedBufferedImage, outputFleName);
        } catch (IOException e) {
            System.out.println("Error occurred while converting the image: " + e.getMessage());
        }
    }

    public static boolean isMaxAllowedImageSize(BufferedImage outputImage) {
        DataBuffer dataBuffer = outputImage.getData().getDataBuffer();
        // Each bank element in the data buffer is a 32-bit integer
        long sizeBytes = ((long) dataBuffer.getSize()) * 4L;
        long sizeMB = sizeBytes / (1024L * 1024L);
        System.out.println("current input image size: " + sizeMB);
        if (sizeMB > MAX_IMAGE_SIZE_FOR_THUMBNAIL) {
            return false;
        }
        else {
            return true;
        }
    }

    private static void saveImage(BufferedImage image, String outputFileName) throws IOException {
        File outputFile = new File(outputFileName);
        ImageIO.write(image, TIF_FORMAT, outputFile);
        System.out.println("Converted output file: "+outputFileName);
    }

    private static String getInputFilePath(String fileName){
        URL resourceUrl = ImageConverter.class.getClassLoader().getResource(fileName);
        String filePath = null;
        if (resourceUrl != null) {
            // Get the file path from the URL
            filePath = resourceUrl.getPath();
            System.out.println("Input file path: " + filePath);
        } else {
            System.out.println("File not found: " + fileName);
        }
        return filePath;
    }
    private static String getOutputFileName(String filePath){
        int lastIndexOfSlash = filePath.lastIndexOf('/');
        String inputFileName = filePath.substring(lastIndexOfSlash + 1);
        return inputFileName.replaceAll("\\.[^.]*$", "") + "."+TIF_FORMAT.toLowerCase();
    }


    public static BufferedImage resizeImage(BufferedImage bufferedImage, String dpi) {
        BufferedImage resultImage = bufferedImage;
        try {
            if (isResizeNeeded(bufferedImage, Integer.parseInt(dpi))) {
                System.out.println("Resize is necessary because image size is larger than 20X20 CM");
                BufferedImage thumbnail = Thumbnails.of(bufferedImage)
                        .size(THUMBNAIL_HEIGHT_OR_WIDTH, THUMBNAIL_HEIGHT_OR_WIDTH)
                        .asBufferedImage();
                if (thumbnail == null) {
                    throw new RuntimeException("Error occurred while generating thumbnail from image");
                }
                resultImage = convertToTif(thumbnail);
            }
            else {
                System.out.println("No resize is necessary because image size is below 20X20 CM");
                resultImage = convertToTif(bufferedImage);
            }
            System.out.println("Image thumbnail generated and converted to tif successfully.");
            deleteTempFile();
        }
        catch (OutOfMemoryError e) {
            throw new OutOfMemoryError();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resultImage;
    }

    private static boolean isResizeNeeded(BufferedImage inputImage, int dpi) {
        int widthInPixels = inputImage.getWidth();
        int heightInPixels = inputImage.getHeight();
        if (widthInPixels <= 0) {
            widthInPixels = WHEN_NO_METADATA_DEFAULT_WIDTH;
        }
        if (heightInPixels <= 0) {
            heightInPixels = WHEN_NO_METADATA_DEFAULT_HEIGHT;
        }
        if (dpi <= 0) {
            dpi = LOGO_DPI;
        }
        double centimetersWidth = widthInPixels * ONE_INCH_TO_CENTIMETER / dpi;
        double centimetersHeight = heightInPixels * ONE_INCH_TO_CENTIMETER / dpi;
        System.out.println("Current input image width: " + centimetersWidth + " cm");
        System.out.println("Current input image height: " + centimetersHeight + " cm");
        return centimetersWidth > MAX_ALLOWED_SIZE || centimetersHeight > MAX_ALLOWED_SIZE;
    }

    private static BufferedImage convertToTif(BufferedImage inputBufferedImage) throws IOException {
        try {
            ImageIO.write(inputBufferedImage, TIF_FORMAT, tempFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return setImageDpi(LOGO_DPI, tempFile);
    }

    public static BufferedImage setImageDpi(int dpi, File outputImageFile) throws IOException {
        final ImageInputStream imageInputStream = ImageIO.createImageInputStream(outputImageFile);

        if (imageInputStream == null) {
            throw new IOException("Unable to obtain an input stream for file: " + outputImageFile.getAbsoluteFile());
        }
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
        if (imageReaders == null) {
            throw new IOException("Unable to obtain an image reader to decode the file: " + outputImageFile.getAbsoluteFile());
        }
        final ImageReader imageReader = imageReaders.next();
        imageReader.setInput(imageInputStream, false, true);
        final BufferedImage image = imageReader.read(0);
        final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputImageFile);
        if (imageOutputStream == null) {
            throw new IOException("Unable to obtain an output stream for file: " + outputImageFile.getAbsoluteFile());
        }
        imageOutputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        final Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(TIF_FORMAT);
        if (imageWriters == null) {
            throw new IOException("Unable to obtain an image writer for the tif file format!");
        }
        final ImageWriter imageWriter = imageWriters.next();
        imageWriter.setOutput(imageOutputStream);

        final ImageWriteParam parameters = imageWriter.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // Group 4 Compression
        parameters.setCompressionType("CCITT T.6");
        // high quality
        parameters.setCompressionQuality(1.0f);
        final List<TIFFEntry> entries = new ArrayList<>();
        entries.add(new TIFFEntry(TIFF.TAG_X_RESOLUTION, new Rational(dpi)));
        entries.add(new TIFFEntry(TIFF.TAG_Y_RESOLUTION, new Rational(dpi)));
        final IIOMetadata tiffImageMetadata = new TIFFImageMetadata(entries);
        try {
            imageWriter.write(null, new IIOImage(image, null, tiffImageMetadata), null);
        }
        catch (IOException e) {
            throw new IOException("Unable to write image to file: " + outputImageFile.getAbsoluteFile(), e);
        }
        imageInputStream.close();
        imageOutputStream.close();
        System.out.println("Successfully added " + outputImageFile.getAbsoluteFile() + " DPI: " + dpi);
        return ImageIO.read(outputImageFile);
    }

    public static int getDpiFromFile(File inputFile) throws IOException {
        int horizontalDPI = -1;
        int verticalDPI = -1;
        ImageInputStream stream = ImageIO.createImageInputStream(inputFile);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(stream);

            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode standardTree = (IIOMetadataNode) metadata
                    .getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode dimension = (IIOMetadataNode) standardTree.getElementsByTagName("Dimension").item(0);
            if (dimension == null) {
                // return default DPI when image missing metadata
                return LOGO_DPI;
            }
            float horizontalPixelSizeMM = getPixelSizeMM(dimension, "HorizontalPixelSize");
            float verticalPixelSizeMM = getPixelSizeMM(dimension, "VerticalPixelSize");

            System.out.println("horizontalPixelSizeMM: " + horizontalPixelSizeMM);
            System.out.println("verticalPixelSizeMM: " + verticalPixelSizeMM);

            // Convert pixelsPerMM to DPI
            horizontalDPI = calculateRoundedDPI(horizontalPixelSizeMM);
            verticalDPI = calculateRoundedDPI(verticalPixelSizeMM);

            System.out.println("horizontalDPI: " + horizontalDPI);
            System.out.println("verticalDPI: " + verticalDPI);
        }
        else {
            System.out.println("Could not read "+inputFile.getAbsolutePath());
        }
        if (horizontalDPI <= 0 || verticalDPI <= 0) {
            return LOGO_DPI;
        }
        return horizontalDPI;
    }

    private static float getPixelSizeMM(final IIOMetadataNode dimension, final String elementName) {
        NodeList pixelSizes = dimension.getElementsByTagName(elementName);
        IIOMetadataNode pixelSize = pixelSizes.getLength() > 0 ? (IIOMetadataNode) pixelSizes.item(0) : null;
        return pixelSize != null ? Float.parseFloat(pixelSize.getAttribute("value")) : -1;
    }

    private static int calculateRoundedDPI(float pixelsPerMM) {
        float dpi = 25.4f / pixelsPerMM;
        return (int) Math.ceil(dpi);
    }

    private static void deleteTempFile() throws IOException {
        Files.deleteIfExists(tempFile.toPath());
    }

    public static void makeTifFromBufferedImage(BufferedImage inputBufferedImage, String outputFile ) throws IOException {
        File outputImageFile = new File(outputFile);
        try {
            ImageIO.write(inputBufferedImage, TIF_FORMAT, outputImageFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        setImageDpi(LOGO_DPI, outputImageFile);
    }

    public static String getExtension(String fileName) {
        int dotIndex = fileName.indexOf(".");
        if (dotIndex > -1)
            return fileName.substring(dotIndex + 1);
        return fileName;
    }
}

