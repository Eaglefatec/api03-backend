package com.eagle.fusex.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {

    private final String publicBaseUrl;

    public QrCodeService(@Value("${fusex.public-base-url:http://localhost:8080/api}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.replaceAll("/+$", "") : "http://localhost:8080/api";
    }

    public String obterLinkPublicoBeneficiario(String token) {
        return publicBaseUrl + "/beneficiario.html?token=" + token;
    }

    public byte[] gerarQrCodePng(String conteudo, int largura, int altura) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(conteudo, BarcodeFormat.QR_CODE, largura, altura, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Erro ao gerar QR Code para o conteúdo informado", e);
        }
    }

    public byte[] gerarQrCodePngPorToken(String token) {
        return gerarQrCodePng(obterLinkPublicoBeneficiario(token), 300, 300);
    }

    public String gerarQrCodeBase64PorToken(String token) {
        byte[] bytes = gerarQrCodePngPorToken(token);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}

