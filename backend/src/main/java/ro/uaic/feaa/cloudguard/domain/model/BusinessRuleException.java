package ro.uaic.feaa.cloudguard.domain.model;

/**
 * Încălcarea unei reguli de business la nivel de agregat (A1–A3, roluri).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
