package edu.sc.seis.receiverFunction;


/**
 * @author crotwell
 * Created on Jan 26, 2005
 */
public class RecFuncException extends Exception {

    public RecFuncException() {
        super();
    }

    public RecFuncException(String message) {
        super(message);
    }

    public RecFuncException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecFuncException(Throwable cause) {
        super(cause);
    }
}
