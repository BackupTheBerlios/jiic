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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

import de.tu_darmstadt.informatik.rbg.hatlak.rockridge.impl.POSIXFileMode;

public class ISOFileSet extends FileSet {
	private String prefix = "";
	private POSIXFileMode filemode;
	private POSIXFileMode dirmode;
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
	
	public void setFilemode(String filemode) {
		try {
			this.filemode = new POSIXFileMode(filemode);
			this.filemode.setFile();
		} catch (NumberFormatException e) {
			throw new BuildException("Badly formatted filemode", e);
		}
	}
	
	public POSIXFileMode getFilemode() {
		return filemode;
	}
	
	public void setDirmode(String dirmode) {
		try {
			this.dirmode = new POSIXFileMode(dirmode);
			this.dirmode.setDirectory();
		} catch (NumberFormatException e) {
			throw new BuildException("Badly formatted dirmode", e);
		}
	}
	
	public POSIXFileMode getDirmode() {
		return dirmode;
	}
}
