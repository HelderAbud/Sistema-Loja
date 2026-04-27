package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.NfeXmlUnreadableException;
import com.lojapp.service.NfeXmlParser.ParsedNfe;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NfeXmlParserTest {

    @Test
    void parse_cEanSemGtin_preservesCeanTribDigits() {
        String xml =
                """
                <nfe>
                  <nNF>2</nNF>
                  <xNome>Forn</xNome>
                  <chNFe>35200222222222222222550020000020022000000000</chNFe>
                  <prod>
                    <xProd>Desc outro</xProd>
                    <cEAN>SEM GTIN</cEAN>
                    <cEANTrib>7899988877766</cEANTrib>
                    <qCom>2</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """;
        ParsedNfe parsed = NfeXmlParser.parse(xml);
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().getFirst().cEanRaw()).isEqualTo("SEM GTIN");
        assertThat(parsed.items().getFirst().cEanTribRaw()).isEqualTo("7899988877766");
    }

    @Test
    void parse_readsCeanAndTrib() {
        String xml =
                """
                <nfe>
                  <nNF>1</nNF>
                  <xNome>Forn</xNome>
                  <chNFe>35200111111111111111550010000010011000000000</chNFe>
                  <prod>
                    <xProd>Produto X</xProd>
                    <cEAN>7891234567890</cEAN>
                    <cEANTrib>SEM GTIN</cEANTrib>
                    <qCom>1</qCom>
                    <vUnCom>10.00</vUnCom>
                  </prod>
                </nfe>
                """;
        ParsedNfe parsed = NfeXmlParser.parse(xml);
        assertThat(parsed.supplierTaxId()).isEmpty();
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().getFirst().cEanRaw()).isEqualTo("7891234567890");
        assertThat(parsed.items().getFirst().quantity()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void parse_doctypeDecl_failsSafely_noXxeResolution() {
        String malicious =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE nfe [
                  <!ENTITY xxe SYSTEM "http://127.0.0.1:9/oops">
                ]>
                <nfe>
                  <nNF>1</nNF>
                  <xNome>A</xNome>
                  <chNFe></chNFe>
                  <prod>
                    <xProd>&xxe;</xProd>
                    <qCom>1</qCom>
                    <vUnCom>1</vUnCom>
                  </prod>
                </nfe>
                """;
        assertThatThrownBy(() -> NfeXmlParser.parse(malicious))
                .isInstanceOf(NfeXmlUnreadableException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void parse_namespacedNfe_readsEmitAndDetProd() {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe352001" versao="4.00">
                    <ide><nNF>7788</nNF></ide>
                    <emit>
                      <CNPJ>12345678000199</CNPJ>
                      <xNome>Distribuidora NS Ltda</xNome>
                    </emit>
                    <dest><xNome>Loja Destino</xNome></dest>
                    <det nItem="1">
                      <prod>
                        <xProd>Papel A4 resma</xProd>
                        <cEAN>7891234567890</cEAN>
                        <qCom>5</qCom>
                        <vUnCom>22.50</vUnCom>
                      </prod>
                    </det>
                    <infNFeSupl><chNFe>35200111111111111111550010000077881123456789</chNFe></infNFeSupl>
                  </infNFe>
                </NFe>
                """;
        ParsedNfe parsed = NfeXmlParser.parse(xml);
        assertThat(parsed.number()).isEqualTo("7788");
        assertThat(parsed.supplierName()).isEqualTo("Distribuidora NS Ltda");
        assertThat(parsed.supplierTaxId()).isEqualTo(Optional.of("12345678000199"));
        assertThat(parsed.accessKey()).isEqualTo("35200111111111111111550010000077881123456789");
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().getFirst().description()).isEqualTo("Papel A4 resma");
        assertThat(parsed.items().getFirst().quantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(parsed.items().getFirst().unitCost()).isEqualByComparingTo(new BigDecimal("22.50"));
    }
}
