package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.OcsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OcsCadastroLoaderTest {

    @Mock
    private OcsRepository ocsRepository;

    private OcsCadastroLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loader = new OcsCadastroLoader(ocsRepository, "data/ocs_cadastro.csv",
                "src/test/resources/data/ocs_cadastro.csv");
    }

    @Test
    void testRun_AtualizaOcsExistentesSemCnpjPrevio() throws Exception {
        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.of(new Ocs()));
        when(ocsRepository.findByOcsNome("CLINICA SEM CADASTRO PREVIO")).thenReturn(Optional.of(new Ocs()));
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loader.run();

        verify(ocsRepository, times(2)).save(any(Ocs.class));
    }

    @Test
    void testRun_OcsNaoEncontradaPorNome_NaoAtualiza() throws Exception {
        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.of(new Ocs()));
        when(ocsRepository.findByOcsNome("CLINICA SEM CADASTRO PREVIO")).thenReturn(Optional.empty());
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loader.run();

        verify(ocsRepository, times(1)).save(any(Ocs.class));
    }

    @Test
    void testRun_OcsJaTemCnpj_NaoSobrescreve() throws Exception {
        Ocs ocsComCnpj = new Ocs();
        ocsComCnpj.setOcsInscricaoFederal("00000000000000");
        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.of(ocsComCnpj));
        when(ocsRepository.findByOcsNome("CLINICA SEM CADASTRO PREVIO")).thenReturn(Optional.empty());

        loader.run();

        verify(ocsRepository, never()).save(any(Ocs.class));
    }

    @Test
    void testRun_PreencheTodosOsCamposCadastrais() throws Exception {
        Ocs ocs = new Ocs();
        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.of(ocs));
        when(ocsRepository.findByOcsNome("CLINICA SEM CADASTRO PREVIO")).thenReturn(Optional.empty());
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loader.run();

        assertEquals("18674947000224", ocs.getOcsInscricaoFederal());
        assertEquals("Rua Teste", ocs.getOcsEndereco());
        assertEquals("100", ocs.getOcsEnderecoNumero());
        assertEquals("Centro", ocs.getOcsEnderecoBairro());
        assertEquals("Cidade Teste", ocs.getOcsEnderecoCidade());
        assertEquals("SP", ocs.getOcsEnderecoUf());
        assertEquals("12000000", ocs.getOcsEnderecoCep());
        assertEquals("1234567890", ocs.getOcsContatoTelefone());
    }
}
