package com.avast.syringe;

/**
 * A generic provider of instances. The provider must not rely on the number of {@link #getInstance()} invocations.
 * Therefore, if it wants to provide a singleton, for example, it must implement its own logic.
 * <p/>
 * User: slajchrt
 * Date: 4/5/12
 * Time: 12:44 PM
 */
public interface Provider<T> {

    /**
     * Note: this method can be invoked any times.
     * @return the instance.
     * @throws Exception
     */
    T getInstance() throws Exception;

}
