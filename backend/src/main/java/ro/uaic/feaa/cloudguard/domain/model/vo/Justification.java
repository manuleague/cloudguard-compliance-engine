package ro.uaic.feaa.cloudguard.domain.model.vo;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Value object: justificarea unei rezolvări sau a unei excepții de risc (regula V4).
 */
@Embeddable
public class Justification {

    @NotBlank(message = "Justificarea este obligatorie")
    @Size(min = 20, max = 1000, message = "Justificarea trebuie să aibă minimum 20 de caractere")
    @Column(name = "justification", length = 1000)
    private String text;

    protected Justification() {
        // pentru JPA
    }

    private Justification(String text) {
        this.text = text == null ? null : text.trim();
    }

    public static Justification of(String text) {
        return new Justification(text);
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Justification other && Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }

    @Override
    public String toString() {
        return text;
    }
}
