package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.MedicoInfoDTO;
import com.eagle.fusex.model.dto.response.UsuarioLogadoResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.repository.MedicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MedicoRepository medicoRepository;

    public AuthController(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        MedicoInfoDTO medicoInfo = null;
        if (roles.contains("ROLE_MEDICO") || roles.contains("ROLE_ADMIN")) {
            Medico medico = medicoRepository.findByIdAndCredenciadoTrue(1L)
                    .or(() -> medicoRepository.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getCredenciado())).findFirst())
                    .orElse(null);

            if (medico != null) {
                medicoInfo = new MedicoInfoDTO(medico.getId(), medico.getNome(), medico.getCrm());
            }
        }

        return ResponseEntity.ok(new UsuarioLogadoResponse(username, roles, medicoInfo));
    }
}
