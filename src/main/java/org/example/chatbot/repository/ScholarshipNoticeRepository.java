package org.example.chatbot.repository;

import org.example.chatbot.domain.ScholarshipNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarshipNoticeRepository extends JpaRepository<ScholarshipNotice, Long> {

    List<ScholarshipNotice> findByNoticeDateContainingAndTitleContaining(String date, String title);

    List<ScholarshipNotice> findByNoticeDateContaining(String date);

    List<ScholarshipNotice> findByTitleContaining(String title);
}