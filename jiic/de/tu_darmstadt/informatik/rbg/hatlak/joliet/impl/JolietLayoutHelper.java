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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.FilenameDataReference;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660Directory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660File;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660MovedDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.ISO9660RootDirectory;
import de.tu_darmstadt.informatik.rbg.hatlak.iso9660.LayoutHelper;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.StreamHandler;

public class JolietLayoutHelper extends LayoutHelper {
	public JolietLayoutHelper(StreamHandler streamHandler, ISO9660RootDirectory root) {
		super(streamHandler, root, new JolietNamingConventions());
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660Directory dir) throws HandlerException {
		return new JolietFilenameDataReference(dir);
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660File file) throws HandlerException {
		return new JolietFilenameDataReference(file);
	}

	@Override
	public FilenameDataReference getFilenameDataReference(ISO9660MovedDirectory moved) throws HandlerException {
		return new JolietFilenameDataReference(moved);
	}
	
	@Override
	public byte[] pad(String string, int targetByteLength) throws HandlerException {
		try {
			byte[] in = string.getBytes("UTF-16BE"); // UCS-2
			byte[] out = new byte[targetByteLength]; // Java initializes the array to 0s
			System.arraycopy(in, 0, out, 0, Math.min(in.length, targetByteLength));
			
			// Pad with 0x20
			if (in.length < targetByteLength) {
				Arrays.fill(out, in.length, targetByteLength - 1, (byte) 0x20);
			}
			
			// zero terminate, two 0's for an even length, as this is a dbcs
			out[targetByteLength - 1] = 0;
			if (targetByteLength % 2 == 0) out[targetByteLength - 2] = 0;
			
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Missing ISO-8859-1 encoding, required by Java standard");
		}
	}
}
