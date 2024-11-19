package org.folio.services.protection;

public final class AcqUnitConstants {
  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";
  public static final String FD_FUND_ACQUISITIONS_UNIT_IDS = "fundAcqUnitIds"; // for finance data view table
  public static final String FD_BUDGET_ACQUISITIONS_UNIT_IDS = "budgetAcqUnitIds"; // for finance data view table
  public static final String NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " + ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String FD_NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " +
    FD_BUDGET_ACQUISITIONS_UNIT_IDS + " <> []" + FD_FUND_ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String IS_DELETED_PROP = "isDeleted";
  public static final String ALL_UNITS_CQL = IS_DELETED_PROP + "=*";
  public static final String ACTIVE_UNITS_CQL = IS_DELETED_PROP + "==false";
  public static final String ACQUISITIONS_UNIT_ID = "acquisitionsUnitId";

  private AcqUnitConstants() {

  }
}
