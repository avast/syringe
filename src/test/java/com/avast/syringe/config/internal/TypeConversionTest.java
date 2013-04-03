package com.avast.syringe.config.internal;

import org.junit.Assert;
import org.junit.Test;

public class TypeConversionTest {

    @Test
    public void testBasic() throws Exception {
        Assert.assertEquals(true, TypeConversion.convert("true", Boolean.class).booleanValue());
        Assert.assertEquals(true, TypeConversion.convert("1", Boolean.class).booleanValue());

        Assert.assertEquals(1234567890, TypeConversion.convert("1234567890", Integer.class).intValue());

        Assert.assertEquals(12345678901234L, TypeConversion.convert("12345678901234", Long.class).longValue());

        Assert.assertEquals(TypeConversionTest.class, TypeConversion.convert("com.avast.syringe.config.internal.TypeConversionTest", Class.class));
    }
}
