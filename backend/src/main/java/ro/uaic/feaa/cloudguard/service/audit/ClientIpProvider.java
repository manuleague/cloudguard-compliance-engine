package ro.uaic.feaa.cloudguard.service.audit;

/** Furnizează adresa IP a utilizatorului curent, pentru jurnalul de audit. */
@FunctionalInterface
public interface ClientIpProvider {

    String currentIp();
}
