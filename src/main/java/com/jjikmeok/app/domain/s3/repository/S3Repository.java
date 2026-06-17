package com.jjikmeok.app.domain.s3.repository;

import com.jjikmeok.app.domain.s3.Entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3Repository extends JpaRepository<Image, Long> {

}