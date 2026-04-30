package com.lojapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cash_sessions")
@Getter
@Setter
@NoArgsConstructor
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_user_id")
    private User openedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private User closedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashSessionStatus status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "opening_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Column(name = "expected_amount", precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "counted_amount", precision = 19, scale = 2)
    private BigDecimal countedAmount;

    @Column(name = "difference_amount", precision = 19, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "difference_reason", length = 500)
    private String differenceReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (status == null) {
            status = CashSessionStatus.OPEN;
        }
        if (openingAmount == null) {
            openingAmount = BigDecimal.ZERO;
        }
        if (openedAt == null) {
            openedAt = Instant.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
