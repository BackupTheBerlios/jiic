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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;

public abstract class NamingConventions {
	public static boolean VERBOSE = false;
	private static Map<String,String> extensionMapper;
	private String id;
	
	public NamingConventions(String id) {
		this.id = id;
		addExtensionMapping("tar.gz", "tgz");
		addExtensionMapping("tar.bz2", "tbz");
	}
	
	public String getID() {
		return id;
	}
	
	public static void addExtensionMapping(String extension, String mapping) {
		if (extensionMapper==null) {
			extensionMapper = new HashMap<String,String>();
		}
		
		if (!extensionMapper.containsKey(extension)) {
			extensionMapper.put(extension, mapping);
		}
	}
	
	public static String getExtensionMapping(String extension) {
		if (extensionMapper==null) {
			return null;
		}
		
		return extensionMapper.get(extension);
	}

	public void startRenaming(ISO9660Directory dir) {
		if (VERBOSE) {
			System.out.print(id + ": Renamed directory " + dir.getISOPath());
		}
	}

	public void startRenaming(ISO9660File file) {
		if (VERBOSE) {
			System.out.print(id + ": Renamed file " + file.getISOPath());
		}
	}

	public void endRenaming(ISO9660Directory dir) {
		if (VERBOSE) {
			System.out.println(" to " + dir.getName());
		}
	}

	public void endRenaming(ISO9660File file) {
		if (VERBOSE) {
			System.out.println(" to " + file.getFullName());
		}
	}
	
	public void setFilename(ISO9660Directory dir, String filename) {
		if (!filename.equals(dir.getName())) {
			startRenaming(dir);
			dir.setName(filename);
			endRenaming(dir);
		}
	}

	public void setFilename(ISO9660File file, String filename) {
		if (!filename.equals(file.getFilename())) {
			startRenaming(file);
			file.setFilename(filename);
			endRenaming(file);
		}
	}

	public void setFilename(ISO9660File file, String filename, String extension) {
		if (!filename.equals(file.getFilename()) || !extension.equals(file.getExtension())) {
			startRenaming(file);
			file.setFilename(filename);
			file.setExtension(extension);
			endRenaming(file);
		}
	}

	public void incrementFilename(ISO9660Directory dir) throws HandlerException {
		String filename = dir.getName();
		
		if (filename.length() > 0) {
			int[] pair = getNumericSuffix(filename);
			int number = pair[0];
			int position = pair[1];
			
			if (number >= 0) {
				// Filename ends with a number -> overwrite with incremented number
				number++;
				if (position > 0) {
					filename = filename.substring(0, position+1);
				} else {
					filename = "";
				}
				filename += number;
			} else {
				// Filename does not end with a number -> append 2
				// First try to append the number
				ISO9660Directory copy = new ISO9660Directory(filename + "2");
				apply(copy);
				if (checkFilenameEquality(copy.getName(), filename)) {
					// Adding the number did not change the filename -> replace last character
					filename = filename.substring(0, filename.length()) + "2";
				} else {
					filename = copy.getName();
				}
			}
		} else {
			filename = "2";
		}

		setFilename(dir, filename);
	}
			
	public void incrementFilename(ISO9660File file) throws HandlerException {
		String filename = file.getFilename();
		
		if (filename.length() > 0) {
			int[] pair = getNumericSuffix(filename);
			int number = pair[0];
			int position = pair[1];
			
			if (number >= 0) {
				// Filename ends with a number -> overwrite with incremented number
				number++;
				if (position > 0) {
					filename = filename.substring(0, position+1);
				} else {
					filename = "";
				}
				filename += number;
			} else {
				// Filename does not end with a number -> append 2
				// First try to append the number
				ISO9660File copy = null;
				try {
					copy = new ISO9660File(file);
				} catch (HandlerException e) {
					e.printStackTrace(System.err);
				}
				apply(copy);
				if (checkFilenameEquality(copy.getFilename(), filename)) {
					// Adding the number did not change the filename -> replace last character
					filename = filename.substring(0, filename.length()) + "2";
				} else {
					filename = copy.getFilename();
				}
			}
		} else {
			filename = "2";
		}

		setFilename(file, filename);
	}
	
	static private int[] getNumericSuffix(String filename){
		int number = -1;
		int position = filename.length() - 1;
		
		while (position >=0 && Character.isDigit(filename.charAt(position))) {
			position--;
		}
		if (position < filename.length() - 1) {
			String substring = filename.substring(position + 1, filename.length());
			if (substring.length() < 10) {
				number = Integer.parseInt(substring);
			}
		}
		return new int[] {number, position};
    }

	public boolean checkFilenameEquality(String name1, String name2) {
		return name1.equals(name2);
	}
	
	public void processDirectory(ISO9660Directory dir) throws HandlerException {
		Map<String, Set<Integer>> duplicates = new HashMap<String, Set<Integer>>();
		
		// Prepare files and directories to be processed in sorted order
        List<ISO9660Directory> dirs = dir.getDirectories();
        List<ISO9660File> files = dir.getFiles();

		List<ISO9660HierarchyObject> contents = new ArrayList<ISO9660HierarchyObject>(dirs.size() + files.size());
		contents.addAll(dirs);
		contents.addAll(files);
		Collections.sort(contents);

		Iterator<ISO9660HierarchyObject> it = contents.iterator();
		while (it.hasNext()) {
			boolean duplicate = false;
			ISO9660HierarchyObject object = it.next();
			if (object instanceof ISO9660Directory) {
				ISO9660Directory subdir = (ISO9660Directory) object;
				apply(subdir);
				while (checkDuplicate(duplicates, subdir.getName(), -1)) {
					incrementFilename(subdir);
					duplicate = true;
				}
				if (!duplicate) {
					duplicates.clear();
				}
				addDuplicate(duplicates, subdir.getName(), -1);
				checkPathLength(subdir.getISOPath());
			} else if (object instanceof ISO9660File){
				ISO9660File file = (ISO9660File) object;
				apply(file);
				while (checkDuplicate(duplicates, file.getName(), file.getVersion())) {
					incrementFilename(file);
					duplicate = true;
				}
				if (!duplicate) {
					duplicates.clear();
				}
				addDuplicate(duplicates, file.getName(), file.getVersion());
				checkPathLength(file.getISOPath());
			} else {
				throw new HandlerException("Neither file nor directory: " + object);
			}
		}
	}

	public boolean checkDuplicate(Map<String, Set<Integer>> duplicates, String name, int version) {
		return checkDuplicate(duplicates, name, version, true);
	}

	public boolean checkDuplicate(Map<String, Set<Integer>> duplicates, String name, int version, boolean checkVersion) {
		Set<Integer> versions = duplicates.get(name);
		if(versions == null) {
			return false;
		}
		return ! checkVersion || versions.contains(-1) || versions.contains(version);
	}

	public void addDuplicate(Map<String, Set<Integer>> duplicates, String name, int version) {
		Set<Integer> set = duplicates.get(name);
		if(set == null){
			set = new HashSet<Integer>(1, 1.0f);
			duplicates.put(name, set);
		}
		set.add(version);
	}

	public abstract void apply(ISO9660Directory dir) throws HandlerException;

	public abstract void apply(ISO9660File file) throws HandlerException;
	
	public abstract void checkPathLength(String isoPath);
}
