package com.avast.syringe.config;

/**
 * User: slajchrt
 * Date: 6/12/12
 * Time: 7:40 PM
 */
public interface InstanceManager {

    String findRefName(Object ref);

    <T> T load(final String configFileName) throws Exception;

}
