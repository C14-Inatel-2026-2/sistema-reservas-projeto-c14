package com.reservas.sistemareservas.controller;

import com.reservas.sistemareservas.config.SenhaConfig;
import com.reservas.sistemareservas.dto.UsuarioResponse;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.exception.GlobalExceptionHandler;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import com.reservas.sistemareservas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buscar_comIdExistente_retorna200ComOUsuario() throws Exception {
        when(usuarioService.buscar(1L)).thenReturn(
                new UsuarioResponse(1L, "Ana", "ana@teste.com", TipoUsuario.ALUNO, LocalDateTime.now()));

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist());
    }

    @Test
    void criar_comDadosValidos_retorna201SemSenhaOuHash() throws Exception {
        when(usuarioService.criar(any())).thenReturn(
                new UsuarioResponse(1L, "Ana", "ana@teste.com", TipoUsuario.ALUNO, LocalDateTime.now()));
        String corpo = "{\"nome\":\"Ana\",\"email\":\"ana@teste.com\",\"senha\":\"123456\",\"tipo\":\"ALUNO\"}";

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist());
    }

    @Test
    void listar_comUsuariosCadastrados_retorna200SemSenhaOuHash() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(
                new UsuarioResponse(1L, "Ana", "ana@teste.com", TipoUsuario.ALUNO, LocalDateTime.now())));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].hash").doesNotExist());
    }

    @Test
    void criar_comSenhaAcimaDe72Bytes_retorna422ENaoSalva() throws Exception {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        UsuarioService service = new UsuarioService(repository, new SenhaConfig().passwordEncoder());
        MockMvc cadastro = MockMvcBuilders.standaloneSetup(new UsuarioController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String corpo = "{\"nome\":\"Ana\",\"email\":\"ana@teste.com\",\"senha\":\""
                + "á".repeat(37) + "\",\"tipo\":\"ALUNO\"}";

        cadastro.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("A senha deve ter no máximo 72 bytes em UTF-8."));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void buscar_comIdInexistente_retorna404() throws Exception {
        when(usuarioService.buscar(99L)).thenThrow(new EntidadeNaoEncontradaException("Usuário", 99L));

        mockMvc.perform(get("/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void criar_comEmailInvalido_retorna400() throws Exception {
        String corpo = "{\"nome\":\"Ana\",\"email\":\"invalido\",\"senha\":\"123456\",\"tipo\":\"ALUNO\"}";

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }
}
