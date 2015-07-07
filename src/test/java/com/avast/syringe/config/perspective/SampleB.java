/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
@Named
public class SampleB {

    @Inject
    private String name;

    @Inject
    private Integer age;


}
