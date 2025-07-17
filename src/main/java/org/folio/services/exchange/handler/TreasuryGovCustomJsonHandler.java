package org.folio.services.exchange.handler;

import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.ExchangeRate;
import org.folio.rest.jaxrs.model.ExchangeRateSource;

import javax.ws.rs.core.HttpHeaders;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.folio.rest.jaxrs.model.ExchangeRate.OperationMode;

@Log4j2
public class TreasuryGovCustomJsonHandler extends AbstractCustomJsonHandler {

  private static final String URI_TEMPLATE = "%s?fields=country_currency_desc,exchange_rate,record_date"
    + "&filter=country_currency_desc:in:(%s),record_date:lte:%s"
    + "&sort=-record_date"
    + "&page[size]=1";
  private static final String DATA = "data";
  private static final String EXCHANGE_RATE = "exchange_rate";

  public TreasuryGovCustomJsonHandler(HttpClient httpClient, ExchangeRateSource rateSource) {
    super(httpClient, rateSource);
  }

  @Override
  @SneakyThrows
  public Pair<BigDecimal, ExchangeRate.OperationMode> getExchangeRateFromApi(String from, String to) {
    if (StringUtils.equals(from, to)) {
      return Pair.of(BigDecimal.ONE, ExchangeRate.OperationMode.MULTIPLY);
    }
    var operationMode = getOperationMode(true, from);
    if (operationMode == ExchangeRate.OperationMode.DIVIDE) {
      to = from;
    }

    var requestDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    var normalizedPreparedUri = String.format(URI_TEMPLATE, rateSource.getProviderUri(),
      CountryCurrency.valueOf(to).value, requestDate).replace(" ", "+");
    var httpRequest = HttpRequest.newBuilder()
      .uri(new URI(normalizedPreparedUri))
      .headers(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF_8).GET()
      .build();

    var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    log.debug("getExchangeRateFromApi:: Status code: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

    var exchangeRate = new JsonObject(httpResponse.body())
      .getJsonArray(DATA)
      .getJsonObject(0)
      .getString(EXCHANGE_RATE);

    return Pair.of(new BigDecimal(exchangeRate), operationMode);
  }

  public static OperationMode getOperationMode(boolean isTreasureGovProvider, String from) {
    if (isTreasureGovProvider && !StringUtils.equals(from, TreasuryGovCustomJsonHandler.CountryCurrency.USD.name())) {
      return ExchangeRate.OperationMode.DIVIDE;
    }
    return ExchangeRate.OperationMode.MULTIPLY;
  }

  public enum CountryCurrency {
    USD("United States of America-Dollar"),
    ZWL("Zimbabwe-Rtgs"),
    AFN("Afghanistan-Afghani"),
    ALL("Albania-Lek"),
    DZD("Algeria-Dinar"),
    AOA("Angola-Kwanza"),
    XCD("Antigua & Barbuda-East Caribbean Dollar"),
    ARS("Argentina-Peso"),
    AMD("Armenia-Dram"),
    AUD("Australia-Dollar"),
    EUR("Euro Zone-Euro"),
    AZN("Azerbaijan-Manat"),
    BSD("Bahamas-Dollar"),
    BHD("Bahrain-Dinar"),
    BDT("Bangladesh-Taka"),
    BBD("Barbados-Dollar"),
    BYN("Belarus-New Ruble"),
    BZD("Belize-Dollar"),
    BMD("Bermuda-Dollar"),
    BOB("Bolivia-Boliviano"),
    BAM("Bosnia-Marka"),
    BWP("Botswana-Pula"),
    BRL("Brazil-Real"),
    BND("Brunei-Dollar"),
    BGN("Bulgaria-Lev New"),
    XOF("Guinea Bissau-Cfa Franc"),
    BIF("Burundi-Franc"),
    KHR("Cambodia-Riel"),
    CAD("Canada-Dollar"),
    CVE("Cape Verde-Escudo"),
    KYD("Cayman Islands-Dollar"),
    XAF("Central African Republic-Cfa Franc"),
    CLP("Chile-Peso"),
    CNY("China-Renminbi"),
    COP("Colombia-Peso"),
    KMF("Comoros-Franc"),
    CRC("Costa Rica-Colon"),
    HRK("Croatia-Kuna"),
    CUC("Cuba-Chavito"),
    CUP("Cuba-Peso"),
    CZK("Czech Republic-Koruna"),
    CDF("Democratic Republic Of Congo-Congolese Franc"),
    DKK("Denmark-Krone"),
    DJF("Djibouti-Franc"),
    DOP("Dominican Republic-Peso"),
    EGP("Egypt-Pound"),
    SVC("El Salvador-Dollar"),
    XFA("Equatorial Guinea-Cfa Franc"),
    ERN("Eritrea-Nakfa"),
    SZL("Eswatini-Lilangeni"),
    ETB("Ethiopia-Birr"),
    FJD("Fiji-Dollar"),
    GMD("Gambia-Dalasi"),
    GEL("Georgia-Lari"),
    GHS("Ghana-Cedi"),
    GTQ("Guatemala-Quetzal"),
    GNF("Guinea-Franc"),
    GYD("Guyana-Dollar"),
    HTG("Haiti-Gourde"),
    HNL("Honduras-Lempira"),
    HKD("Hong Kong-Dollar"),
    HUF("Hungary-Forint"),
    ISK("Iceland-Krona"),
    INR("India-Rupee"),
    IDR("Indonesia-Rupiah"),
    IRR("Iran-Rial"),
    IQD("Iraq-Dinar"),
    ILS("Israel-Shekel"),
    JMD("Jamaica-Dollar"),
    JPY("Japan-Yen"),
    JOD("Jordan-Dinar"),
    KZT("Kazakhstan-Tenge"),
    KES("Kenya-Shilling"),
    KRW("Korea-Won"),
    KWD("Kuwait-Dinar"),
    KGS("Kyrgyzstan-Som"),
    LAK("Laos-Kip"),
    LBP("Lebanon-Pound"),
    LSL("Lesotho-Maloti"),
    LRD("Liberia-Dollar"),
    LYD("Libya-Dinar"),
    MGA("Madagascar-Ariary"),
    MWK("Malawi-Kwacha"),
    MYR("Malaysia-Ringgit"),
    MVR("Maldives-Rufiyaa"),
    MRU("Mauritania-Ouguiya"),
    MUR("Mauritius-Rupee"),
    MXN("Mexico-Peso"),
    MDL("Moldova-Leu"),
    MNT("Mongolia-Tugrik"),
    MAD("Morocco-Dirham"),
    MZN("Mozambique-Metical"),
    MMK("Myanmar-Kyat"),
    NAD("Namibia-Dollar"),
    NPR("Nepal-Rupee"),
    ANG("Netherlands Antilles-Guilder"),
    NZD("New Zealand-Dollar"),
    NIO("Nicaragua-Cordoba"),
    NGN("Nigeria-Naira"),
    NOK("Norway-Krone"),
    OMR("Oman-Rial"),
    PKR("Pakistan-Rupee"),
    PAB("Panama-Balboa"),
    PGK("Papua New Guinea-Kina"),
    PYG("Paraguay-Guarani"),
    PEN("Peru-Sol"),
    PHP("Philippines-Peso"),
    PLN("Poland-Zloty"),
    QAR("Qatar-Riyal"),
    MKD("Republic Of North Macedonia-Denar"),
    RON("Romania-New Leu"),
    RUR("Russia-Ruble"),
    RWF("Rwanda-Franc"),
    STN("Sao Tome & Principe-New Dobras"),
    SAR("Saudi Arabia-Riyal"),
    RSD("Serbia-Dinar"),
    SCR("Seychelles-Rupee"),
    SLL("Sierra Leone-Leone"),
    SGD("Singapore-Dollar"),
    SBD("Solomon Islands-Dollar"),
    SOS("Somali-Shilling"),
    ZAR("South Africa-Rand"),
    SSP("South Sudan-Sudanese Pound"),
    LKR("Sri Lanka-Rupee"),
    SDG("Sudan-Pound"),
    SRD("Suriname-Dollar"),
    SEK("Sweden-Krona"),
    CHF("Switzerland-Franc"),
    SYP("Syria-Pound"),
    TWD("Taiwan-Dollar"),
    TJS("Tajikistan-Somoni"),
    TZS("Tanzania-Shilling"),
    THB("Thailand-Baht"),
    TOP("Tonga-Pa'Anga"),
    TTD("Trinidad & Tobago-Dollar"),
    TND("Tunisia-Dinar"),
    TRY("Turkey-New Lira"),
    TMT("Turkmenistan-New Manat"),
    UGX("Uganda-Shilling"),
    UAH("Ukraine-Hryvnia"),
    AED("United Arab Emirates-Dirham"),
    GBP("United Kingdom-Pound"),
    UYU("Uruguay-Peso"),
    UZS("Uzbekistan-Som"),
    VUV("Vanuatu-Vatu"),
    VES("Venezuela-Bolivar Soberano"),
    VEF("Venezuela-Fuerte"),
    VND("Vietnam-Dong"),
    WST("Western Samoa-Tala"),
    YER("Yemen-Rial"),
    ZMW("Zambia-New Kwacha"),
    ZWG("Zimbabwe-Gold");

    private final String value;

    CountryCurrency(String value) {
      this.value = value;
    }
  }
}
