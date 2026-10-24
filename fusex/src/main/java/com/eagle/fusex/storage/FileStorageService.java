package com.eagle.fusex.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String salvar(MultipartFile arquivo, String nomeDestino);
    String salvar(byte[] conteudo, String nomeDestino);
}
