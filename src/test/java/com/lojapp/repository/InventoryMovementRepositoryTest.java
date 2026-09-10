package com.lojapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.entity.InventoryMovement;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class InventoryMovementRepositoryTest {

    @Autowired private InventoryMovementRepository movements;
    @Autowired private TestEntityManager em;

    @Test
    void findByUserAndProduct_ordersNewestFirstAndIgnoresOtherTenant() {
        User owner = persistUser("owner-kardex@unit.test");
        User other = persistUser("other-kardex@unit.test");
        Product owned = persistProduct(owner, "Camisa A");
        Product foreign = persistProduct(other, "Camisa B");

        InventoryMovement older = persistMovement(owner, owned, "ENTRY", "1", Instant.parse("2026-09-01T10:00:00Z"));
        InventoryMovement newer = persistMovement(owner, owned, "SALE", "-1", Instant.parse("2026-09-10T14:32:00Z"));
        persistMovement(other, foreign, "ENTRY", "9", Instant.parse("2026-09-10T18:00:00Z"));

        em.flush();
        em.clear();

        Page<InventoryMovement> page =
                movements.findByUser_IdAndProduct_IdOrderByCreatedAtDesc(
                        owner.getId(), owned.getId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(InventoryMovement::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("noop");
        user.setAppRole("USER");
        em.persist(user);
        return user;
    }

    private Product persistProduct(User user, String name) {
        Product product = new Product();
        product.setUser(user);
        product.setName(name);
        product.setCostPrice(BigDecimal.ONE);
        product.setSalePrice(BigDecimal.TEN);
        product.setMinimumStock(BigDecimal.ZERO);
        em.persist(product);
        return product;
    }

    private InventoryMovement persistMovement(
            User user, Product product, String type, String qty, Instant createdAt) {
        InventoryMovement m = new InventoryMovement();
        m.setUser(user);
        m.setProduct(product);
        m.setMovementType(type);
        m.setQuantity(new BigDecimal(qty));
        m.setSource("TEST");
        m.setCreatedAt(createdAt);
        em.persist(m);
        return m;
    }
}
