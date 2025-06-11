package org.example.chatbot.repository;

import org.example.chatbot.domain.HankyongNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HankyongNoticeRepository extends JpaRepository<HankyongNotice, Long> {

    List<HankyongNotice> findByNoticeDateContainingAndTitleContaining(String date, String title);

    List<HankyongNotice> findByNoticeDateContaining(String date);

    List<HankyongNotice> findByTitleContaining(String title);
}