package com.eagle.fusex.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MedicoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleMedicoInvalidoException(MedicoInvalidoException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleArquivoInvalidoException(ArquivoInvalidoException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PacienteNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handlePacienteNaoEncontradoException(PacienteNaoEncontradoException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errorResponse.put("codigo", "PACIENTE_NAO_ENCONTRADO");
        errorResponse.put("error", ex.getMessage());
        errorResponse.put("dadosExtraidos", ex.getDadosExtraidos());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleSolicitacaoNaoEncontradaException(SolicitacaoNaoEncontradaException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TriagemJaPreenchidaException.class)
    public ResponseEntity<Map<String, Object>> handleTriagemJaPreenchidaException(TriagemJaPreenchidaException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OcsInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleOcsInvalidoException(OcsInvalidoException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ProcedimentoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleProcedimentoInvalidoException(ProcedimentoInvalidoException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TriagemNaoPreenchidaException.class)
    public ResponseEntity<Map<String, Object>> handleTriagemNaoPreenchidaException(TriagemNaoPreenchidaException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedOperationException(UnauthorizedOperationException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> erros = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                erros.put(error.getField(), error.getDefaultMessage())
        );

        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Erro de validação");
        errorResponse.put("campos", erros);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade ou constraint de dados: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflito de dados ou restrição de tamanho excedida");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado: " + ex.getResourcePath());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("Arquivo enviado excede o tamanho máximo permitido: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "O arquivo excede o limite de 5 MB.");
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPartException(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        log.warn("Parte obrigatória do upload ausente: {}", ex.getRequestPartName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Selecione um arquivo PDF.");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Corpo JSON da requisição inválido ou malformado: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
    public ResponseEntity<Map<String, Object>> handleServletRequestBindingException(org.springframework.web.bind.ServletRequestBindingException ex) {
        log.warn("Parâmetros de requisição inválidos ou ausentes: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Parâmetros de requisição inválidos ou ausentes: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Erro interno do servidor: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
