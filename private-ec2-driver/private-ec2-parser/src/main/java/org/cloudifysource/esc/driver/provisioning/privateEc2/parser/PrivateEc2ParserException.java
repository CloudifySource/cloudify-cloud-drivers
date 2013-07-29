package org.cloudifysource.esc.driver.provisioning.privateEc2.parser;

public class PrivateEc2ParserException extends Exception {

    private static final long serialVersionUID = 1L;

    public PrivateEc2ParserException() {
    }

    public PrivateEc2ParserException(String message) {
        super(message);
    }

    public PrivateEc2ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrivateEc2ParserException(Throwable cause) {
        super(cause);
    }

}
