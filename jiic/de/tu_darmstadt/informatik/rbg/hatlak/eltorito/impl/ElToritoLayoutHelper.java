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

import java.io.UnsupportedEncodingException;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.FilenameDataReference;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660Directory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660File;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660MovedDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.LayoutHelper;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.StreamHandler;

public class ElToritoLayoutHelper extends LayoutHelper {
	public ElToritoLayoutHelper(StreamHandler streamHandler) {
		super(streamHandler, null, null);
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660Directory dir)
			throws HandlerException {
		return null;
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660File file)
			throws HandlerException {
		return null;
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660MovedDirectory moved)
			throws HandlerException {
		return null;
	}
	
	@Override
	public byte[] pad(String string, int targetByteLength) throws HandlerException {
		byte[] bytes = new byte[targetByteLength];
		byte[] original;
		
		try {
			original = string.getBytes("ISO-8859-1"); // ISO Latin 1
			for (int i = 0; i < original.length; i++) {
				bytes[i] = original[i];
			}
			for (int i = original.length; i < bytes.length; i++) {
				bytes[i] = 0;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return bytes;
	}
}
