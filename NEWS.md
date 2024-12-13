## 5.1.0 - Unreleased

## 5.0.1 - Released (Ramsons R2 2024 Bug Fix)
The focus of this release was to fix issue with including Rollover transfer amounts in Budget summary for Net Transfers

  [Full Changelog](https://github.com/folio-org/mod-finance/compare/v5.0.0...v5.0.1)

### Bug Fixes
* [MODFIN-394](https://issues.folio.org/browse/MODFIN-394) - Budget summary for Net Transfers do not include Rollover transfer amounts

## 5.0.0 - Released (Ramsons R2 2024)
The primary focus of this release was to improve budget and expenditure handling, and update core functionality.

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.9.0...v5.0.0)

### Stories
* [MODFIN-380](https://issues.folio.org/browse/MODFIN-380) - Update libraries of dependant acq modules to the latest versions
* [MODFIN-374](https://issues.folio.org/browse/MODFIN-374) - Update budget validation when updating a budget
* [MODFIN-371](https://issues.folio.org/browse/MODFIN-371) - Separate credits from expenditures in mod-finance
* [MODFIN-370](https://issues.folio.org/browse/MODFIN-370) - Update the schemas to separate credits from expenditures
* [MODFIN-358](https://issues.folio.org/browse/MODFIN-358) - Add tenantId to the locations schema for restrict by Fund functionality
* [MODFIN-352](https://issues.folio.org/browse/MODFIN-352) - Remove the old transaction API

### Bug Fixes
* [MODFIN-372](https://issues.folio.org/browse/MODFIN-372) - Wrong expense class percent of total expended when budget over expended

### Dependencies
* Bump `raml` from `35.2.0` to `35.3.0`
* Added `folio-module-descriptor-validator` version `1.0.0`

## 4.9.0 - Released (Quesnelia R1 2024)
The primary focus of this release was to add new Batch Transactions API

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.8.0...v4.9.0)

### Stories
* [MODFIN-350](https://issues.folio.org/browse/MODFIN-350) - Use the batch API for the release/unrelease encumbrance endpoints
* [MODFIN-349](https://issues.folio.org/browse/MODFIN-349) - Implement a new endpoint for currency exchange calculation
* [MODFIN-346](https://issues.folio.org/browse/MODFIN-346) - Implement unrelease encumbrance endpoint
* [MODFIN-344](https://issues.folio.org/browse/MODFIN-344) - Implement functionality to recalculate budgets based on conducted transactions
* [MODFIN-342](https://issues.folio.org/browse/MODFIN-342) - Add restictByLocations flag to Fund in acq models
* [MODFIN-337](https://issues.folio.org/browse/MODFIN-337) - Add locationIds to fund schema
* [MODFIN-334](https://issues.folio.org/browse/MODFIN-334) - Update RMB and vertx to the latest version
* [MODFIN-333](https://issues.folio.org/browse/MODFIN-333) - Add donor info to Fund
* [MODFIN-320](https://issues.folio.org/browse/MODFIN-320) - Create batch endpoint to update list of transactions at single request

### Bug Fixes
* [MODFIN-363](https://folio-org.atlassian.net/browse/MODFIN-363) - Missing ledger-rollover interface dependency in module descriptor
* [MODFIN-357](https://folio-org.atlassian.net/browse/MODFIN-357) - Missed declaration of backend permissions

### Tech Dept
* [MODFIN-327](https://folio-org.atlassian.net/browse/MODFIN-327) - Replace FolioVertxCompletableFuture usage
* [MODFIN-248](https://folio-org.atlassian.net/browse/MODFIN-248) - Refactoring : Replace extra RestClients with one
* [MODFIN-232](https://folio-org.atlassian.net/browse/MODFIN-232) - Logging improvement

### Dependencies
* Bump `rmb` from `35.0.1` to `35.2.0`
* Bump `vertex` from `4.3.4` to `4.5.4`

## 4.8.0 - Released (Poppy R2 2023)
The primary focus of this release was to add possibility to filter expense classes by fiscal year id

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.7.0...v4.8.0)

### Stories
* [MODFIN-329](https://issues.folio.org/browse/MODFIN-329) - Add missed permissions to module descriptor
* [MODFIN-324](https://issues.folio.org/browse/MODFIN-324) - Add missing required interface to module descriptor
* [MODFIN-322](https://issues.folio.org/browse/MODFIN-322) - Update to Java 17 mod-finance
* [MODFIN-317](https://issues.folio.org/browse/MODFIN-317) - Add possibility to filter expense classes by fiscal year id
* [MODFIN-316](https://issues.folio.org/browse/MODFIN-316) - Provide proxy endpoints to add and delete ledger rollover error in the business layer
* [MODFIN-296](https://issues.folio.org/browse/MODFIN-296) - Update dependent raml-util

### Bug Fixes
* [MODFIN-308](https://issues.folio.org/browse/MODFIN-308) - Maintain displaying "Available" as a negative number for group and ledger

### Dependencies
* Bump `java version` from `11` to `17`

## 4.7.0  - Released (Orchid R1 2023)
The primary focus of this release was to restrict search/view of Fiscal year records based upon acquisitions unit

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.6.0...v4.7.0)

### Stories
* [MODFIN-293](https://issues.folio.org/browse/MODFIN-293) - Restrict search/view of Fiscal year records based upon acquisitions unit
* [MODFIN-286](https://issues.folio.org/browse/MODFIN-286) - Logging improvement - Configuration

### Bug Fixes
* [MODFIN-299](https://issues.folio.org/browse/MODFIN-299) - Group and Ledger summary for Net Transfers do not include Rollover transfer amounts.
* [MODFIN-264](https://issues.folio.org/browse/MODFIN-264) - Optimistic locking ignored when changing expense class and adding group

## 4.6.0 Nolana R3 2022 - Released
This release contains RMB upgrade and implement new business API

### Stories
* [FOLIO-3604](https://issues.folio.org/browse/FOLIO-3604) - FolioVertxCompletableFuture copyright violation
* [MODFIN-279](https://issues.folio.org/browse/MODFIN-279) - Upgrade RAML Module Builder
* [MODFIN-270](https://issues.folio.org/browse/MODFIN-270) - Define and implement Business API : Ledger Rollover Logs
* [MODFIN-256](https://issues.folio.org/browse/MODFIN-256) - Define and implement Business API : Ledger Rollover Budgets

## 4.5.0 Morning Glory R2 2022 - Released
This release contains RMB upgrade and fixes for updating budget metadata when transferring between funds

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.4.0...v4.5.0)

### Stories
* [MODFIN-254](https://issues.folio.org/browse/MODFIN-254) - Upgrade RAML Module Builder to 34.1.0

### Bug Fixes
* [MODFIN-250](https://issues.folio.org/browse/MODFIN-250) - Budget metadata not updated when transferring between Funds

## 4.4.0 - Released
This release contains updates for supporting transactions cancel

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.3.0...v4.4.0)
### Stories
* [MODFIN-230](https://issues.folio.org/browse/MODFIN-230) - Allow PUT for payments/credits to cancel invoices


## 4.3.0 - Released
This release contains implementation of two new endpoints for deleting encumbrances and retrieving combination of funds info

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.2.0...v4.3.0)
### Stories
* [MODFIN-207](https://issues.folio.org/browse/MODFIN-207) - Create business API for removing Encumbrance transactions
* [MODFIN-199](https://issues.folio.org/browse/MODFIN-199) - Create API for retrieving combination of fund code and expense classes

### Bug Fixes
* [MODFIN-201](https://issues.folio.org/browse/MODFIN-201) - The fiscal year is successfully saved with wrong date


## 4.2.0 - Released
The focus of this release was to update RMB up to v33.0.0 and fix rollover procedure

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.1.2...v4.2.0)
### Stories
* [MODFIN-191](https://issues.folio.org/browse/MODFIN-191) - mod-finance: Update RMB

### Bug Fixes
* [MODFIN-197](https://issues.folio.org/browse/MODFIN-197) - "Request-URI Too Long" error occurs when attempting to view a ledger when there are more than a few budgets
* [MODFIN-195](https://issues.folio.org/browse/MODFIN-195) - Unable to complete fiscal year rollover


## 4.1.2 - Released
The focus of this release was to fix issue with many budgets for ledger

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.1.1...v4.1.2)

### Bug Fixes
* [MODFIN-197](https://issues.folio.org/browse/MODFIN-197) - "Request-URI Too Long" error occurs when attempting to view a ledger when there are more than a few budgets

## 4.1.1 - Released
The focus of this release was to fix Unable to complete fiscal year rollover issue

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.1.0...v4.1.1)

### Bug Fixes
* [MODFIN-195](https://issues.folio.org/browse/MODFIN-195) - Unable to complete fiscal year rollover

## 4.1.0 - Released
The focus of this release was to update RMB, support ledger fiscal year rollover. 
Add financial summary for the fiscal year, ledger and budget. 

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.0.1...v4.1.0)

### Technical tasks
* [MODFIN-171](https://issues.folio.org/browse/MODFIN-171) - mod-finance: Update RMB

### Stories
* [MODFIN-188](https://issues.folio.org/browse/MODFIN-188) - Verify fiscal year start date and end date, Times
* [MODFIN-187](https://issues.folio.org/browse/MODFIN-187) - Align fetching current fiscal year to FOLIO's timezone
* [MODFIN-183](https://issues.folio.org/browse/MODFIN-183) - Update API with addition call to order-transaction-summaries
* [MODFIN-175](https://issues.folio.org/browse/MODFIN-175) - Support PUT for the Budget
* [MODFIN-173](https://issues.folio.org/browse/MODFIN-173) - Update /finance/group-fiscal-year-summaries API to include more summary information
* [MODFIN-170](https://issues.folio.org/browse/MODFIN-170) - Update business API for retrieving fiscal year by Id with financial summary
* [MODFIN-168](https://issues.folio.org/browse/MODFIN-168) - Update logic with financial detail in the Ledger summary to improve the users ability to manage financial activity
* [MODFIN-166](https://issues.folio.org/browse/MODFIN-166) - Restrict search/view of Fund records based upon acquisitions unit
* [MODFIN-162](https://issues.folio.org/browse/MODFIN-162) - Define business API for starting building Error Report
* [MODFIN-161](https://issues.folio.org/browse/MODFIN-161) - Add fields to the group expense classes schema
* [MODFIN-159](https://issues.folio.org/browse/MODFIN-159) - Update logic for calculating group expense classes totals with encumbered and awaiting payment
* [MODFIN-158](https://issues.folio.org/browse/MODFIN-158) - Define business API for retrieving rollover status/progress
* [MODFIN-157](https://issues.folio.org/browse/MODFIN-157) - Define business API for the ledger fiscal yea rollover

### Bug Fixes
* [MODFIN-194](https://issues.folio.org/browse/MODFIN-194) - Field "allocated" must be required In the shared budget schema
* [MODFIN-189](https://issues.folio.org/browse/MODFIN-189) - Incorrect calculation total finance information for Ledger
* [MODFIN-174](https://issues.folio.org/browse/MODFIN-174) - Incorrect Group "Decrease in allocation" value
* [MODFIN-165](https://issues.folio.org/browse/MODFIN-165) - Some transactions are not considered when calculating group expense-class-totals


## 4.0.1 - Released
The focus of this release was to fix logging issues

[Full Changelog](https://github.com/folio-org/mod-finance/compare/v4.0.0...v4.0.1)

### Bug Fixes
* [MODFIN-163](https://issues.folio.org/browse/MODFIN-163) No logging in honeysuckle version


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
