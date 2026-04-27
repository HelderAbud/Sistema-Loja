package com.lojapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nfe_entries")
@Getter
@Setter
@NoArgsConstructor
public class NfeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nfe_number", nullable = false, length = 50)
    private String nfeNumber;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    /** CNPJ ou CPF do emitente (só dígitos), até 14 caracteres. */
    @Column(name = "supplier_tax_id", length = 14)
    private String supplierTaxId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "access_key", length = 80)
    private String accessKey;

    /** SHA-256 hex do XML bruto (UTF-8), só quando não há chave de acesso — dedupe de reimportação. */
    @Column(name = "content_fingerprint", length = 64)
    private String contentFingerprint;

    @Column(name = "raw_xml", columnDefinition = "TEXT")
    private String rawXml;

    @Column(name = "raw_xml_key", length = 512)
    private String rawXmlKey;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @PrePersist
    void onPersist() {
        importedAt = Instant.now();
    }
}
