package ro.uaic.feaa.cloudguard.domain.model.vo;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

/**
 * Value object: configurația de securitate a unei resurse cloud.
 * Nu are identitate proprie; se salvează în tabela cloud_resource.
 */
@Embeddable
public class ResourceConfiguration {

    public static final String ENCRYPTION_ENABLED = "encryptionEnabled";
    public static final String PUBLIC_ACCESS = "publicAccess";
    public static final String OPEN_PORTS = "openPorts";

    @Column(name = "encryption_enabled")
    private boolean encryptionEnabled;

    @Column(name = "public_access")
    private boolean publicAccess;

    @Convert(converter = PortSetConverter.class)
    @Column(name = "open_ports", length = 200)
    private PortSet openPorts = PortSet.empty();

    protected ResourceConfiguration() {
        // pentru JPA
    }

    public ResourceConfiguration(boolean encryptionEnabled, boolean publicAccess, PortSet openPorts) {
        this.encryptionEnabled = encryptionEnabled;
        this.publicAccess = publicAccess;
        this.openPorts = openPorts == null ? PortSet.empty() : openPorts;
    }

    /**
     * Verifică un atribut al configurației față de valoarea cerută de o politică.
     * Pentru openPorts, valoarea cerută este lista porturilor permise, ex. "443".
     */
    public boolean satisfies(String attribute, String expectedValue) {
        return switch (attribute) {
            case ENCRYPTION_ENABLED -> encryptionEnabled == Boolean.parseBoolean(expectedValue);
            case PUBLIC_ACCESS -> publicAccess == Boolean.parseBoolean(expectedValue);
            case OPEN_PORTS -> openPorts.isSubsetOf(PortSet.parse(expectedValue));
            default -> throw new IllegalArgumentException("Atribut necunoscut: " + attribute);
        };
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public PortSet getOpenPorts() {
        return openPorts;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResourceConfiguration other
                && encryptionEnabled == other.encryptionEnabled
                && publicAccess == other.publicAccess
                && Objects.equals(openPorts, other.openPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryptionEnabled, publicAccess, openPorts);
    }
}
