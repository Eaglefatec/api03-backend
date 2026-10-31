package com.eagle.fusex.model.dto.response;

import java.util.List;

public record TabelaArquivoResponse(String nome, List<List<String>> linhas) {
}
