## 1.2.0 - Released

## 1.1.0 - Released
This release contains changes to the schema in ledger adding fields restrictEncumbrance,restrictExpenditures

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v1.0.1...v1.1.0)

## 1.0.1 - Released
Bugfix release to fix group-fiscal-year-summaries API and ledger totals calculation

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v1.0.0...v1.0.1)

### Bug Fixes
* [MODFIN-102](https://issues.folio.org/browse/MODFIN-102) GET /finance/group-fiscal-year-summaries pagination bug
* [MODFIN-98](https://issues.folio.org/browse/MODFIN-98) Ledger not displaying correct allocated total

## 1.0.0 - Released
The primary focus of this release was to update the finance API according to significantly updated schemas.

### Stories
* [MODFIN-99](https://issues.folio.org/browse/MODFIN-99) Update groupFundFiscalYear when fund is updated
* [MODFIN-97](https://issues.folio.org/browse/MODFIN-97) Update groupFundFiscalYear when budget is created
* [MODFIN-95](https://issues.folio.org/browse/MODFIN-95) Use JVM features to manage container memory
* [MODFIN-94](https://issues.folio.org/browse/MODFIN-94) Add series field while creating a FiscalYear
* [MODFIN-93](https://issues.folio.org/browse/MODFIN-93) Create an API to fetch the current FY for a ledger
* [MODFIN-88](https://issues.folio.org/browse/MODFIN-88) Create order_transaction_summaries API
* [MODFIN-82](https://issues.folio.org/browse/MODFIN-82) Create awaiting-payment API
* [MODFIN-81](https://issues.folio.org/browse/MODFIN-81) Create awaiting_payment schema
* [MODFIN-79](https://issues.folio.org/browse/MODFIN-79) Fund API updates: PUT/DELETE
* [MODFIN-78](https://issues.folio.org/browse/MODFIN-78) Fund API updates: GET by id/GET by query
* [MODFIN-77](https://issues.folio.org/browse/MODFIN-77) Fund API updates: POST
* [MODFIN-76](https://issues.folio.org/browse/MODFIN-76) CompositeFund schema
* [MODFIN-73](https://issues.folio.org/browse/MODFIN-73) Implement the GET /finance/group-fiscal-year-summaries API
* [MODFIN-70](https://issues.folio.org/browse/MODFIN-70) Create transaction APIs - Allocations, Transfers, Encumbrances
* [MODFIN-69](https://issues.folio.org/browse/MODFIN-69) Implement basic Fiscal Year API
* [MODFIN-68](https://issues.folio.org/browse/MODFIN-68) Implement Groups API
* [MODFIN-65](https://issues.folio.org/browse/MODFIN-65) Provide currency when creating Fiscal Years
* [MODFIN-63](https://issues.folio.org/browse/MODFIN-63) Define and implement the GroupFundFY API
* [MODFIN-61](https://issues.folio.org/browse/MODFIN-61) Define and Implement Budget API
* [MODFIN-60](https://issues.folio.org/browse/MODFIN-60) Implement basic Ledger API
* [MODFIN-59](https://issues.folio.org/browse/MODFIN-59) Define and Implement Fund/FundType APIs
* [MODFIN-58](https://issues.folio.org/browse/MODFIN-58) Create API Test Skeleton
* [MODFIN-52](https://issues.folio.org/browse/MODFIN-52) mod-finance project setup

### Bug Fixes
* [MODFIN-101](https://issues.folio.org/browse/MODFIN-101)	Set default headers after RMB update	
