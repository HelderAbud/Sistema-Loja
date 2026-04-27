package com.lojapp.service;

import com.lojapp.exception.domain.NfeXmlUnreadableException;
import com.lojapp.xml.SecureDocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parse mínimo de NFe (XML simplificado ou layout real com namespace) com factory à prova de XXE.
 * Usa {@code getElementsByTagNameNS("*", local)} para ignorar prefixos ({@code nfe:}, default NS, etc.).
 */
public final class NfeXmlParser {

    private static final String NS_ANY = "*";

    private NfeXmlParser() {}

    public static ParsedNfe parse(String rawXml) {
        try {
            DocumentBuilderFactory factory = SecureDocumentBuilderFactory.createNfeFactory();
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawXml)));
            String number = firstTextInDoc(doc, "nNF", "0");
            String supplierName = firstSupplierName(doc);
            Optional<String> supplierTaxId = firstEmitTaxId(doc);
            String accessKey = firstTextInDoc(doc, "chNFe", "");

            NodeList prodNodes = doc.getElementsByTagNameNS(NS_ANY, "prod");
            List<ParsedNfeItem> items = new ArrayList<>();
            for (int i = 0; i < prodNodes.getLength(); i++) {
                Node node = prodNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element prod = (Element) node;
                String description = textFromElementNs(prod, "xProd", "Produto sem nome");
                String cEan = textFromElementNs(prod, "cEAN", "");
                String cEanTrib = textFromElementNs(prod, "cEANTrib", "");
                BigDecimal qty = new BigDecimal(textFromElementNs(prod, "qCom", "0"));
                BigDecimal unitCost = new BigDecimal(textFromElementNs(prod, "vUnCom", "0"));
                items.add(new ParsedNfeItem(description, cEan, cEanTrib, qty, unitCost));
            }
            return new ParsedNfe(number, supplierName, accessKey, supplierTaxId, items);
        } catch (Exception e) {
            throw new NfeXmlUnreadableException("Falha ao ler XML da NFe", e);
        }
    }

    /** Preferência: primeiro {@code emit/xNome} (fornecedor); senão primeiro {@code xNome} no documento. */
    private static String firstSupplierName(Document doc) {
        NodeList emits = doc.getElementsByTagNameNS(NS_ANY, "emit");
        for (int i = 0; i < emits.getLength(); i++) {
            Node n = emits.item(i);
            if (n instanceof Element emit) {
                String name = textFromElementNs(emit, "xNome", "");
                if (!name.isBlank()) {
                    return name;
                }
            }
        }
        return firstTextInDoc(doc, "xNome", "Fornecedor nao informado");
    }

    /** Primeiro {@code emit} com {@code CNPJ} ou {@code CPF} preenchido (NFe). */
    private static Optional<String> firstEmitTaxId(Document doc) {
        NodeList emits = doc.getElementsByTagNameNS(NS_ANY, "emit");
        for (int i = 0; i < emits.getLength(); i++) {
            Node n = emits.item(i);
            if (!(n instanceof Element emit)) {
                continue;
            }
            String cnpj = textFromElementNs(emit, "CNPJ", "").trim();
            if (!cnpj.isBlank()) {
                return Optional.of(normalizeTaxDigits(cnpj, 14));
            }
            String cpf = textFromElementNs(emit, "CPF", "").trim();
            if (!cpf.isBlank()) {
                return Optional.of(normalizeTaxDigits(cpf, 14));
            }
        }
        return Optional.empty();
    }

    private static String normalizeTaxDigits(String raw, int maxLen) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > maxLen) {
            return digits.substring(0, maxLen);
        }
        return digits;
    }

    private static String firstTextInDoc(Document doc, String localName, String fallback) {
        NodeList nodes = doc.getElementsByTagNameNS(NS_ANY, localName);
        return firstNonBlankText(nodes, fallback);
    }

    private static String firstNonBlankText(NodeList nodes, String fallback) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getTextContent() != null) {
                String t = n.getTextContent().trim();
                if (!t.isBlank()) {
                    return t;
                }
            }
        }
        return fallback;
    }

    private static String textFromElementNs(Element element, String localName, String fallback) {
        NodeList nodes = element.getElementsByTagNameNS(NS_ANY, localName);
        return firstNonBlankText(nodes, fallback);
    }

    public record ParsedNfe(
            String number,
            String supplierName,
            String accessKey,
            Optional<String> supplierTaxId,
            List<ParsedNfeItem> items) {}

    public record ParsedNfeItem(
            String description,
            String cEanRaw,
            String cEanTribRaw,
            BigDecimal quantity,
            BigDecimal unitCost) {}
}
