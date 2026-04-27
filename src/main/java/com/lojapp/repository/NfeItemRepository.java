package com.lojapp.repository;

import com.lojapp.entity.NfeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NfeItemRepository extends JpaRepository<NfeItem, Long> {

    @Query(
            "select i from NfeItem i join i.nfeEntry e where e.id = :entryId and e.user.id = :userId")
    List<NfeItem> findByNfeEntryIdAndUserId(
            @Param("entryId") long entryId, @Param("userId") long userId);
}
