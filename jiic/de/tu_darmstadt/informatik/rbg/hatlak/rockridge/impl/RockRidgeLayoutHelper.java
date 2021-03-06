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

package de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.FilenameDataReference;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660Directory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660File;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660MovedDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660RootDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.LayoutHelper;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.StreamHandler;

public class RockRidgeLayoutHelper extends LayoutHelper {
	private ISO9660RootDirectory rripRoot;
	/** Map files by either ID or ISO9660File */
	private Map<Object,ISO9660File> fileMapper;
	/** Map directories by ID */
	private Map<Object,ISO9660Directory> directoryMapper;
	
	public RockRidgeLayoutHelper(StreamHandler streamHandler, ISO9660RootDirectory isoRoot, ISO9660RootDirectory rripRoot) {
		super(streamHandler, isoRoot, new RockRidgeNamingConventions());
		this.rripRoot = rripRoot;
		setup(isoRoot);
	}
	
	private void setup(ISO9660RootDirectory isoRoot) {
		// Lookup tables mapping files and directories between hierarchies
		// (ISO 9660 -> Rock Ridge)
		int fileCount = isoRoot.deepFileCount() + 1;
		fileMapper = new HashMap<Object,ISO9660File>(fileCount);
		int dirCount = isoRoot.deepDirCount() + 1;
		directoryMapper = new HashMap<Object,ISO9660Directory>(dirCount);
		
		// Root files (root itself does not have to be mapped)
		Iterator<ISO9660File> isoFit = isoRoot.getFiles().iterator();
		Iterator<ISO9660File> rripFit = rripRoot.getFiles().iterator();
		while (isoFit.hasNext()) {
			ISO9660File isoFile = isoFit.next();
			ISO9660File rripFile = rripFit.next();
			fileMapper.put(isoFile, rripFile);
		}		
		
		// Subdirectories:
		// Since rripRoot and isoRoot are just a deep copy of the same
		// root at this point, simultaneous iteration can be applied here
		Iterator<ISO9660Directory> isoIt = isoRoot.unsortedIterator();
		Iterator<ISO9660Directory> rripIt = rripRoot.unsortedIterator();
		while (isoIt.hasNext()) {
			ISO9660Directory isoDir = isoIt.next();
			ISO9660Directory rripDir = rripIt.next();
			directoryMapper.put(isoDir.getID(), rripDir);
			
			isoFit = isoDir.getFiles().iterator();
			rripFit = rripDir.getFiles().iterator();
			while (isoFit.hasNext()) {
				ISO9660File isoFile = isoFit.next();
				ISO9660File rripFile = rripFit.next();
				fileMapper.put(isoFile.getID(), rripFile);
			}
		}
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660Directory dir) throws HandlerException {
		return new RockRidgeFilenameDataReference(matchDirectory(dir));
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660File file) throws HandlerException {
		return new RockRidgeFilenameDataReference(matchFile(file));
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660MovedDirectory moved) throws HandlerException {
		return new RockRidgeFilenameDataReference(matchFile(moved));
	}
	
	public FilenameDataReference getFilenameDataReference(String name) throws HandlerException {
		return new RockRidgeFilenameDataReference(name);
	}

	public ISO9660Directory matchDirectory(ISO9660Directory dir) {
		if (dir==dir.getRoot()) {
			return rripRoot;
		}
		
		if (dir==dir.getRoot().getMovedDirectoriesStore()) {
			return rripRoot.getMovedDirectoriesStore();
		}
		
		ISO9660Directory rripDir = directoryMapper.get(dir.getID());
		if (rripDir!=null) {
			return rripDir;
		}
		
		throw new RuntimeException("No matching directory found for " + dir.getISOPath());
	}

	public ISO9660File matchFile(ISO9660File file) {		
		ISO9660File rripFile = fileMapper.get(file.getID());
		if (rripFile!=null) {
			return rripFile;
		}
		
		throw new RuntimeException("No matching file found for " + file.getISOPath());
	}
	
	public ISO9660File matchFile(ISO9660MovedDirectory moved) {		
		ISO9660File rripFile = fileMapper.get(moved.getID());
		if (rripFile!=null) {
			return rripFile;
		}
		
		throw new RuntimeException("No matching file found for " + moved.getISOPath());
	}

	@Override
	public byte[] pad(String string, int targetByteLength)
			throws HandlerException {
		// Unused
		return null;
	}
}
