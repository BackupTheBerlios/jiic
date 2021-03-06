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

package de.tu_darmstadt.informatik.rbg.hatlak.joliet.impl;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660Directory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660File;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.NamingConventions;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;

public class JolietNamingConventions extends NamingConventions {
	public static boolean FORCE_DOT_DELIMITER = true;
	public static int MAX_NAME_LENGTH = 64;
	// Note: Backslash escaped for both the RegEx and Java itself
	public static final Pattern PATTERN = Pattern.compile("[*/:;?\\\\]");

	public JolietNamingConventions() {
		super("Joliet");
	}
	
	@Override
	public void apply(ISO9660Directory dir) throws HandlerException {
		// Joliet directory name restrictions:
		// Directory Identifier length (filename) <= 128 bytes (64 characters)
		// name may contain extension
		// name non-empty
		
		String filename = normalize(dir.getName());
		
		if (filename.length() > MAX_NAME_LENGTH) {
			filename = filename.substring(0, MAX_NAME_LENGTH);
		}
		
		if (filename.length()==0) {
			throw new HandlerException(getID() + ": Empty directory name encountered.");
		}
		
		setFilename(dir, filename);
	}

	@Override
	public void apply(ISO9660File file) throws HandlerException {
		// Joliet file name restrictions:
		// File Identifier length (filename + extension + overhead) <= 128 bytes (64 characters)
		// either filename or extension non-empty
		
		String filename = normalize(file.getFilename());
		String extension = normalize(file.getExtension());
		file.enforceDotDelimiter(FORCE_DOT_DELIMITER);
		
		if (filename.length()==0 && extension.length()==0) {
			throw new HandlerException(getID() + ": Empty file name encountered.");
		}
		
		if (file.enforces8plus3()) {
			if (filename.length() > 8) {
				filename = filename.substring(0, 8);
			}
			if (extension.length() > 3) {
				String mapping = getExtensionMapping(extension);
				if (mapping!=null && mapping.length() <= 3) {
					extension = normalize(mapping);
				} else {
					extension = extension.substring(0, 3);
				}
			}
		}
		
		int versionAndSeparatorsLength = (String.valueOf(file.getVersion())).length() + 2; // ;. -> 2
		if (filename.length() + extension.length() + versionAndSeparatorsLength > MAX_NAME_LENGTH) {
			if (filename.length() >= extension.length()) {
				// Shorten filename
				filename = filename.substring(0, MAX_NAME_LENGTH - (extension.length() + versionAndSeparatorsLength));
			} else {
				// Shorten extension
				extension = extension.substring(0, MAX_NAME_LENGTH - (filename.length() + versionAndSeparatorsLength));
			}
		}
		
		setFilename(file, filename, extension);
	}

	private String normalize(String name) {
		return PATTERN.matcher(name).replaceAll("_");
	}
	
	@Override
	public void addDuplicate(Map<String, Set<Integer>> duplicates, String name, int version) {
		super.addDuplicate(duplicates, name.toUpperCase(), version);
	}

	@Override
	public boolean checkFilenameEquality(String name1, String name2) {
		return name1.equalsIgnoreCase(name2);
	}

	@Override
	public void checkPathLength(String isoPath) {
		// "Remainder of ISO 9660 section 6.8.2.1": 240 Byte (120 characters)
		if (isoPath.length() > 120) {
			System.out.println(getID() + ": Path length exceeds limit: " + isoPath);
		}
	}
}
