package com.eagle.fusex.service;

import com.eagle.fusex.exception.BusinessException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.exception.TriagemNaoPreenchidaException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.dto.response.PreGuiaResumoResponse;
import com.eagle.fusex.model.entity.Encaminhamento;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.model.enums.StatusPreGuia;
import com.eagle.fusex.repository.EncaminhamentoRepository;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class PreGuiaService {

    private final TriagemPreGuiaRepository triagemRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final GuiaAssinaturaRepository guiaAssinaturaRepository;
    private final GuiaAssinaturaService guiaAssinaturaService;

    public PreGuiaService(TriagemPreGuiaRepository triagemRepository,
                          EncaminhamentoRepository encaminhamentoRepository,
                          SolicitacaoMedicaRepository solicitacaoRepository,
                          GuiaAssinaturaRepository guiaAssinaturaRepository,
                          GuiaAssinaturaService guiaAssinaturaService) {
        this.triagemRepository = triagemRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.guiaAssinaturaRepository = guiaAssinaturaRepository;
        this.guiaAssinaturaService = guiaAssinaturaService;
    }

    @Transactional(readOnly = true)
    public List<PreGuiaResumoResponse> listar(StatusPreGuia status) {
        List<TriagemPreGuia> triagens = triagemRepository.findPreGuiasDeSolicitacoesValidas();
        List<Long> ids = triagens.stream().map(t -> t.getSolicitacaoMedica().getId()).toList();

        Set<Long> comEncaminhamento = ids.isEmpty() ? Set.of() : encaminhamentoRepository.findBySolicitacaoMedicaIdIn(ids)
                .stream()
                .map(Encaminhamento::getSolicitacaoMedica)
                .map(SolicitacaoMedica::getId)
                .collect(Collectors.toSet());

        Map<Long, GuiaAssinatura> guiasNaEsteira = ids.isEmpty() ? Map.of() : guiaAssinaturaRepository
                .findBySolicitacaoMedicaIdIn(ids)
                .stream()
                .collect(Collectors.toMap(g -> g.getSolicitacaoMedica().getId(), g -> g, maisRelevante()));

        return triagens.stream()
                .map(t -> paraResumo(t, comEncaminhamento.contains(t.getSolicitacaoMedica().getId()),
                        guiasNaEsteira.get(t.getSolicitacaoMedica().getId())))
                .filter(r -> status == null || pertence(r, status))
                .sorted(Comparator.comparing(this::dataDeOrdenacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public PreGuiaResumoResponse confirmarEmissao(String token) {
        SolicitacaoMedica solicitacao = buscarSolicitacao(token);
        TriagemPreGuia triagem = buscarTriagem(solicitacao);
        if (solicitacao.getDataConfirmacaoEmissao() != null) {
            throw new BusinessException("A emissão da guia desta pré-guia já foi confirmada");
        }
        solicitacao.setDataConfirmacaoEmissao(LocalDateTime.now());
        solicitacaoRepository.save(solicitacao);
        return paraResumo(triagem, encaminhamentoRepository.existsBySolicitacaoMedicaId(solicitacao.getId()), null);
    }

    @Transactional
    public GuiaAssinaturaResponse enviarParaAssinatura(String token, MultipartFile arquivo) {
        SolicitacaoMedica solicitacao = buscarSolicitacao(token);
        TriagemPreGuia triagem = buscarTriagem(solicitacao);
        if (solicitacao.getDataConfirmacaoEmissao() == null) {
            throw new BusinessException("Confirme a emissão da guia na consulta de pré-guias antes de enviá-la para assinatura");
        }
        if (guiaAssinaturaRepository.existsBySolicitacaoMedicaIdAndStatusNot(solicitacao.getId(), StatusEsteiraAssinatura.REJEITADA)) {
            throw new BusinessException("A guia desta pré-guia já foi enviada para a esteira de assinaturas");
        }
        Paciente paciente = solicitacao.getPaciente();
        GuiaAssinaturaService.DadosPreGuia dados = new GuiaAssinaturaService.DadosPreGuia(
                solicitacao, paciente.getNome(), paciente.getPrec(), paciente.getEmail(), triagem.getOcs().getOcsNome());
        return guiaAssinaturaService.enviarGuiaParaAssinatura(arquivo, null, dados);
    }

    @Transactional(readOnly = true)
    public ArquivoLoteDownload abrirEncaminhamento(String token) {
        SolicitacaoMedica solicitacao = buscarSolicitacao(token);
        Encaminhamento encaminhamento = encaminhamentoRepository
                .findFirstBySolicitacaoMedicaIdOrderByDataHoraDesc(solicitacao.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Encaminhamento não encontrado"));
        Path caminho = Path.of(encaminhamento.getCaminhoArquivo());
        if (!Files.exists(caminho)) {
            throw new ResourceNotFoundException("Arquivo do encaminhamento não encontrado");
        }
        try {
            return new ArquivoLoteDownload(Files.readAllBytes(caminho), encaminhamento.getNomeArquivo(),
                    encaminhamento.getTipoArquivo());
        } catch (IOException e) {
            throw new ResourceNotFoundException("Não foi possível ler o arquivo do encaminhamento");
        }
    }

    private SolicitacaoMedica buscarSolicitacao(String token) {
        return solicitacaoRepository.findByTokenPublico(token)
                .filter(SolicitacaoMedica::getValida)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException("Solicitação não encontrada"));
    }

    private TriagemPreGuia buscarTriagem(SolicitacaoMedica solicitacao) {
        return triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())
                .orElseThrow(() -> new TriagemNaoPreenchidaException("A triagem desta pré-guia ainda não foi preenchida"));
    }

    private BinaryOperator<GuiaAssinatura> maisRelevante() {
        return (a, b) -> {
            boolean aRejeitada = a.getStatus() == StatusEsteiraAssinatura.REJEITADA;
            boolean bRejeitada = b.getStatus() == StatusEsteiraAssinatura.REJEITADA;
            if (aRejeitada != bRejeitada) {
                return aRejeitada ? b : a;
            }
            return a.getDataEnvio().isAfter(b.getDataEnvio()) ? a : b;
        };
    }

    private boolean pertence(PreGuiaResumoResponse resumo, StatusPreGuia status) {
        return switch (status) {
            case PENDENTE -> resumo.dataEmissao() == null;
            case EMITIDA -> resumo.dataEmissao() != null;
            case LIBERADA -> resumo.dataEmissao() != null && !resumo.enviadaParaAssinatura();
        };
    }

    private LocalDateTime dataDeOrdenacao(PreGuiaResumoResponse resumo) {
        return resumo.dataEmissao() != null ? resumo.dataEmissao() : resumo.dataRegistro();
    }

    private PreGuiaResumoResponse paraResumo(TriagemPreGuia triagem, boolean possuiEncaminhamento, GuiaAssinatura guia) {
        SolicitacaoMedica solicitacao = triagem.getSolicitacaoMedica();
        boolean enviada = guia != null && guia.getStatus() != StatusEsteiraAssinatura.REJEITADA;
        return new PreGuiaResumoResponse(
                solicitacao.getId(),
                solicitacao.getTokenPublico(),
                solicitacao.getPaciente().getNome(),
                solicitacao.getPaciente().getPrec(),
                triagem.getOcs().getOcsNome(),
                triagem.getDataCriacao(),
                solicitacao.getDataConfirmacaoEmissao(),
                possuiEncaminhamento,
                enviada,
                guia != null ? guia.getNumeroGuia() : null,
                guia != null ? guia.getStatus().getDescricao() : null
        );
    }
}
