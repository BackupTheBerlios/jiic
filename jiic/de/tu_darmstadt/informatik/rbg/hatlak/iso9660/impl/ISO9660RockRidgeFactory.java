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

package de.tu_darmstadt.informatik.rbg.hatlak.iso9660.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.FilenameDataReference;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660Directory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660File;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660RootDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.LayoutHelper;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.NamingConventions;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.StandardConfig;
import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.POSIXFileMode;
import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.RRIPFactory;
import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.RockRidgeLayoutHelper;
import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.RockRidgeNamingConventions;
import de.tu_darmstadt.informatik.rbg.hatlak.sabre.impl.BothWordDataReference;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.DataReference;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.Fixup;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.StreamHandler;

public class ISO9660RockRidgeFactory extends ISO9660Factory {
	private RRIPFactory rripFactory;
	private LinkedList<UnfinishedNMEntry> unfinishedNMEntries;
	private RockRidgeLayoutHelper helper;
	private ISO9660RootDirectory rripRoot;
	private Map<ISO9660Directory, Collection<ISO9660Directory>> originalParentMapper;
	private Map<ISO9660Directory, Fixup> parentLocationFixups;
	private Map<ISO9660Directory, Integer> parentLocations;
	private Map<ISO9660Directory, Fixup> childLocationFixups;
	private Map<ISO9660Directory, Integer> childLocations;
	
	public ISO9660RockRidgeFactory(StreamHandler streamHandler, StandardConfig config, LayoutHelper helper, ISO9660RootDirectory root, ISO9660RootDirectory isoRoot, HashMap volumeFixups) {
		super(streamHandler, config, helper, isoRoot, volumeFixups);
		this.rripFactory = new RRIPFactory(streamHandler);
		this.unfinishedNMEntries = new LinkedList<UnfinishedNMEntry>();
		
		// Use a copy of the original root for Rock Ridge
		rripRoot = (ISO9660RootDirectory) root.clone();
		this.helper = new RockRidgeLayoutHelper(streamHandler, isoRoot, rripRoot);
		
		originalParentMapper = new HashMap<ISO9660Directory, Collection<ISO9660Directory>>();
	}

	@Override
	public void applyNamingConventions() throws HandlerException {
		super.applyNamingConventions();

		NamingConventions namingConventions = helper.getNamingConventions();
		namingConventions.processDirectory(rripRoot);
		
		Iterator<ISO9660Directory> sit = rripRoot.unsortedIterator();
		while (sit.hasNext()) {
			ISO9660Directory dir = sit.next();
			namingConventions.processDirectory(dir);
		}
	}
	
	@Override
	public void relocateDirectories() {
		if (rripRoot.deepLevelCount()>=8) {
			parentLocationFixups = new HashMap<ISO9660Directory, Fixup>();
			parentLocations = new HashMap<ISO9660Directory, Integer>();
			childLocationFixups = new HashMap<ISO9660Directory, Fixup>();
			childLocations = new HashMap<ISO9660Directory, Integer>();
			rripRoot.setMovedDirectoryStore();
			
			if (RockRidgeNamingConventions.HIDE_MOVED_DIRECTORIES_STORE
				&& !rripRoot.getMovedDirectoriesStore().getName().startsWith(".")) {
				// Hide Moved Directories Store for Rock Ridge
				rripRoot.getMovedDirectoriesStore().setName("." +
						ISO9660RootDirectory.MOVED_DIRECTORIES_STORE_NAME);
			}
		}

		super.relocateDirectories();
	}

	@Override
	ISO9660Directory relocate(ISO9660Directory dir) {
		ISO9660Directory originalParent = super.relocate(dir);
		
		helper.matchDirectory(dir).relocate();
		
		if (dir.getRoot()==root) {
			// Save only mappings from ISO 9660 hierarchy
			if (! originalParentMapper.containsKey(originalParent)) {
				originalParentMapper.put(originalParent, new Vector<ISO9660Directory>());
			}
			Collection<ISO9660Directory> children = originalParentMapper.get(originalParent);
			children.add(dir);
		}
		
		return originalParent;
	}

	@Override
	public void doDRA() throws HandlerException {
		super.doDRA();
		if (originalParentMapper.size() > 0) {
			doRelocationFixups();
		}
		doCA();
	}

