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

package de.tu_darmstadt.informatik.rbg.hatlak.eltorito.impl;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.LayoutHelper;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.impl.*;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.volumedescriptors.*;
import de.tu_darmstadt.informatik.rbg.hatlak.sabre.impl.*;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.*;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.impl.*;

public class ElToritoHandler extends ChainingStreamHandler {
	private ElToritoConfig config;
	private Fixup bootCatalogLocation, bootImageLocation;
	
	public ElToritoHandler(StreamHandler streamHandler, ElToritoConfig config) {
		super(streamHandler, streamHandler);
		this.config = config;
	}
	
	public void startElement(Element element) throws HandlerException {
		if (element instanceof ISO9660Element) {
			String id = (String) element.getId();
			process(id);
		}
		super.startElement(element);
	}
	
	private void process(String id) throws HandlerException {
		if (id.equals("VDS")) {
			doBVD();
		} else
		if (id.equals("BIA")) {
			doCatalog();
		}
		if (id.equals("BDA")) {
			doImage();
		}
	}

	private void doBVD() throws HandlerException {
		super.startElement(new LogicalSectorElement("BR"));
		
		LayoutHelper helper = new ElToritoLayoutHelper(this);
		BootRecord br = new BootRecord(this, helper);
		br.setMetadata(config);
		br.doBR();
		
		// Remember Boot System Use (absolute pointer to first sector of Boot Catalog)
		bootCatalogLocation = fixup(new LSBFWordDataReference(0));
		
		super.endElement();
	}
	
	private void doCatalog() throws HandlerException {
		super.startElement(new LogicalSectorElement("BCAT"));
		
		// Write and close Boot Catalog Location Fixup
		long position = mark();
		int location = (int) (position / ISO9660Constants.LOGICAL_BLOCK_SIZE);
		bootCatalogLocation.data(new LSBFWordDataReference(location));
		bootCatalogLocation.close();
		
		ElToritoFactory etf = new ElToritoFactory(this);
		
		// Validation Entry
		int platformID = config.getPlatformID();
		String idString = config.getIDString();
		etf.doValidationEntry(platformID, idString);
		
		// Initial/Default Entry
		boolean bootable = config.getBootable();
		int bootMediaType = config.getBootMediaType();
		int loadSegment = config.getLoadSegment();
		int systemType = config.getSystemType();
		int sectorCount = config.getSectorCount();
		bootImageLocation = etf.doDefaultEntry(bootable, bootMediaType, loadSegment, systemType, sectorCount);

		super.endElement();
	}
	
	private void doImage() throws HandlerException {
		super.startElement(new LogicalSectorElement("BIMG"));
		
		// Write and close Boot Image Location Fixup
		long position = mark();
		int location = (int) (position / ISO9660Constants.LOGICAL_BLOCK_SIZE);
		bootImageLocation.data(new LSBFWordDataReference(location));
		bootImageLocation.close();
		
		// Write Boot Image
		FileDataReference fdr = new FileDataReference(config.getBootImage());
		data(fdr);
	
		super.endElement();
	}
}
