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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.tu_darmstadt.informatik.rbg.mhartle.sabre.DataReference;

public class ISO9660DateDataReference implements DataReference {
	private static final byte[] EMPTY_DATE_BUFFER = new byte[] { 0x30, 0x30,
			0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30,
			0x30, 0x30, 0x30, 0x00 };

	private Date date = null;

	public ISO9660DateDataReference(Date date) {
		this.date = date;
	}

	public ISO9660DateDataReference(long date) {
		this(new Date(date));
	}

	public ISO9660DateDataReference() {
		this(new Date());
	}

	public long getLength() {
		return 17;
	}
	
	public InputStream createInputStream() throws IOException {
		byte[] buffer;
		if (date == null) {
			buffer = EMPTY_DATE_BUFFER;
		} else {
			buffer = getDate();
		}
		
		return new ByteArrayInputStream(buffer);
	}
	
	private byte[] getDate() {
		byte[] buffer = new byte[17];
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		
		// Parse date
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int hundredth_sec = cal.get(Calendar.MILLISECOND) / 10;
		int gmt_offset = cal.get(Calendar.ZONE_OFFSET) / (15 * 60 * 1000);

		// Create ISO9660 date
		StringBuilder dateString = new StringBuilder(17);
		dateString.append(padIntToString(year, 4));
		dateString.append(padIntToString(month, 2));
		dateString.append(padIntToString(day, 2));
		dateString.append(padIntToString(hour, 2));
		dateString.append(padIntToString(minute, 2));
		dateString.append(padIntToString(second, 2));
		dateString.append(padIntToString(hundredth_sec, 2));
		dateString.append(0);

		try {
			buffer = dateString.toString().getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Missing ISO-8859-1 encoding, required by Java standard");
		}
		buffer[16] = (byte) gmt_offset;

		return buffer;
	}

	private String padIntToString(int value, int length) {
		StringBuilder buf = new StringBuilder(length);
		buf.append(value);
		while (buf.length() < length) {
			buf.insert(0, "0");
		}
		return buf.toString();
	}
}
