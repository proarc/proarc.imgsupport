/*
 * Copyright (C) 2010 Pavel Stastny
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.imgsupport;

import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuOptions;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvubean.DjVuImage;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JPanel;

public final class ImageSupport {

    private static final Logger LOGGER = Logger.getLogger(ImageSupport.class.getName());

    static {
        // disable djvu convertor verbose logging
        DjVuOptions.out = new java.io.PrintStream(new java.io.OutputStream() {

            public void write(int b) {
            }
        });
    }

    public static BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    public static BufferedImage readImage(URL url, ImageMimeType type) throws IOException {
        LOGGER.fine("type is " + type);
        if (type == null) {
            return null;
        }
        if (type.javaNativeSupport()) {
            InputStream stream = url.openStream();
            return ImageIO.read(stream);
        } else if (type.isSupportedbyJAI()) {
            InputStream stream = url.openStream();
            ImageDecoder decoder = ImageCodec.createImageDecoder(type.getDefaultFileExtension(), url.openStream(), null);
            if (decoder != null) {
                RenderedImage decodedImage = decoder.decodeAsRenderedImage();
                if (decodedImage instanceof BufferedImage) {
                    return (BufferedImage) decodedImage;
                } else {
                    return convertRenderedImage(decodedImage);
                }
            } else {
                throw new IllegalArgumentException("no jai decoder for type '" + type.getValue() + "'");
            }

        } else if ((type.equals(ImageMimeType.DJVU)) || (type.equals(ImageMimeType.VNDDJVU)) || (type.equals(ImageMimeType.XDJVU))) {
            com.lizardtech.djvu.Document doc = new com.lizardtech.djvu.Document(url);
            doc.setAsync(false);
            DjVuPage[] p = new DjVuPage[1];
            p[0] = doc.getPage(0 /* only page 0 */, 1, true);
            p[0].setAsync(false);

            DjVuImage djvuImage = new DjVuImage(p, true);
            Rectangle pageBounds = djvuImage.getPageBounds(0);

            Image[] images = djvuImage.getImage(new JPanel(), new Rectangle(pageBounds.width, pageBounds.height));
            if (images.length == 1) {
                Image img = images[0];
                if (img instanceof BufferedImage) {
                    return (BufferedImage) img;
                } else {
                    return toBufferedImage(img);
                }
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("unsupported mimetype '" + type.getValue() + "'");
        }
    }

    // public static void writeImageToStream(BufferedImage image, String
    // javaFormat, OutputStream os) throws IOException {
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // ImageIO.write(image, javaFormat, bos);
    // IOUtils.copyStreams(new ByteArrayInputStream(bos.toByteArray()), os);
    // }

    public static Dimension readDimension(URL url, ImageMimeType type) throws IOException {
        if (type.javaNativeSupport()) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(type.getDefaultFileExtension());
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                ImageInputStream istream = ImageIO.createImageInputStream(url.openStream());
                reader.setInput(istream);
                int height = reader.getHeight(0);
                int width = reader.getWidth(0);
                return new Dimension(width, height);
            } else {
                return null;
            }
        } else {
            com.lizardtech.djvu.Document doc = new com.lizardtech.djvu.Document(url);
            doc.setAsync(false);
            DjVuPage page = doc.getPage(0, 1, true);
            DjVuInfo info = page.getInfoWait();
            DjVuImage djvuImage = new DjVuImage(new DjVuPage[] { page }, true);
            Rectangle pageBounds = djvuImage.getPageBounds(0);
            System.out.println(pageBounds);
            System.out.println(new Dimension(info.width, info.height));

            return new Dimension(info.width, info.height);
        }
    }

    public static void writeImageToStream(BufferedImage scaledImage, String javaFormat, FileImageOutputStream os, float quality) throws IOException {

        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(javaFormat);
        if (iter.hasNext()) {
            ImageWriter writer = iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            String compType = iwp.getCompressionType();
            System.out.println(compType);
            // iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // iwp.setCompressionQuality(quality); // an integer between 0 and 1
            writer.setOutput(os);
            IIOImage image = new IIOImage(scaledImage, null, null);
            writer.write(null, image, iwp);
            writer.dispose();

        } else {
            throw new IOException("No writer for format '" + javaFormat + "'");
        }

    }

    public static BufferedImage scale(BufferedImage img, int targetWidth, int targetHeight) {
        ScalingMethod method = ScalingMethod.valueOf("BICUBIC_STEPPED");
        boolean higherQuality = true;
        return scale(img, targetWidth, targetHeight, method, higherQuality);
    }

    public static BufferedImage scale(BufferedImage img, int targetWidth, int targetHeight, ScalingMethod method, boolean higherQuality) {
        // System.out.println("SCALE:"+method+" width:"+targetWidth+" height:"+targetHeight);
        switch (method) {
        case REPLICATE:
            Image rawReplicate = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_REPLICATE);
            if (rawReplicate instanceof BufferedImage) {
                return (BufferedImage) rawReplicate;
            } else {
                return toBufferedImage(rawReplicate);
            }
        case AREA_AVERAGING:
            Image rawAveraging = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
            if (rawAveraging instanceof BufferedImage) {
                return (BufferedImage) rawAveraging;
            } else {
                return toBufferedImage(rawAveraging);
            }
        case BILINEAR:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, higherQuality);
        case BICUBIC:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, higherQuality);
        case NEAREST_NEIGHBOR:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, higherQuality);
        case BILINEAR_STEPPED:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, higherQuality);
        case BICUBIC_STEPPED:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, higherQuality);
        case NEAREST_NEIGHBOR_STEPPED:
            return getScaledInstanceJava2D(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, higherQuality);
        }
        return null;
    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     *
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to
     *            {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique
     *            that provides higher quality than the usual one-step technique
     *            (only useful in downscaling cases, where {@code targetWidth}
     *            or {@code targetHeight} is smaller than the original
     *            dimensions, and generally only when the {@code BILINEAR} hint
     *            is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    private static BufferedImage getScaledInstanceJava2D(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w > targetWidth || h > targetHeight);

        return ret;
    }

    public static GraphicsConfiguration getDefaultConfiguration() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration();
    }

    public static BufferedImage getScaledInstanceJava2D(BufferedImage image, int targetWidth, int targetHeight, Object hint, GraphicsConfiguration gc) {

        // if (gc == null)
        // gc = getDefaultConfiguration();
        int w = image.getWidth();
        int h = image.getHeight();

        int transparency = image.getColorModel().getTransparency();
        // BufferedImage result = gc.createCompatibleImage(w, h, transparency);
        BufferedImage result = new BufferedImage(w, h, transparency);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        double scalex = (double) targetWidth / image.getWidth();
        double scaley = (double) targetHeight / image.getHeight();
        AffineTransform xform = AffineTransform.getScaleInstance(scalex, scaley);
        g2.drawRenderedImage(image, xform);

        g2.dispose();
        return result;
    }

    public static BufferedImage toBufferedImage(Image img) {
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g = bufferedImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bufferedImage;
    }

    public static enum ScalingMethod {
        REPLICATE, AREA_AVERAGING, BILINEAR, BICUBIC, NEAREST_NEIGHBOR, BILINEAR_STEPPED, BICUBIC_STEPPED, NEAREST_NEIGHBOR_STEPPED
    }

}
