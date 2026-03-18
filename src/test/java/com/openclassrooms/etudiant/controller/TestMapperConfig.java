package com.openclassrooms.etudiant.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Configuration de test pour s'assurer que le package des mappers MapStruct
 * est bien scanné (les implémentations générées sont dans com.openclassrooms.etudiant.mapper).
 */
@TestConfiguration
@ComponentScan(basePackages = "com.openclassrooms.etudiant.mapper")
public class TestMapperConfig {
}
