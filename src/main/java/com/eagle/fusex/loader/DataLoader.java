package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final OcsRepository ocsRepository;

    public DataLoader(MedicoRepository medicoRepository, PacienteRepository pacienteRepository,
                      OcsRepository ocsRepository) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.ocsRepository = ocsRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (medicoRepository.count() == 0) {
            Medico medico1 = new Medico("Dr. João Silva", "123456", true);
            Medico medico2 = new Medico("Dra. Maria Santos", "654321", false);

            medicoRepository.save(medico1);
            medicoRepository.save(medico2);

            log.info("Médicos de teste inseridos com sucesso!");
        }

        if (pacienteRepository.count() == 0) {
            Paciente paciente = new Paciente("Sargento Carlos Silva", "11122233344", "DCT", 38, "(11) 98765-4321");
            pacienteRepository.save(paciente);
            log.info("Paciente padrão '{}' inserido com sucesso!", paciente.getNome());
        }

        if (ocsRepository.findByLoginUsuario("ocs").isEmpty()) {
            ocsRepository.findFirstByOrderByOcsIdAsc().ifPresent(ocs -> {
                ocs.setLoginUsuario("ocs");
                ocsRepository.save(ocs);
                log.info("Usuário 'ocs' vinculado à OCS '{}'", ocs.getOcsNome());
            });
        }
    }
}
