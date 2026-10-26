package ro.uaic.feaa.cloudguard.service.validation;

import java.util.List;

/** Una sau mai multe reguli de validare pe câmpuri (V1–V5) nu sunt respectate. */
public class DomainValidationException extends RuntimeException {

    private final List<String> errors;

    public DomainValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
