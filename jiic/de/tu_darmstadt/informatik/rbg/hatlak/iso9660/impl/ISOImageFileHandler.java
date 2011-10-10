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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.tu_darmstadt.informatik.rbg.mhartle.sabre.DataReference;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.Element;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.Fixup;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.HandlerException;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.StreamHandler;
import de.tu_darmstadt.informatik.rbg.mhartle.sabre.impl.FileFixup;

public class ISOImageFileHandler implements StreamHandler {
	private File file = null;
	private DataOutputStream dataOutputStream = null;
	private long position = 0;
	
	private static final int BUFFER_LENGTH = 65535;
	private final byte[] buffer = new byte[BUFFER_LENGTH];
	
	/**
	 * ISO Image File Handler 
	 * 
	 * @param file ISO image output file
	 * @throws FileNotFoundException File not found
	 */
	public ISOImageFileHandler(File file) throws FileNotFoundException {
		this.file = file;
		this.dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.file)));
	}

	public void startDocument() throws HandlerException {
		// nothing to do here
	}
	
	public void startElement(Element element) throws HandlerException {
		// nothing to do here
	}

	public void data(DataReference reference) throws HandlerException {
		InputStream inputStream = null;
		
		try {
			inputStream = reference.createInputStream();
			
			long start = position;
			int read = inputStream.read(buffer);
			
			while(read > -1) {
				dataOutputStream.write(buffer, 0, read);
				position += read;
				read = inputStream.read(buffer);
			}
			
			dataOutputStream.flush();
			
			if (position - start != reference.getLength()) {
				throw new HandlerException("Data reference length did not match input stream.");
			}
		} catch (IOException e) {
			throw new HandlerException(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
			} catch (IOException e) {
			}
		}
	}
	
	public Fixup fixup(DataReference reference) throws HandlerException {
		Fixup fixup = null;
		fixup = new FileFixup(file, position, reference.getLength());
		data(reference);
		return fixup;
	}
	
	public long mark() throws HandlerException {
		return position;
	}

	public void endElement() throws HandlerException {
		// nothing to do here
	}

	public void endDocument() throws HandlerException {
		try {
			this.dataOutputStream.close();
		} catch (IOException e) {
			throw new HandlerException(e);
		}
	}
}
