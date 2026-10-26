package ro.uaic.feaa.cloudguard.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entitate de suport: echipa care deține resurse cloud și are membri. */
@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele echipei este obligatoriu")
    @Size(max = 100, message = "Numele echipei poate avea maximum 100 de caractere")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @OneToMany(mappedBy = "team")
    private List<CloudResource> resources = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<AppUser> members = new ArrayList<>();

    protected Team() {
        // pentru JPA
    }

    public Team(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<CloudResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public List<AppUser> getMembers() {
        return Collections.unmodifiableList(members);
    }

    void addResource(CloudResource resource) {
        resources.add(resource);
    }

    void removeResource(CloudResource resource) {
        resources.remove(resource);
    }

    void addMember(AppUser user) {
        members.add(user);
    }
}
