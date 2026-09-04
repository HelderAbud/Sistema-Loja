package com.lojapp.util;

/**
 * Impressão digital de XML de NFe sem {@code chNFe}: SHA-256 do conteúdo canónico (BOM, EOL e
 * espaços exteriores).
 */
public final class NfeXmlFingerprint {

    private NfeXmlFingerprint() {}

    public static String sha256Hex(String rawXml) {
        return TokenHashUtil.sha256Hex(canonicalize(rawXml));
    }

    static String canonicalize(String rawXml) {
        if (rawXml == null) {
            return "";
        }
        String canonical = rawXml;
        if (!canonical.isEmpty() && canonical.charAt(0) == '\uFEFF') {
            canonical = canonical.substring(1);
        }
        return canonical.replace("\r\n", "\n").replace("\r", "\n").strip();
    }
}
