package org.folio.services.protection.models;

import java.util.Arrays;
import java.util.List;

public enum AcqDesiredPermissions {
  MANAGE("organizations.acquisitions-units-assignments.manage");

  private final String permission;
  private static final List<String> values;
  static {
    values = Arrays.stream(AcqDesiredPermissions.values())
        .map(AcqDesiredPermissions::getPermission)
        .toList();
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
