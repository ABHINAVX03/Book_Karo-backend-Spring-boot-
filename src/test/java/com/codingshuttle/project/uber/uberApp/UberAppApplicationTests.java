package com.codingshuttle.project.uber.uberApp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import(TestContainerConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UberAppApplicationTests {

}