	@Override
	void doDir(ISO9660Directory dir, Map<ISO9660Directory, ParentInfo> parentMapper) throws HandlerException {
		Integer location = new Integer(helper.getCurrentLocation());
		
		if (originalParentMapper.containsKey(dir)) {
			// Remember directory locations for PL Location Fixup
			Collection<ISO9660Directory> children = originalParentMapper.get(dir);
			for (ISO9660Directory child : children) {
				parentLocations.put(child, location);
			}
		} else
		if (dir.isMoved()) {
			// Remember directory location for CL Location Fixup
			childLocations.put(dir, location);
		}
		
		super.doDir(dir, parentMapper);
	}
		
	@Override
	HashMap doFakeDR(ISO9660Directory dir) throws HandlerException {
		long position = streamHandler.mark();
		HashMap memory = super.doFakeDR(dir);

		if (RRIPFactory.MKISOFS_COMPATIBILITY) {
			// RR: Recorded Fields
			int flags = RRIPFactory.RR_PX_RECORDED | RRIPFactory.RR_NM_RECORDED | RRIPFactory.RR_CL_RECORDED;
			rripFactory.doRREntry(flags);
		}

		// PX: POSIX File Attributes
		POSIXFileMode fileMode = dir.getFileMode();
		if (fileMode == null) fileMode = POSIXFileMode.DIRECTORY_DEFAULT;
		int fileLinks = 2 + dir.getDirectories().size();
		rripFactory.doPXEntry(fileMode.getFileMode(), fileLinks, 0, 0, 1);

		// CL: Child link (location of the actual directory record)
		childLocationFixups.put(dir, rripFactory.doCLEntry());

		// Compute length up to here
		int length = helper.getDifferenceTo(position);

		// NM: Alternate Name
		length = doNM(helper.getFilenameDataReference(dir), length);
		
		// Update Directory Record Length
		return finalizeDR(memory, length);
	}
	
	@Override
	HashMap doDR(ISO9660File file) throws HandlerException {
		long position = streamHandler.mark();
		HashMap memory = super.doDR(file);
		
		if (RRIPFactory.MKISOFS_COMPATIBILITY) {
			// RR: Recorded Fields
			int flags = RRIPFactory.RR_PX_RECORDED | RRIPFactory.RR_TF_RECORDED | RRIPFactory.RR_NM_RECORDED;
			rripFactory.doRREntry(flags);
		}

		// PX: POSIX File Attributes
		POSIXFileMode fileMode = file.getFileMode();
		if (fileMode == null) fileMode = POSIXFileMode.FILE_DEFAULT;
		rripFactory.doPXEntry(fileMode.getFileMode(), 1, 0, 0, 1);
		
		// TF: Timestamp
		ISO9660ShortDateDataReference date = new ISO9660ShortDateDataReference(file.lastModified());
		rripFactory.doTFEntry(RRIPFactory.TF_MODIFY, date);

		// Compute length up to here
		int length = helper.getDifferenceTo(position);

		// NM: Alternate Name
		length = doNM(helper.getFilenameDataReference(file), length);
		
		// Update Directory Record Length
		return finalizeDR(memory, length);
	}
	
	@Override
	HashMap doDR(ISO9660Directory dir) throws HandlerException {
		long position = streamHandler.mark();
		HashMap memory = super.doDR(dir);
		
		if (RRIPFactory.MKISOFS_COMPATIBILITY) {
			// RR: Recorded Fields
			int flags = RRIPFactory.RR_PX_RECORDED | RRIPFactory.RR_TF_RECORDED | RRIPFactory.RR_NM_RECORDED;
			if (dir.isMoved()) {
				flags |= RRIPFactory.RR_RE_RECORDED;
			}
			rripFactory.doRREntry(flags);
		}
		
		if (dir.isMoved()) {
			// RE: Directory has been relocated (moved)
			rripFactory.doREEntry();
		}

		// PX: POSIX File Attributes
		POSIXFileMode fileMode = dir.getFileMode();
		if (fileMode == null) fileMode = POSIXFileMode.DIRECTORY_DEFAULT;
		int fileLinks = 2 + dir.getDirectories().size();
		rripFactory.doPXEntry(fileMode.getFileMode(), fileLinks, 0, 0, 1);
		
		// TF: Timestamp
		ISO9660ShortDateDataReference date = new ISO9660ShortDateDataReference(dir.lastModified());
		rripFactory.doTFEntry(RRIPFactory.TF_MODIFY, date);

		// Compute length up to here
		int length = helper.getDifferenceTo(position);

		// NM: Alternate Name
		length = doNM(helper.getFilenameDataReference(dir), length);

		// Update Directory Record Length
		return finalizeDR(memory, length);
	}

