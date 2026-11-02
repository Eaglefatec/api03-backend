package com.eagle.fusex.service;

import com.eagle.fusex.exception.MedicoInvalidoException;
import com.eagle.fusex.exception.ProcedimentoInvalidoException;
import com.eagle.fusex.model.dto.request.CriarSolicitacaoRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.ProcedimentoRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitacaoMedicaServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    @Mock
    private SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;

    @Mock
    private SolicitacaoEmailService emailService;

    @InjectMocks
    private SolicitacaoMedicaService service;

    private Medico medicoCredenciado;
    private Medico medicoResponsavel;
    private Medico medicoNaoCredenciado;
    private Paciente paciente;
    private Procedimento procedimento;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        medicoCredenciado = new Medico("Dr. João Silva", "123456", true);
        medicoCredenciado.setId(1L);

        medicoResponsavel = new Medico("Dr. Carlos Eduardo", "998877", true);
        medicoResponsavel.setId(3L);

        medicoNaoCredenciado = new Medico("Dra. Maria Santos", "654321", false);
        medicoNaoCredenciado.setId(2L);

        paciente = new Paciente("João Silva", "12345678901", "12345", "OM001", "joao@email.com");
        paciente.setId(1L);

        procedimento = new Procedimento("DGP001", "Consulta eletiva", 1);

        when(procedimentoRepository.findById("DGP001")).thenReturn(Optional.of(procedimento));
    }

    private List<CriarSolicitacaoRequest.ProcedimentoQuantidade> umProcedimento() {
        CriarSolicitacaoRequest.ProcedimentoQuantidade pq = new CriarSolicitacaoRequest.ProcedimentoQuantidade();
        pq.setProcedimentoCodigoDgp("DGP001");
        pq.setQuantidade(1);
        return List.of(pq);
    }

    private CriarSolicitacaoRequest criarRequestValido() {
        Set<Especialidade> especialidades = new HashSet<>();
        especialidades.add(Especialidade.CARDIOLOGISTA);

        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest();
        request.setMedicoId(1L);
        request.setNomePaciente("João Silva");
        request.setCpf("12345678901");
        request.setPrec("12345");
        request.setEmail("joao@email.com");
        request.setOm("OM001");
        request.setEspecialidades(especialidades);
        request.setProcedimentos(umProcedimento());
        return request;
    }

    @Test
    void testCriarSolicitacao_MedicoNaoCredenciado_LancaException() {
        when(medicoRepository.findByIdAndCredenciadoTrue(2L))
                .thenReturn(Optional.empty());

        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest();
        request.setMedicoId(2L);

        assertThrows(MedicoInvalidoException.class, () -> service.criar(request));
    }

    @Test
    void testCriarSolicitacao_PacienteNovo_CriaComSucesso() {
        CriarSolicitacaoRequest request = criarRequestValido();

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.empty());
        when(pacienteRepository.save(any(Paciente.class)))
                .thenReturn(paciente);
        when(solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(1L, 1L))
                .thenReturn(List.of());

        SolicitacaoMedica solicitacaoSalva = new SolicitacaoMedica();
        solicitacaoSalva.setId(1L);
        solicitacaoSalva.setTokenPublico("token-uuid");

        when(solicitacaoRepository.save(any(SolicitacaoMedica.class)))
                .thenReturn(solicitacaoSalva);

        SolicitacaoResponse response = service.criar(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("token-uuid", response.getTokenPublico());
        assertTrue(response.getLinkBeneficiario().contains("/solicitacoes/publico/"));
        assertTrue(response.getQrCodeUrl().contains("/qrcode"));
    }

    @Test
    void testCriarSolicitacao_PacienteExistente_PreservaDadosNaoNulos() {
        CriarSolicitacaoRequest request = criarRequestValido();
        request.setNomePaciente("Tentativa Sobrescrita");
        request.setOm("OM_ALTERADA");

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class)))
                .thenReturn(paciente);
        when(solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(1L, 1L))
                .thenReturn(List.of());

        SolicitacaoMedica solicitacaoSalva = new SolicitacaoMedica();
        solicitacaoSalva.setId(2L);
        solicitacaoSalva.setTokenPublico("token-uuid-2");

        when(solicitacaoRepository.save(any(SolicitacaoMedica.class)))
                .thenReturn(solicitacaoSalva);

        SolicitacaoResponse response = service.criar(request);

        assertNotNull(response);
        assertEquals("Tentativa Sobrescrita", paciente.getNome());
        assertEquals("OM_ALTERADA", paciente.getOm());
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    void testCriarSolicitacao_MultiplasSolicitacoesAnteriores_InvalidaTodasSemExcecao() {
        CriarSolicitacaoRequest request = criarRequestValido();

        SolicitacaoMedica anterior1 = new SolicitacaoMedica();
        anterior1.setId(98L);
        anterior1.setValida(true);

        SolicitacaoMedica anterior2 = new SolicitacaoMedica();
        anterior2.setId(99L);
        anterior2.setValida(true);

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class)))
                .thenReturn(paciente);
        when(solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(1L, 1L))
                .thenReturn(List.of(anterior1, anterior2));

        SolicitacaoMedica solicitacaoNova = new SolicitacaoMedica();
        solicitacaoNova.setId(3L);
        solicitacaoNova.setTokenPublico("token-uuid-3");

        when(solicitacaoRepository.save(any(SolicitacaoMedica.class)))
                .thenReturn(solicitacaoNova);

        SolicitacaoResponse response = service.criar(request);

        assertNotNull(response);
        assertFalse(anterior1.getValida());
        assertFalse(anterior2.getValida());
        verify(solicitacaoRepository, times(3)).save(any(SolicitacaoMedica.class));
    }

    @Test
    void testCriarSolicitacao_ComMedicoResponsavel_SalvaComSucesso() {
        CriarSolicitacaoRequest request = criarRequestValido();
        request.setMedicoResponsavelId(3L);

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(medicoRepository.findById(3L))
                .thenReturn(Optional.of(medicoResponsavel));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class)))
                .thenReturn(paciente);
        when(solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(1L, 1L))
                .thenReturn(List.of());

        SolicitacaoMedica solicitacaoSalva = new SolicitacaoMedica();
        solicitacaoSalva.setId(5L);
        solicitacaoSalva.setMedicoResponsavel(medicoResponsavel);
        solicitacaoSalva.setTokenPublico("token-uuid-5");

        when(solicitacaoRepository.save(any(SolicitacaoMedica.class)))
                .thenReturn(solicitacaoSalva);

        SolicitacaoResponse response = service.criar(request);

        assertNotNull(response);
        verify(medicoRepository).findById(3L);
    }

    @Test
    void testCriarSolicitacao_ComMedicoResponsavelInvalido_LancaException() {
        CriarSolicitacaoRequest request = criarRequestValido();
        request.setMedicoResponsavelId(999L);

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(MedicoInvalidoException.class, () -> service.criar(request));
    }

    @Test
    void testCriarSolicitacao_ProcedimentoInvalido_LancaException() {
        CriarSolicitacaoRequest.ProcedimentoQuantidade pq = new CriarSolicitacaoRequest.ProcedimentoQuantidade();
        pq.setProcedimentoCodigoDgp("INEXISTENTE");
        pq.setQuantidade(1);

        CriarSolicitacaoRequest request = criarRequestValido();
        request.setProcedimentos(List.of(pq));

        when(medicoRepository.findByIdAndCredenciadoTrue(1L))
                .thenReturn(Optional.of(medicoCredenciado));
        when(pacienteRepository.findByCpfOrPrec(any(), any()))
                .thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class)))
                .thenReturn(paciente);
        when(solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(1L, 1L))
                .thenReturn(List.of());
        when(procedimentoRepository.findById("INEXISTENTE"))
                .thenReturn(Optional.empty());

        SolicitacaoMedica solicitacaoSalva = new SolicitacaoMedica();
        solicitacaoSalva.setId(1L);
        solicitacaoSalva.setTokenPublico("token-uuid");

        when(solicitacaoRepository.save(any(SolicitacaoMedica.class)))
                .thenReturn(solicitacaoSalva);

        assertThrows(ProcedimentoInvalidoException.class, () -> service.criar(request));
    }

    @Test
    void testReenviarEmail_ComNovoEmail_AtualizaPacienteEDispara() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(10L);
        solicitacao.setTokenPublico("token-reenvio");
        solicitacao.setPaciente(paciente);
        solicitacao.setEmailDestinatario("antigo@email.com");

        when(solicitacaoRepository.findById(10L)).thenReturn(Optional.of(solicitacao));
        when(emailService.enviarEmailSolicitacao(any()))
                .thenReturn(com.eagle.fusex.model.dto.ResultadoEnvioEmail.sucesso("novo@email.com"));

        SolicitacaoResponse response = service.reenviarEmail(10L, "novo@email.com");

        assertNotNull(response);
        assertTrue(response.getEmailEnviado());
        assertEquals("novo@email.com", paciente.getEmail());
        assertEquals("novo@email.com", solicitacao.getEmailDestinatario());
        assertEquals(com.eagle.fusex.model.enums.StatusEmail.ENVIADO, solicitacao.getStatusEmail());
        verify(pacienteRepository).save(paciente);
        verify(solicitacaoRepository).save(solicitacao);
    }

    @Test
    void testReenviarEmail_SolicitacaoNaoEncontrada_LancaException() {
        when(solicitacaoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.reenviarEmail(999L, "novo@email.com"));
    }
}
