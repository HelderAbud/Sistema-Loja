package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NfeXmlFingerprintTest {

    @Test
    void sha256Hex_crlfAndLf_sameHash() {
        String lf = "<nfe><nNF>1</nNF></nfe>\n";
        String crlf = "<nfe><nNF>1</nNF></nfe>\r\n";

        assertThat(NfeXmlFingerprint.sha256Hex(crlf)).isEqualTo(NfeXmlFingerprint.sha256Hex(lf));
    }

    @Test
    void sha256Hex_bomAndOuterWhitespace_sameHash() {
        String core = "<nfe><nNF>1</nNF></nfe>";
        String padded = "\uFEFF  \n" + core + "\n  ";

        assertThat(NfeXmlFingerprint.sha256Hex(padded)).isEqualTo(NfeXmlFingerprint.sha256Hex(core));
    }

    @Test
    void sha256Hex_differentPayloads_differ() {
        assertThat(NfeXmlFingerprint.sha256Hex("<nfe>A</nfe>"))
                .isNotEqualTo(NfeXmlFingerprint.sha256Hex("<nfe>B</nfe>"));
    }
}
