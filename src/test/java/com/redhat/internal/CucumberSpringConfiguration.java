package com.redhat.internal;

import org.springframework.boot.test.context.SpringBootTest;

import com.redhat.internal.kie.Application;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = Application.class)
public class CucumberSpringConfiguration {
}
