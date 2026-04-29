package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private Medico medico;

        @BeforeEach
        void setUp() {
                medico = new Medico();
                medico.setId(1L);
                medico.setDni("835");
                medico.setNombre("Miguel");
                medico.setEspecialidad("Ginecologia");
        }

        private void crearMedico(Medico medico) throws Exception {
                this.mockMvc.perform(post("/medico")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(medico)))
                                .andExpect(status().isCreated());
        }

        private void errorAlCrearMedicoDuplicado(Medico medico) throws Exception {
                this.mockMvc.perform(post("/medico")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(medico)))
                                .andExpect(status().isCreated());
                this.mockMvc.perform(post("/medico")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(medico)))
                                .andExpect(status().isInternalServerError());

        }

        @Test
        @DisplayName("Crear un medico y recuperarlo por su ID")
        void crearMedico_yRecuperarloPorId() throws Exception {
                crearMedico(medico);

                this.mockMvc.perform(get("/medico/" + medico.getId()))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(content().contentType("application/json"))
                                .andExpect(jsonPath("$.nombre").value("Miguel"))
                                .andExpect(jsonPath("$.dni").value("835"))
                                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
        }

        @Test
        @DisplayName("Crear un medico y recuperarlo por su DNI")
        void crearMedico_yRecuperarloPorDni() throws Exception {
                crearMedico(medico);

                this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType("application/json"))
                                .andExpect(jsonPath("$.nombre").value("Miguel"))
                                .andExpect(jsonPath("$.dni").value("835"))
                                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
        }

        @Test
        @DisplayName("Actualizar un medico y verificar los cambios")
        void actualizarMedico_yVerificarCambios() throws Exception {
                crearMedico(medico);

                // Actualizar especialidad
                medico.setEspecialidad("Traumatologia");
                medico.setNombre("Miguel Angel");

                this.mockMvc.perform(put("/medico")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(medico)))
                                .andExpect(status().isNoContent());

                // Verificar los cambios
                this.mockMvc.perform(get("/medico/" + medico.getId()))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.nombre").value("Miguel Angel"))
                                .andExpect(jsonPath("$.especialidad").value("Traumatologia"))
                                .andExpect(jsonPath("$.dni").value("835"));
        }

        @Test
        @DisplayName("Eliminar un medico y verificar que ya no existe")
        void eliminarMedico_yVerificarQueNoExiste() throws Exception {
                crearMedico(medico);

                // Verificar que existe
                this.mockMvc.perform(get("/medico/" + medico.getId()))
                                .andExpect(status().is2xxSuccessful())
                                .andExpect(jsonPath("$.dni").value("835"));

                // Eliminar
                this.mockMvc.perform(delete("/medico/" + medico.getId()))
                                .andExpect(status().isOk());

                // Verificar que ya no existe
                this.mockMvc.perform(get("/medico/" + medico.getId()))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("error al eliminar médico con id no existente")
        void errorAlEliminarMedicoConIdNoExistente() throws Exception {
                this.mockMvc.perform(delete("/medico/" + 1))
                                .andExpect(status().isInternalServerError());
        }

}
