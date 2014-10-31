/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.acorn.xom;

import org.oasisopen.xliff.om.v1.IExtObjects;
import org.oasisopen.xliff.om.v1.IWithExtObjects;

abstract class BaseData2 extends BaseData1 implements IWithExtObjects {

	private IExtObjects xObjs;

	@Override
	public boolean hasExtObject () {
		if ( xObjs == null ) return false;
		return !xObjs.isEmpty();
	}

	@Override
	public IExtObjects getExtObjects () {
		if ( xObjs == null ) xObjs = new ExtObjects(this);
		return xObjs;
	}

	@Override
	public String getNSUri () {
		return Const.NS_XLIFF_CORE20;
	}

}
