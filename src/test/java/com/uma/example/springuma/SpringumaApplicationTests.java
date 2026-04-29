package com.uma.example.springuma;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

import com.uma.example.springuma.integration.MedicoControllerMockMvcIT;
import com.uma.example.springuma.integration.PacienteControllerMockMvcIT;
import com.uma.example.springuma.integration.ImagenControllerWebTestClientIT;
import com.uma.example.springuma.integration.InformeControllerWebTestClientIT;

@Suite
@SelectClasses({
    MedicoControllerMockMvcIT.class,
    PacienteControllerMockMvcIT.class,
    ImagenControllerWebTestClientIT.class,
    InformeControllerWebTestClientIT.class
})
@SpringBootTest
class SpringumaApplicationTests {

    @Test
    void contextLoads() {

    }

}
