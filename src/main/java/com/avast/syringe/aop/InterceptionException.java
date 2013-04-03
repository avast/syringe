package com.avast.syringe.aop;

/**
 * User: vacata
 * Date: 1/30/13
 * Time: 4:57 PM
 *
 * Unexpected unchecked exception in interceptor (check {@link Interceptor}). An occurrence of this exception will usually
 * break the chain of interceptors.
 */
public class InterceptionException extends RuntimeException {

    public InterceptionException() {
    }

    public InterceptionException(String message) {
        super(message);
    }

    public InterceptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterceptionException(Throwable cause) {
        super(cause);
    }
}
