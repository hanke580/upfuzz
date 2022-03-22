package org.zlab.upfuzz;


public class CustomExceptions {
    public static class EmptyCollectionException extends RuntimeException {
        public EmptyCollectionException(String errorMessage, Throwable err ) {
            super(errorMessage, err);
        }
    }
}
