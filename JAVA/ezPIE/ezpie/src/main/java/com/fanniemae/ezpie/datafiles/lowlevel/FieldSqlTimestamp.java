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
import java.util.Date;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class FieldSqlTimestamp extends FieldReadWrite {

    public FieldSqlTimestamp(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldSqlTimestamp(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        long l = _bis.readLong();
        if (l == 0) {
            return null;
        }
        return new Date(l);
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeLong(0);
            return;
        }
        java.sql.Timestamp dtSql = (java.sql.Timestamp) o;
        Date dtValue = new Date(dtSql.getTime());
        _bos.writeLong(dtValue.getTime());
    }
}
