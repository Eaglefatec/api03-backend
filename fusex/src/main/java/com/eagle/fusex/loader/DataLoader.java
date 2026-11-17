package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Medico;
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

        garantirPaciente("Sargento Carlos Silva", "11122233344", "11122233344", "DCT", "paciente@fusex.eb.mil.br", 38, "(11) 98765-4321");
        garantirPaciente("WALDIR DE CARVALHO MESSIAS", "12300922200", "12300922200", "FUSEX", "waldir.messias@fusex.eb.mil.br", 45, "(12) 98888-1111");
        garantirPaciente("ANDRÉ LUÍS QUIRINO DE ALMEIDA", "12435976600", "12435976600", "FUSEX", "andre.almeida@fusex.eb.mil.br", 48, "(12) 98888-2222");
        garantirPaciente("ROSANA TICIANELLI PINTO", "98223010000", "98223010000", "FUSEX", "rosana.pinto@fusex.eb.mil.br", 62, "(12) 98888-3333");

        var ocsExistente = ocsRepository.findByOcsInscricaoFederal("07.185.057/0001-51")
                .or(() -> ocsRepository.findByOcsNome("CENTRO DIAGNÓSTICO ANDRADE LTDA"));

        if (ocsExistente.isEmpty()) {
            com.eagle.fusex.model.entity.Ocs ocsAndrade = new com.eagle.fusex.model.entity.Ocs();
            ocsAndrade.setOcsNome("CENTRO DIAGNÓSTICO ANDRADE LTDA");
            ocsAndrade.setOcsInscricaoFederal("07.185.057/0001-51");
            ocsRepository.save(ocsAndrade);
        } else {
            com.eagle.fusex.model.entity.Ocs ocs = ocsExistente.get();
            if (!"CENTRO DIAGNÓSTICO ANDRADE LTDA".equals(ocs.getOcsNome())) {
                ocs.setOcsNome("CENTRO DIAGNÓSTICO ANDRADE LTDA");
                ocsRepository.save(ocs);
            }
        }

        if (ocsRepository.findByLoginUsuario("ocs").isEmpty()) {
            ocsRepository.findFirstByOrderByOcsIdAsc().ifPresent(ocs -> {
                ocs.setLoginUsuario("ocs");
                ocsRepository.save(ocs);
                log.info("Usuário 'ocs' vinculado à OCS '{}'", ocs.getOcsNome());
            });
        }
    }

    private void garantirPaciente(String nome, String cpf, String prec, String om, String email, Integer idade, String telefone) {
        if (pacienteRepository.findByCpfPrec(prec).isEmpty() && pacienteRepository.findByCpfPrec(cpf).isEmpty()) {
            Paciente p = new Paciente(nome, cpf, prec, om, email, idade, telefone);
            pacienteRepository.save(p);
            log.info("Paciente '{}' (Prec {}) cadastrado com sucesso!", nome, prec);
        }
    }
}
