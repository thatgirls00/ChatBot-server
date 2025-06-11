package org.example.chatbot.repository;

import org.example.chatbot.domain.AcademicNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicNoticeRepository extends JpaRepository<AcademicNotice, Long> {

    List<AcademicNotice> findByNoticeDateContainingAndTitleContaining(String noticeDate, String title);

    List<AcademicNotice> findByNoticeDateContaining(String noticeDate);

    List<AcademicNotice> findByTitleContaining(String title);
}