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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.imageio.stream.FileImageOutputStream;

public class ScaleTiff {
    
    public static void main(String[] args) throws MalformedURLException, IOException {
        File inputTif = new File("CCITT_1.TIF");
        BufferedImage readImage = ImageSupport.readImage(inputTif.toURI().toURL(), ImageMimeType.TIFF);
        BufferedImage scaled = scale(readImage,0.5);
            
        saveToFile("output.png", scaled);
    }

    public static void saveToFile(String fname, BufferedImage scaledImage) throws IOException {
        File f = new File(fname);
        f.createNewFile();
        FileImageOutputStream fios = new FileImageOutputStream(f);
        ImageSupport.writeImageToStream(scaledImage, "png", fios, 1.0f);
    }
    
    public static BufferedImage scale(BufferedImage readImage, double scaleFactor) {
        int width = readImage.getWidth();
        int height = readImage.getHeight();
        
        //double scaleFactor = 0.5;
        int scaledH = (int) (height * scaleFactor);
        int scaledW = (int) (width * scaleFactor);
        
        System.out.println(readImage);
        BufferedImage scaled = ImageSupport.scale(readImage, scaledW, scaledH);
        return scaled;
    }
}
