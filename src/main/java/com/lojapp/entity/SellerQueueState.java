package com.lojapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seller_queue_state")
@Getter
@Setter
@NoArgsConstructor
public class SellerQueueState {

    @Id
    @Column(name = "cash_session_id")
    private Long cashSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_assigned_seller_id")
    private Seller lastAssignedSeller;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
