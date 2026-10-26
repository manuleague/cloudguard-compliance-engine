package ro.uaic.feaa.cloudguard.service.computation;

/**
 * Vederea calculată v_team_compliance (regula C5): situația conformității pe echipă.
 */
public record TeamComplianceView(Long teamId,
                                 String teamName,
                                 int resourceCount,
                                 long activeViolations,
                                 double averageComplianceScore) {
}
