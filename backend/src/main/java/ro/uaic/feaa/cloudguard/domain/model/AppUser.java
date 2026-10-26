package ro.uaic.feaa.cloudguard.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Entitate de suport: utilizatorul aplicației, cu un rol. */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele de utilizator este obligatoriu")
    @Size(min = 3, max = 50, message = "Numele de utilizator trebuie să aibă între 3 și 50 de caractere")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Adresa de e-mail este obligatorie")
    @Email(message = "Adresa de e-mail nu este validă")
    @Column(nullable = false, length = 150)
    private String email;

    @NotNull(message = "Rolul utilizatorului este obligatoriu")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    protected AppUser() {
        // pentru JPA
    }

    public AppUser(String username, String email, UserRole role, Team team) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.team = team;
        if (team != null) {
            team.addMember(this);
        }
    }

    public boolean hasRole(UserRole expected) {
        return role == expected;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Team getTeam() {
        return team;
    }
}
