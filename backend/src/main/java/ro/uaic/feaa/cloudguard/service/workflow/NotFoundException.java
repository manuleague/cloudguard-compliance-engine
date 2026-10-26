package ro.uaic.feaa.cloudguard.service.workflow;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
