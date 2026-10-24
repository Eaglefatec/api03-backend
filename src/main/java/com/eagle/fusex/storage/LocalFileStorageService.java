package com.eagle.fusex.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path uploadPath;

    public LocalFileStorageService(@Value("${fusex.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir);
        criarDiretorioSeNaoExistir();
    }

    private void criarDiretorioSeNaoExistir() {
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Diretório de uploads criado em: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Erro ao criar diretório de uploads: {}", uploadPath, e);
            throw new RuntimeException("Erro ao criar diretório de uploads", e);
        }
    }

    @Override
    public String salvar(MultipartFile arquivo, String nomeDestino) {
        try {
            Path destino = this.uploadPath.resolve(nomeDestino);
            try (InputStream in = arquivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return destino.toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo MultipartFile {}", nomeDestino, e);
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }

    @Override
    public String salvar(byte[] conteudo, String nomeDestino) {
        try {
            Path destino = this.uploadPath.resolve(nomeDestino);
            Files.write(destino, conteudo);
            return destino.toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo de bytes {}", nomeDestino, e);
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }
}
