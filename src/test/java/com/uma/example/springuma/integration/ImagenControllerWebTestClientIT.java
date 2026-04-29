
package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

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

import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.BodyInserters;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;
    private Paciente paciente;
    private Medico medico;

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

        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();
    }

    private void subirImagen(String nombreArchivo) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile()));
            builder.part("paciente", paciente);

            testClient.post().uri("/imagen")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange()
                    .expectStatus().isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Subir imagen y obtener su informacion")
    void subirImagen_yObtenerInfo() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/info/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("healthy.png")
                .jsonPath("$.paciente.nombre").isEqualTo("Maria");
    }

    @Test
    @DisplayName("Subir imagen y descargarla")
    void subirImagen_yDescargar() {
        subirImagen("healthy.png");

        FluxExchangeResult<byte[]> result = testClient.get().uri("/imagen/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .returnResult(byte[].class);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Subir imagen y obtenerla por paciente")
    void subirImagen_yObtenerPorPaciente() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].nombre").isEqualTo("healthy.png");
    }

    @Test
    @DisplayName("Subir varias imagenes y obtenerlas por paciente")
    void subirVariasImagenes_yObtenerPorPaciente() {
        subirImagen("healthy.png");
        subirImagen("no_healthty.png");

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("Subir imagen y eliminarla")
    void subirImagen_yEliminar() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/info/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("healthy.png");

        testClient.delete().uri("/imagen/1")
                .exchange()
                .expectStatus().isNoContent();

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Subir imagen y realizar prediccion con resultado aleatorio")
    void subirImagen_yRealizarPrediccion() {
        subirImagen("healthy.png");

        FluxExchangeResult<String> result = testClient.get().uri("/imagen/predict/1")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String body = result.getResponseBody().blockFirst();
        assertNotNull(body);
        assertTrue(body.toLowerCase().contains("cancer"));
        assertTrue(body.contains("score"));
    }

}
