package ro.uaic.feaa.cloudguard.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * (Bonus) Interfața CRUD generică pentru repository-urile „complete JPA”,
 * implementate manual cu EntityManager, fără Spring Data.
 */
public interface IRepository<T, ID> {

    /** Inserează entitatea nouă sau o actualizează pe cea existentă. */
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void delete(T entity);

    void deleteById(ID id);

    long count();
}
