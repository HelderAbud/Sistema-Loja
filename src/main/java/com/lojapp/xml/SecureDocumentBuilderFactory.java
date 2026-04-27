package com.lojapp.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Configuração defensiva do {@link DocumentBuilderFactory} para reduzir risco de XXE ao parsear XML
 * de terceiros (ex.: NFe).
 */
public final class SecureDocumentBuilderFactory {

    private SecureDocumentBuilderFactory() {}

    public static DocumentBuilderFactory createNfeFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // NFe real usa default namespace (ex.: http://www.portalfiscal.inf.br/nfe); precisamos de
        // getElementsByTagNameNS para localizar prod, nNF, etc.
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        applySecureFeatures(factory);
        return factory;
    }

    private static void applySecureFeatures(DocumentBuilderFactory factory)
            throws ParserConfigurationException {
        setFeatureQuietly(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeatureQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeatureQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeatureQuietly(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // implementação não suporta o atributo
        }
    }

    private static void setFeatureQuietly(DocumentBuilderFactory factory, String name, boolean value) {
        try {
            factory.setFeature(name, value);
        } catch (ParserConfigurationException ignored) {
            // feature opcional conforme o parser da JVM
        }
    }
}
