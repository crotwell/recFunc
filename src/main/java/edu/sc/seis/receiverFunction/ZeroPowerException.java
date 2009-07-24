package edu.sc.seis.receiverFunction;


public class ZeroPowerException extends RecFuncException {

    public ZeroPowerException() {
    }

    public ZeroPowerException(String message) {
        super(message);
    }

    public ZeroPowerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZeroPowerException(Throwable cause) {
        super(cause);
    }
}
