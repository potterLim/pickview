package com.pickview.repository;

import com.pickview.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface INoticeRepository extends JpaRepository<Notice, String> {

}
