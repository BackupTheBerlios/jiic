/*  
 *  JIIC: Java ISO Image Creator. Copyright (C) 2007, Jens Hatlak <hatlak@rbg.informatik.tu-darmstadt.de>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package de.tu_darmstadt.informatik.rbg.hatlak.iso9660;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.impl.ISO9660Constants;
import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.POSIXFileMode;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class ISO9660File implements ISO9660HierarchyObject {
	public static final Pattern FILEPATTERN = Pattern.compile("^([^.]+)\\.(.+)$");
	private boolean enforceDotDelimiter = false;
	private static final long serialVersionUID = 1L;
	private String filename, extension;
	private int version;
	private boolean enforce8plus3;
	private ISO9660Directory parent;
	private Object id;
	private File file;
	private POSIXFileMode filemode;

	private String cachedName = null;

	/**
	 * Create file from File object
	 * 
	 * @param file File
	 * @param version File version
	 * @throws HandlerException Invalid File version or file is a directory
	 */
	public ISO9660File(File file, int version) throws HandlerException {
		this.file = file;
		setName(file.getName());
		setVersion(version);
		id = new Object();
		enforce8plus3 = false;
		
		if (isDirectory()) {
			throw new HandlerException("Cannot wrap directory in " + getClass() + ": " + file.getAbsolutePath());
		}		
	}

	/**
	 * Create file from File object
	 * 
	 * @param pathname File
	 * @param version File version
	 * @throws HandlerException Invalid File version or file is a directory
	 */
	public ISO9660File(String pathname, int version) throws HandlerException {
		this.file = new File(pathname);
		setName(this.file.getName());
		setVersion(version);
		id = new Object();
		enforce8plus3 = false;
		
		if (isDirectory()) {
			throw new HandlerException("Cannot wrap directory in " + getClass() + ": " + file.getAbsolutePath());
		}
	}

	/**
	 * Create File
	 * 
	 * @param file File
	 * @throws HandlerException File is a directory
	 */
	public ISO9660File(ISO9660File file) throws HandlerException {
		this(file.file);
		setFileMode(file.getFileMode());
	}
	
	/**
	 * Create File
	 * 
	 * @param file File
	 * @throws HandlerException File is a directory
	 */
	public ISO9660File(File file) throws HandlerException {
		this(file, 1);
	}

	/**
	 * Create File
	 * 
	 * @param pathname File
	 * @throws HandlerException File is a directory
	 */
	public ISO9660File(String pathname) throws HandlerException {
		this(pathname, 1);
	}

	/**
	 * Returns the name of the file (without dot)
	 * 
	 * @return File name
	 */
	public String getFilename() {
		if (enforce8plus3) {
			return filename.substring(0, 8);
		}
		return filename;
	}
	
	/**
	 * Returns the extension of the file (without front dot)
	 * 
	 * @return File extension
	 */
	public String getExtension() {
		if (enforce8plus3) {
			return extension.substring(0, 3);
		}
		return extension;
	}
	
	public String getName() {
		if (this.cachedName == null) {
			if (!extension.equals("") || enforceDotDelimiter) {
				this.cachedName = new StringBuilder(100).append(filename).append(".").append(extension).toString();
			} else{
				this.cachedName = filename;
			}	
		}
		return this.cachedName;
    }

	/**
	 * Set the name of the file (without dot)
	 * 
	 * @param filename File name
	 */
	public void setFilename(String filename) {
                this.cachedName = null;
		this.filename = filename;
		if (parent!=null) {
			parent.forceSort();
		}
	}
	
	/**
	 * Set the extension of the file (without front dot)
	 * 
	 * @param extension File extension
	 */
	public void setExtension(String extension) {
                this.cachedName = null;
		this.extension = extension;
		if (parent!=null) {
			parent.forceSort();
		}
	}
	
	public void setName(String name) {
                this.cachedName = null;
		Matcher m = ISO9660File.FILEPATTERN.matcher(name);
		if (m.matches()) {
			filename = m.group(1);
			extension = m.group(2);
		} else {
			filename = name;
			extension = "";
		}

		if (parent!=null) {
			parent.forceSort();
		}
	}
	
	/**
	 * Returns the full ISO 9660 filename, i.e. with file version
	 * 
	 * @return Full ISO 9660 file name
	 */
	public String getFullName() {
		return new StringBuilder(100).append(getName()).append(";").append(getVersion()).toString();
	}

	/**
	 * Returns the file version
	 * 
	 * @return File version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Set file version
	 * 
	 * @param version File version
	 * @throws HandlerException Invalid file version
	 */
	public void setVersion(int version) throws HandlerException {
		if (version < 1 || version > ISO9660Constants.MAX_FILE_VERSION) {
			throw new HandlerException("Invalid file version: " + version);
		}
		this.version = version;

		if (parent!=null) {
			parent.forceSort();
		}
	}

	public void setFileMode(POSIXFileMode filemode) {
		this.filemode = filemode;
	}
	
	public POSIXFileMode getFileMode() {
		return this.filemode;
	}
	
	/**
	 * Returns whether this file's name is forced to be recorded "8+3"
	 * 
	 * @return Whether this file name will be short
	 */
	public boolean enforces8plus3() {
		return enforce8plus3;
	}
	
	/**
	 * Force short filename ("8+3")
	 * 
	 * @param force Whether to force this file's name to be short
	 */
	public void enforce8plus3(boolean force) {
		this.enforce8plus3 = force;
	}

	/**
	 * Returns whether this file's name is forced to include the dot character
	 * 
	 * @return Whether the dot character is enforced
	 */
	public boolean enforcesDotDelimiter() {
		return enforceDotDelimiter;
	}
	
	/**
	 * Force dot character
	 * 
	 * @param force Whether to force this file's name to include the dot character
	 */
	public void enforceDotDelimiter(boolean force) {
                this.cachedName = null;
		this.enforceDotDelimiter = force;
	}

	public int compareTo(ISO9660HierarchyObject object) throws ClassCastException, NullPointerException {
		// Alphanumerical case-insensitive sort (according to ISO9660 needs)
		if (object==null) {
			throw new NullPointerException();
		} else
		if (object instanceof ISO9660File) {
			ISO9660File file = (ISO9660File) object;

			if (getName().equalsIgnoreCase(file.getName())) {
				// Same name -> ensure descending version order (see ISO9660:9.3)
				if (version > file.getVersion()) {
					// This version is greater -> This file comes first 
					return -1;
				} else
				if (version < file.getVersion()) {
					// This version is smaller -> This file comes last
					return 1;
				} // else: versions are equal -> file will be renamed later
			} // else: Compare filenames
			
                        int test = getFilename().compareToIgnoreCase(file.getFilename());

			if (test!=0) {
				// Different filenames -> no need to check extension 
				return test;
			} // else: Compare extensions
			
                        return getExtension().compareToIgnoreCase(file.getExtension());
		} else
		if (object instanceof ISO9660Directory) {
			ISO9660Directory dir = (ISO9660Directory) object;
                        return getFullName().compareToIgnoreCase(dir.getName());
		} else {
			throw new ClassCastException();			
		}		
	}
	
	@Override
	public boolean equals(Object toCompare) {
		if (toCompare instanceof ISO9660File) {
			return ((ISO9660File) toCompare).getContentID().equals(getContentID());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	void setParentDirectory(ISO9660Directory parent) {
		this.parent = parent;
	}
	
	public ISO9660Directory getParentDirectory() {
		return parent;
	}
	
	public String getISOPath() throws NullPointerException {
		if (parent==null) {
			throw new NullPointerException("Cannot determine path without parent directory.");
		}
		return parent.getISOPath() + File.separator + getFullName();
	}

	public Object getID() {
		// Identification of the ISO9660File, survives cloning
		return id;
	}
	
	/**
	 * Returns and identification of the File underlying this object
	 * 
	 * @return Content identification Object
	 */
	public Object getContentID() {
		// Identification of the underlying File, may be shared across ISO9660Files 
		return new Integer(hashCode());
	}
	
	public ISO9660RootDirectory getRoot() throws NullPointerException {
		if (getParentDirectory()==null) {
			throw new NullPointerException("Cannot determine root without parent directory.");
		}
		return getParentDirectory().getRoot();
	}
	
	public boolean isDirectory() {
		return file.isDirectory();
	}
	
	public long length() {
		return file.length();
	}
	
	public long lastModified() {
		return file.lastModified();
	}
	
	public File getAbsoluteFile() {
		return file.getAbsoluteFile();
	}

	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}
	
	public File getFile() {
		return file;
	}
	
	@Override
	public ISO9660File clone() {
		ISO9660File clone = null;
		try {
			clone = (ISO9660File) super.clone();
			clone.id = id;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}
}
