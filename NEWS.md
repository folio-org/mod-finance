## 4.1.0 - Unreleased

## 4.0.0 - Released
The primary focus of this release introduce shared allocations and net transfer logic for budgets and groups.
Also **major versions of APIs** were changed for **finance.transactions**

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v3.0.1...v4.0.0)

### Stories
* [MODFIN-153](https://issues.folio.org/browse/MODFIN-153)	Implement PUT /finance/invoice-transaction-summary
* [MODFIN-151](https://issues.folio.org/browse/MODFIN-151)	When creating budget add expense classes from previous budget automatically	
* [MODFIN-146](https://issues.folio.org/browse/MODFIN-146)	Create business API for retrieving current active budget for fund	
* [MODFIN-145](https://issues.folio.org/browse/MODFIN-145)	Define and implement PUT /finance/pending-payments/id API	
* [MODFIN-143](https://issues.folio.org/browse/MODFIN-143)	Calculate ledgers totals on the fly	
* [MODFIN-142](https://issues.folio.org/browse/MODFIN-142)	mod-finance: Update RMB	
* [MODFIN-141](https://issues.folio.org/browse/MODFIN-141)	Implement API for getting expense classes for fund	
* [MODFIN-139](https://issues.folio.org/browse/MODFIN-139)	Migrate mod-finance to JDK 11		
* [MODFIN-138](https://issues.folio.org/browse/MODFIN-138)	Update /finance/budgets POST and PUT APIs	
* [MODFIN-137](https://issues.folio.org/browse/MODFIN-137)	Define and implement GET /finance/groups/id/expense-classes-totals API	
* [MODFIN-136](https://issues.folio.org/browse/MODFIN-136)	Define and implement GET /finance/budgets/id/expense-classes-totals API	
* [MODFIN-134](https://issues.folio.org/browse/MODFIN-134)	Create PUT /finance/order-transaction-summaries/id and PUT /finance/encumbrance/id APIs	
* [MODFIN-131](https://issues.folio.org/browse/MODFIN-131)	Create the pending-payments API
* [MODFIN-130](https://issues.folio.org/browse/MODFIN-130)	Define and Implement Business API for the expense class

### Bug Fixes
* [MODFIN-112](https://issues.folio.org/browse/MODFIN-112)	Shouldn't allow negative payments or credits

## 3.0.1 - Released
Hotfix release to fix budget's totals calculations

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v3.0.0...v3.0.1)

### Bud Fixes
* [MODFIN -147](https://issues.folio.org/browse/MODFIN-147) Budget allocation calculation out of sync with transaction log

## 3.0.0 - Released
The primary focus of this release was to implement exchange rate API and bug fixing.
Also **major versions of APIs** were changed for **finance.transactions** and **finance.invoice-transaction-summaries**

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v2.0.1...v3.0.0)

### Stories
* [MODFIN-133](https://issues.folio.org/browse/MODFIN-133) - mod-finance: Update to RMB v30.0.1
* [MODFIN-124](https://issues.folio.org/browse/MODFIN-124) - Securing APIs by default
* [MODFIN-121](https://issues.folio.org/browse/MODFIN-121) - Return helpful and clear error code, when Group name already exist
* [MODFIN-111](https://issues.folio.org/browse/MODFIN-111) - Exchange rate API

### Bug Fixes
* [MODFIN-129](https://issues.folio.org/browse/MODFIN-129) - Mod-finance calls api without requesting permissions for it.
* [MODFIN-122](https://issues.folio.org/browse/MODFIN-122) - Module started to crash after introducing rate of exchange logic

## 2.0.1 - Released
Bugfix release to fix error message upon budget deletion and fix raml to return json wherever possible

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v2.0.0...v2.0.1)

### Bug Fixes
* [MODFIN-117](https://issues.folio.org/browse/MODFIN-117) - Return meaningful error message upon failed budget deletion
* [MODFIN-64](https://issues.folio.org/browse/MODFIN-64) - Fix raml(contract) to return application/json responses wherever possible

## 2.0.0 - Released
The primary focus of this release was to implement releasing encumbrances and payments\credits API.

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v1.1.0...v2.0.0)

### Stories
*[MODFIN-113](https://issues.folio.org/browse/MODFIN-113) Add invoiceId and invoiceLineId to the awaitingPayment schema
*[MODFIN-106](https://issues.folio.org/browse/MODFIN-106) Add fiscalYear query arg to GET /finance/ledgers API
*[MODFIN-99](https://issues.folio.org/browse/MODFIN-99) Update groupFundFiscalYear when fund is updated
*[MODFIN-86](https://issues.folio.org/browse/MODFIN-86) Restrict adjustment of budget's allowableEncumbrance and allowableExpenditures
*[MODFIN-83](https://issues.folio.org/browse/MODFIN-83) Create release encumbrance API
*[MODFIN-71](https://issues.folio.org/browse/MODFIN-71) Create transaction APIs - Payments, Credits

### Bug Fixes
*[MODFIN-108](https://issues.folio.org/browse/MODFIN-108) Get current fiscal year should include the start and end days in the period

	
## 1.1.0 - Released
This release contains changes to the schema in ledger adding fields restrictEncumbrance, restrictExpenditures

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