	@Override
	HashMap doDotDR(ISO9660Directory dir) throws HandlerException {
		long position = streamHandler.mark();
		HashMap memory = super.doDotDR(dir);
		
		if (dir==root) {
			// SP: SUSP Indicator
			rripFactory.doSPEntry(0);
		}
		
		if (RRIPFactory.MKISOFS_COMPATIBILITY) {
			// RR: Recorded Fields
			int flags = RRIPFactory.RR_PX_RECORDED | RRIPFactory.RR_TF_RECORDED;
			rripFactory.doRREntry(flags);
		}

		// PX: POSIX File Attributes
		POSIXFileMode fileMode = dir.getFileMode();
		if (fileMode == null) fileMode = POSIXFileMode.DIRECTORY_DEFAULT;
		int fileLinks = 2 + dir.getDirectories().size();
		rripFactory.doPXEntry(fileMode.getFileMode(), fileLinks, 0, 0, 1);
		
		// TF: Timestamp
		ISO9660ShortDateDataReference date = new ISO9660ShortDateDataReference(dir.lastModified());
		rripFactory.doTFEntry(RRIPFactory.TF_MODIFY, date);

		if (dir==root) {
			// CE: Continuation Area for RRIP ER
			HashMap ceMemory = rripFactory.doCEEntry();
			volumeFixups.put("rripERLocationFixup", ceMemory.get("ceLocationFixup"));
			volumeFixups.put("rripERLengthFixup", ceMemory.get("ceLengthFixup"));
			
			// Write and close ER Offset Fixup
			Fixup rripEROffsetFixup = (Fixup) ceMemory.get("ceOffsetFixup");
			rripEROffsetFixup.data(new BothWordDataReference(0));
			rripEROffsetFixup.close();
		}
		
		// Update Directory Record Length
		return finalizeDR(memory, helper.getDifferenceTo(position));
	}

	@Override
	HashMap doDotDotDR(ISO9660Directory dir) throws HandlerException {
		long position = streamHandler.mark();
		HashMap memory = super.doDotDotDR(dir);
		ISO9660Directory parentDir = dir.getParentDirectory();
		
		if (RRIPFactory.MKISOFS_COMPATIBILITY) {
			// RR: Recorded Fields
			int flags = RRIPFactory.RR_PX_RECORDED | RRIPFactory.RR_TF_RECORDED;
			if (dir.isMoved()) {
				flags |= RRIPFactory.RR_PL_RECORDED;
			}
			rripFactory.doRREntry(flags);
		}

		if (dir.isMoved()) {
			// PL: Real Parent of this relocated directory
			parentLocationFixups.put(dir, rripFactory.doPLEntry());			
		}
		
		// PX: POSIX File Attributes
		POSIXFileMode fileMode = parentDir.getFileMode();
		if (fileMode == null) fileMode = POSIXFileMode.DIRECTORY_DEFAULT;
		int fileLinks = 2 + parentDir.getDirectories().size();
		rripFactory.doPXEntry(fileMode.getFileMode(), fileLinks, 0, 0, 1);
		
		// TF: Timestamp
		ISO9660ShortDateDataReference date = new ISO9660ShortDateDataReference(parentDir.lastModified());
		rripFactory.doTFEntry(RRIPFactory.TF_MODIFY, date);

		// Update Directory Record Length
		return finalizeDR(memory, helper.getDifferenceTo(position));
	}
	
