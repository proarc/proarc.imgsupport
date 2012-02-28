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
package cz.incad.tiffsup;

public enum ImageMimeType {

	JPEG("image/jpeg","jpg", true, false, false),
	PNG("image/png","png", true, false, false),
	JPEG2000("image/jp2","jp2",true, false ,false),
	
	XDJVU("image/x.djvu","djvu", false, false, true),
	VNDDJVU("image/vnd.djvu","djvu", false, false, true),
	DJVU("image/djvu","djvu", false, false, true),
	PDF("application/pdf","pdf",false, false, true),
	
	TIFF("image/tif","tiff", false, true, false);
	
	private String value;
	private boolean supportedbyJava;
	private boolean supportedbyJAI;
	private boolean multipageFormat;
	private String defaultFileExtension;
	
	private ImageMimeType(String value, String defaultExtension, boolean javasupport, boolean jaiSupport, boolean multipageformat) {
		this.value = value;
		this.supportedbyJava = javasupport;
		this.supportedbyJAI = jaiSupport;
		this.multipageFormat = multipageformat;
		this.defaultFileExtension = defaultExtension;
	}

	public String getValue() {
		return value;
	}

	public boolean javaNativeSupport() {
		return supportedbyJava;
	}
	
	public boolean isMultipageFormat()  {
		return this.multipageFormat;
	}
	
	
	
	
	public boolean isSupportedbyJAI() {
        return supportedbyJAI;
    }

    public void setSupportedbyJAI(boolean supportedbyJAI) {
        this.supportedbyJAI = supportedbyJAI;
    }

    public String getDefaultFileExtension() {
        return defaultFileExtension;
    }

    public static ImageMimeType loadFromMimeType(String mime) {
		ImageMimeType[] values = values();
		for (ImageMimeType iType : values) {
			if (iType.getValue().equals(mime)) return iType;
		}
		return null;
	}
}
