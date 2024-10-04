package com.astrazeneca.weathermicroservice.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestDoneRepository extends JpaRepository<RequestDone, Integer> {
}
