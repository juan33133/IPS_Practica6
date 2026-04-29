package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000)).build();
    }

    @BeforeEach
    void setUp() {

        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setId(1L);
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(medico);

        imagen = new Imagen();
        imagen.setId(1L);
        imagen.setPaciente(paciente);

        // Crea médico
        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea paciente
        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea imagen
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(Paths.get("src/test/resources/healthy.png").toFile()));
        builder.part("paciente", paciente);

        testClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

    }

    private Informe crearInformeObj(String contenido) {
        Informe inf = new Informe();
        inf.setContenido(contenido);
        inf.setImagen(imagen);
        return inf;
    }

    private void crearInforme(Informe inf) {
        testClient.post().uri("/informe")
                .body(Mono.just(inf), Informe.class)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Crear informe y recuperarlo por ID")
    void crearInforme_yRecuperarloPorId() {
        Informe inf = crearInformeObj("Informe de prueba para paciente Maria");
        crearInforme(inf);

        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.contenido").isEqualTo("Informe de prueba para paciente Maria")
                .jsonPath("$.prediccion").isNotEmpty();
    }

    @Test
    @DisplayName("Crear informe y recuperar por imagen")
    void crearInforme_yRecuperarPorImagen() {
        Informe inf = crearInformeObj("Informe analisis imagen");
        crearInforme(inf);

        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].contenido").isEqualTo("Informe analisis imagen");
    }

    @Test
    @DisplayName("Crear informe y eliminarlo")
    void crearInforme_yEliminar() {
        Informe inf = crearInformeObj("Informe a eliminar");
        crearInforme(inf);

        // Verificar que existe
        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.contenido").isEqualTo("Informe a eliminar");

        // Eliminar
        testClient.delete().uri("/informe/1")
                .exchange()
                .expectStatus().isNoContent();

        // Verificar que ya no existe
        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Crear varios informes y recuperar por imagen")
    void crearVariosInformes_yRecuperarPorImagen() {
        Informe inf1 = crearInformeObj("Primer informe");
        Informe inf2 = crearInformeObj("Segundo informe");
        crearInforme(inf1);
        crearInforme(inf2);

        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("Crear informe y verificar prediccion aleatoria")
    void crearInforme_verificarPrediccionAleatoria() {
        Informe inf = crearInformeObj("Informe con prediccion");
        crearInforme(inf);

        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.prediccion").isNotEmpty()
                .jsonPath("$.prediccion").value(prediccion -> {
                    String pred = (String) prediccion;
                    assertTrue(pred.toLowerCase().contains("cancer"),
                            "El informe debe contener prediccion Cancer o Not cancer, pero fue: " + pred);
                    assertTrue(pred.contains("score"),
                            "El informe debe contener un score, pero fue: " + pred);
                });
    }
}
