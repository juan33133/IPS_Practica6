package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.MedicoService;
import com.uma.example.springuma.model.Paciente;

public class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicoService medicoService;

    Paciente paciente;
    Medico medico;

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
        paciente.setMedico(this.medico);
    }
    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }
    private void crearPaciente(Paciente paciente) throws Exception {
        mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());
    }

    private void getPacienteById(Long id, Paciente expected) throws Exception {
        mockMvc.perform(get("/paciente/" + id))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$").value(expected));
    }

    @Test
    @DisplayName("Crear paciente y recuperarlo por ID pasado por parametro")
    void savePaciente_RecuperaPacientePorId() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        //Obtener paciente por ID
        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Maria"))
                .andExpect(jsonPath("$.dni").value("888"))
                .andExpect(jsonPath("$.edad").value(20))
                .andExpect(jsonPath("$.cita").value("Ginecologia"))
                .andExpect(jsonPath("$.medico.nombre").value("Miguel"));
    }

    @Test
    @DisplayName("Crear paciente asociado a medico y recuperar por medico")
    void crearPacienteAsociadoAMedico_yRecuperarPorMedico() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Recuperar pacientes del medico
        mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Maria"))
                .andExpect(jsonPath("$[0].dni").value("888"))
                .andExpect(jsonPath("$[0].medico.dni").value("835"));
    }

    @Test
    @DisplayName("Actualizar paciente y verificar los cambios")
    void actualizarPaciente_yVerificar() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Actualizar paciente
        paciente.setNombre("Maria Actualizada");
        paciente.setCita("Traumatologia");
        paciente.setEdad(25);

        mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        // Verificar cambios
        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.nombre").value("Maria Actualizada"))
                .andExpect(jsonPath("$.cita").value("Traumatologia"))
                .andExpect(jsonPath("$.edad").value(25))
                .andExpect(jsonPath("$.dni").value("888"));
    }

    @Test
    @DisplayName("Eliminar paciente y verificar que ya no existe")
    void eliminarPaciente_yVerificarQueNoExiste() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Verificar que existe
        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.dni").value("888"));

        // Eliminar
        mockMvc.perform(delete("/paciente/" + paciente.getId()))
                .andExpect(status().isOk());

        // Verificar que ya no existe
        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Asociar varios pacientes al mismo medico y listarlos")
    void asociarVariosPacientesAMedico() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Crear segundo paciente asociado al mismo medico
        Paciente paciente2 = new Paciente();
        paciente2.setId(2L);
        paciente2.setNombre("Ana");
        paciente2.setDni("999");
        paciente2.setEdad(30);
        paciente2.setCita("Revision");
        paciente2.setMedico(medico);
        crearPaciente(paciente2);

        // Verificar que ambos pacientes pertenecen al medico
        mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Editar el medico de un paciente")
    void editarMedicoDePaciente() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Crear un segundo medico
        Medico medico2 = new Medico();
        medico2.setId(2L);
        medico2.setDni("222");
        medico2.setNombre("Carlos");
        medico2.setEspecialidad("Traumatologia");
        crearMedico(medico2);

        // Cambiar el medico del paciente
        paciente.setMedico(medico2);
        mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        // Verificar que el paciente tiene el nuevo medico
        mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.medico.nombre").value("Carlos"))
                .andExpect(jsonPath("$.medico.dni").value("222"));

        // Verificar que el medico original ya no tiene pacientes
        mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verificar que el nuevo medico tiene el paciente
        mockMvc.perform(get("/paciente/medico/" + medico2.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Maria"));
    }

}
