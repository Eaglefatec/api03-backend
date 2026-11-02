package com.eagle.fusex.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService("http://localhost:8080/api");

    @Test
    @DisplayName("Deve gerar PNG de QR Code válido a partir de token")
    void deveGerarQrCodePngValido() {
        byte[] bytes = qrCodeService.gerarQrCodePngPorToken("token-teste-123");

        assertNotNull(bytes);
        assertTrue(bytes.length > 50);

        // Validar magic bytes do formato PNG: \x89 P N G
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals((byte) 'P', bytes[1]);
        assertEquals((byte) 'N', bytes[2]);
        assertEquals((byte) 'G', bytes[3]);
    }

    @Test
    @DisplayName("Deve montar link público do beneficiário corretamente")
    void deveMontarLinkPublicoBeneficiario() {
        String link = qrCodeService.obterLinkPublicoBeneficiario("token-abc");
        assertEquals("http://localhost:8080/api/beneficiario.html?token=token-abc", link);
    }
}

