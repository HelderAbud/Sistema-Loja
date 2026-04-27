package com.lojapp.repository;

import com.lojapp.entity.NfeEntry;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NfeEntryRepository extends JpaRepository<NfeEntry, Long> {

    boolean existsByUser_IdAndAccessKey(Long userId, String accessKey);

    boolean existsByUser_IdAndContentFingerprint(Long userId, String contentFingerprint);

    Optional<NfeEntry> findByIdAndUser_Id(long id, long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update NfeEntry e
               set e.rawXml = null,
                   e.rawXmlKey = null
             where (e.rawXml is not null or e.rawXmlKey is not null)
               and e.importedAt < :cutoff
            """)
    int clearRawXmlOlderThan(@Param("cutoff") Instant cutoff);
}
