package com.lojapp.application.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lojapp.config.IdempotencyProperties;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.sale.PosSaleFinalizeResponse;
import com.lojapp.dto.sale.SaleCreatedResponse;
import com.lojapp.entity.ApiIdempotency;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.observability.LojappBusinessMetrics;
import com.lojapp.repository.ApiIdempotencyRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.util.TokenHashUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiIdempotencyService {

    private static final String VOID_BODY_JSON = "{}";

    private final ApiIdempotencyRepository repository;
    private final UserRepository users;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties properties;
    private final LojappBusinessMetrics businessMetrics;

    public ApiIdempotencyService(
            ApiIdempotencyRepository repository,
            UserRepository users,
            ObjectMapper objectMapper,
            IdempotencyProperties properties,
            LojappBusinessMetrics businessMetrics) {
        this.repository = repository;
        this.users = users;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public SaleCreatedResponse runSaleCreate(
            long userId,
            Optional<String> idempotencyHeader,
            String requestHash,
            Supplier<SaleCreatedResponse> create) {
        Optional<String> keyOpt = normalizeKey(idempotencyHeader);
        if (keyOpt.isEmpty()) {
            return create.get();
        }
        String keyHash = TokenHashUtil.sha256Hex(keyOpt.get());
        int k1 = lockKey1(userId);
        int k2 = lockKey2(ApiIdempotencyScope.SALE_REGISTER, keyHash);
        repository.advisoryXactLock(k1, k2);

        Instant now = Instant.now();
        Optional<ApiIdempotency> existing =
                repository.findByUser_IdAndScopeAndKeyHash(
                        userId, ApiIdempotencyScope.SALE_REGISTER.name(), keyHash);

        if (existing.isPresent()) {
            ApiIdempotency row = existing.get();
            if (isExpired(row.getCreatedAt(), now)) {
                repository.delete(row);
            } else {
                if (!row.getRequestHash().equals(requestHash)) {
                    throw new LojappDomainException(
                            ApiErrorCode.CONFLICT,
                            "Idempotency-Key reutilizada com corpo de pedido diferente.");
                }
                businessMetrics.recordIdempotencyReplay(ApiIdempotencyScope.SALE_REGISTER);
                return readSaleResponse(row.getResponseJson());
            }
        }

        SaleCreatedResponse created = create.get();
        String json = writeSaleResponse(created);
        persistRow(userId, ApiIdempotencyScope.SALE_REGISTER, keyHash, requestHash, json, now);
        return created;
    }

    @Transactional
    public void runStockAdjust(
            long userId,
            Optional<String> idempotencyHeader,
            String requestHash,
            Runnable apply) {
        Optional<String> keyOpt = normalizeKey(idempotencyHeader);
        if (keyOpt.isEmpty()) {
            apply.run();
            return;
        }
        String keyHash = TokenHashUtil.sha256Hex(keyOpt.get());
        int k1 = lockKey1(userId);
        int k2 = lockKey2(ApiIdempotencyScope.STOCK_ADJUST, keyHash);
        repository.advisoryXactLock(k1, k2);

        Instant now = Instant.now();
        Optional<ApiIdempotency> existing =
                repository.findByUser_IdAndScopeAndKeyHash(
                        userId, ApiIdempotencyScope.STOCK_ADJUST.name(), keyHash);

        if (existing.isPresent()) {
            ApiIdempotency row = existing.get();
            if (isExpired(row.getCreatedAt(), now)) {
                repository.delete(row);
            } else {
                if (!row.getRequestHash().equals(requestHash)) {
                    throw new LojappDomainException(
                            ApiErrorCode.CONFLICT,
                            "Idempotency-Key reutilizada com corpo de pedido diferente.");
                }
                businessMetrics.recordIdempotencyReplay(ApiIdempotencyScope.STOCK_ADJUST);
                return;
            }
        }

        apply.run();
        persistRow(userId, ApiIdempotencyScope.STOCK_ADJUST, keyHash, requestHash, VOID_BODY_JSON, now);
    }

    @Transactional
    public PosSaleFinalizeResponse runPosSaleFinalize(
            long userId,
            Optional<String> idempotencyHeader,
            String requestHash,
            Supplier<PosSaleFinalizeResponse> create) {
        Optional<String> keyOpt = normalizeKey(idempotencyHeader);
        if (keyOpt.isEmpty()) {
            return create.get();
        }
        String keyHash = TokenHashUtil.sha256Hex(keyOpt.get());
        int k1 = lockKey1(userId);
        int k2 = lockKey2(ApiIdempotencyScope.POS_SALE_FINALIZE, keyHash);
        repository.advisoryXactLock(k1, k2);

        Instant now = Instant.now();
        Optional<ApiIdempotency> existing =
                repository.findByUser_IdAndScopeAndKeyHash(
                        userId, ApiIdempotencyScope.POS_SALE_FINALIZE.name(), keyHash);

        if (existing.isPresent()) {
            ApiIdempotency row = existing.get();
            if (isExpired(row.getCreatedAt(), now)) {
                repository.delete(row);
            } else {
                if (!row.getRequestHash().equals(requestHash)) {
                    throw new LojappDomainException(
                            ApiErrorCode.CONFLICT,
                            "Idempotency-Key reutilizada com corpo de pedido diferente.");
                }
                businessMetrics.recordIdempotencyReplay(ApiIdempotencyScope.POS_SALE_FINALIZE);
                return readPosSaleResponse(row.getResponseJson());
            }
        }

        PosSaleFinalizeResponse created = create.get();
        String json = writePosSaleResponse(created);
        persistRow(userId, ApiIdempotencyScope.POS_SALE_FINALIZE, keyHash, requestHash, json, now);
        return created;
    }

    private void persistRow(
            long userId,
            ApiIdempotencyScope scope,
            String keyHash,
            String requestHash,
            String responseJson,
            Instant createdAt) {
        ApiIdempotency row = new ApiIdempotency();
        row.setUser(users.getReferenceById(userId));
        row.setScope(scope.name());
        row.setKeyHash(keyHash);
        row.setRequestHash(requestHash);
        row.setResponseJson(responseJson);
        row.setCreatedAt(createdAt);
        repository.save(row);
    }

    private SaleCreatedResponse readSaleResponse(String json) {
        try {
            return objectMapper.readValue(json, SaleCreatedResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta idempotência corrupta", e);
        }
    }

    private String writeSaleResponse(SaleCreatedResponse created) {
        try {
            return objectMapper.writeValueAsString(created);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialização idempotência venda", e);
        }
    }

    private PosSaleFinalizeResponse readPosSaleResponse(String json) {
        try {
            return objectMapper.readValue(json, PosSaleFinalizeResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta idempotência PDV corrupta", e);
        }
    }

    private String writePosSaleResponse(PosSaleFinalizeResponse created) {
        try {
            return objectMapper.writeValueAsString(created);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialização idempotência PDV", e);
        }
    }

    private boolean isExpired(Instant createdAt, Instant now) {
        return createdAt.plus(Duration.ofHours(properties.ttlHours())).isBefore(now);
    }

    private Optional<String> normalizeKey(Optional<String> header) {
        if (header.isEmpty()) {
            return Optional.empty();
        }
        String t = header.get().trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }
        if (t.length() > properties.maxKeyLength()) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "Idempotency-Key excede o tamanho máximo permitido.");
        }
        return Optional.of(t);
    }

    private static int lockKey1(long userId) {
        return (int) (userId ^ (userId >>> 32));
    }

    private static int lockKey2(ApiIdempotencyScope scope, String keyHash) {
        return Objects.hash(scope, keyHash);
    }
}
