package com.jjikmeok.app.domain.s3.controller;

import com.jjikmeok.app.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface S3Repository extends JpaRepository<Image, Long> {

}