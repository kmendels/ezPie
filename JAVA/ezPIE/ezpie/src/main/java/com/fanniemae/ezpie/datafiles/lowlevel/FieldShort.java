/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.datafiles.lowlevel;

import java.io.IOException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class FieldShort extends FieldReadWrite {

    public FieldShort(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldShort(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        int i = _bis.readInt();
        if (i == Integer.MIN_VALUE) {
            return null;
        }
        return i;
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeInt(Integer.MIN_VALUE);
            return;
        }
        _bos.writeInt((int) ((short) o));
    }
}
