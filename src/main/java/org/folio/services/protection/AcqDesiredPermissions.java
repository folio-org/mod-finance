package org.folio.services.protection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum AcqDesiredPermissions {
  MANAGE("organizations.acquisitions-units-assignments.manage");

  private String permission;
  private static final List<String> values;
  static {
    values = Collections.unmodifiableList(Arrays.stream(AcqDesiredPermissions.values())
      .map(AcqDesiredPermissions::getPermission)
      .collect(Collectors.toList()));
  }

  AcqDesiredPermissions(String permission) {
    this.permission = permission;
  }

  public String getPermission() {
    return permission;
  }

  public static List<String> getValues() {
    return values;
  }
}
