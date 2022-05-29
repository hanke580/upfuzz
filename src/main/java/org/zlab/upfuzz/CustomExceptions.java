package org.zlab.upfuzz;

public class CustomExceptions {

    public static class EmptyCollectionException extends RuntimeException {
        public EmptyCollectionException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }
    }

    public static class CannotDropException extends RuntimeException {
        public CannotDropException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }
    }

    public static class PredicateUnSatisfyException extends RuntimeException {
        public PredicateUnSatisfyException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }
    }

    public static class systemStartFailureException extends RuntimeException {
        public systemStartFailureException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }
    }
}
