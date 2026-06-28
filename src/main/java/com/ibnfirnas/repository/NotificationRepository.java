package com.ibnfirnas.repository;

import com.ibnfirnas.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByIsSentFalseAndIsDraftFalse();
}