	private int doNM(FilenameDataReference filename, int drLength) throws HandlerException {
		int lengthToAdd = RRIPFactory.NM_ENTRY_LENGTH;
		// Note: Since DR length must be an even number (see ISO 9660 section 9.1.13),
		// a DR length of 255 would be changed to 256 which does not fit into a byte
		int rest = 254 - drLength;
		if (rest >= filename.getLength() + RRIPFactory.NM_ENTRY_LENGTH) {
			// Filename fits into this System Use Area
			rripFactory.doNMEntry(0, filename);
			lengthToAdd += filename.getLength();
		} else {
			// Filename exceeds space left -> Continuation Area needed
			int prefixLength = rest - (RRIPFactory.NM_ENTRY_LENGTH + RRIPFactory.CE_ENTRY_LENGTH);
			String name = filename.getName();
			DataReference filenameRest = helper.getFilenameDataReference(name.substring(0, prefixLength));
			rripFactory.doNMEntry(RRIPFactory.NM_CONTINUES, filenameRest);
			
			// Construct CE Entry to continue filename in Continuation Area
			HashMap ceMemory = rripFactory.doCEEntry();
			UnfinishedNMEntry unfinishedNMEntry = new UnfinishedNMEntry();
			unfinishedNMEntry.location = (Fixup) ceMemory.get("ceLocationFixup");
			unfinishedNMEntry.offset = (Fixup) ceMemory.get("ceOffsetFixup");
			unfinishedNMEntry.length = (Fixup) ceMemory.get("ceLengthFixup");
			unfinishedNMEntry.filenameRest = name.substring(prefixLength);
			unfinishedNMEntries.add(unfinishedNMEntry);
			
			lengthToAdd += prefixLength + RRIPFactory.CE_ENTRY_LENGTH;
		}
		
		return drLength + lengthToAdd;
	}

	private HashMap finalizeDR(HashMap memory, int length) throws HandlerException {
		if (length<=250) {
			// Write ST entry if at least 4 bytes are left
			length = doST(length);
		}
		memory.put("drLength", new Integer(length));
		return memory;
	}

	private int doST(int length) throws HandlerException {
		if (!RRIPFactory.MKISOFS_COMPATIBILITY) {
			rripFactory.doSTEntry();
			length += 4;
		}
		return length;
	}

	private void doRelocationFixups() throws HandlerException {
		doRelocationFixups(parentLocationFixups, parentLocations);
		doRelocationFixups(childLocationFixups, childLocations);
	}

	static private void doRelocationFixups(Map<ISO9660Directory, Fixup> fixups, Map<ISO9660Directory, Integer> locations) throws HandlerException {
		Iterator<ISO9660Directory> it = fixups.keySet().iterator();
		while (it.hasNext()) {
			ISO9660Directory dir = it.next();
			
			// Write and close Location Fixup
			Fixup locationFixup = fixups.get(dir);
			int location = locations.get(dir);
			locationFixup.data(new BothWordDataReference(location));
			locationFixup.close();
		}		
	}

	private void doCA() throws HandlerException {
		long position = streamHandler.mark();
		streamHandler.startElement(new LogicalSectorElement("CA"));
		int location = helper.getCurrentLocation();

		// Write and close RRIP ER Location Fixup
		Fixup rripERLocationFixup = (Fixup) volumeFixups.get("rripERLocationFixup");
		rripERLocationFixup.data(new BothWordDataReference(location));
		rripERLocationFixup.close();

		// Write ER Entry
		rripFactory.doEREntry();
		
		// Write ST Entry and compute length 
		int erLength = doST(helper.getDifferenceTo(position));
		
		// Write and close RRIP ER Length Fixup
		Fixup rripERLengthFixup = (Fixup) volumeFixups.get("rripERLengthFixup");
		rripERLengthFixup.data(new BothWordDataReference(erLength));
		rripERLengthFixup.close();
		
		// Process unfinished NM Entries
		int offset = erLength;
		Iterator<UnfinishedNMEntry> it = unfinishedNMEntries.iterator();
		while (it.hasNext()) {
			UnfinishedNMEntry unfinishedNMEntry = it.next();
			String name = unfinishedNMEntry.filenameRest;
			rripFactory.doNMEntry(0, helper.getFilenameDataReference(name));
			
			// Write and close CE Entry Location Fixup
			unfinishedNMEntry.location.data(new BothWordDataReference(location));
			unfinishedNMEntry.location.close();

			// Write and close CE Entry Offset Fixup
			unfinishedNMEntry.offset.data(new BothWordDataReference(offset));
			unfinishedNMEntry.offset.close();

			// Write ST Entry and compute length
			int ceLength = doST(name.length() + RRIPFactory.NM_ENTRY_LENGTH);
			
			// Write and close CE Entry Length Fixup
			unfinishedNMEntry.length.data(new BothWordDataReference(ceLength));
			unfinishedNMEntry.length.close();

			offset += ceLength;
		}
		
		streamHandler.endElement();
	}
	
	class UnfinishedNMEntry {
		Fixup location, offset, length;
		String filenameRest;
	}
}
