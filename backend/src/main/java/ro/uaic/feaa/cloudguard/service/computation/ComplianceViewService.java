package ro.uaic.feaa.cloudguard.service.computation;

import java.util.List;

import ro.uaic.feaa.cloudguard.domain.model.Team;

/** Serviciu de calcul pentru vederi (regula C5). */
public interface ComplianceViewService {

    TeamComplianceView teamCompliance(Team team);

    List<TeamComplianceView> allTeamsCompliance();
}
