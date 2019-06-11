package exceptions;

public class NotFoundException extends Exception {

    private static final long serialVersionUID = 3016359205102593185L;

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }
}
