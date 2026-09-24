# Backend FUSEx

API REST do projeto FUSEx da equipe Eagle (FATEC SJC). O código Spring Boot fica no diretório `fusex/`.

## Pré-requisitos

- JDK 21 (`java -version`)
- Docker Desktop iniciado, com Docker Compose disponível (`docker --version` e `docker compose version`)
- Git

Se o PowerShell informar que `docker` não é reconhecido, instale/inicie o Docker Desktop e abra um novo terminal antes de continuar.

## Executar localmente

```powershell
git clone https://github.com/Eaglefatec/api03-backend.git
cd api03-backend\fusex
docker compose up -d
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, dentro de `api03-backend/fusex`, use `./mvnw spring-boot:run` (`chmod +x mvnw` se necessário).
O Compose cria o MySQL 8.4 em `localhost:3306` com banco `fusex_db` e credenciais locais `spring/spring`. O Spring Boot detecta o contêiner já iniciado e espera o banco estar pronto. Ao terminar: `docker compose down`. Os dados permanecem no volume `mysql_data`.

## Verificação

Com o MySQL ativo, execute `.\mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS. O teste de contexto verifica se a aplicação inicia com o banco. Também existe um workflow de CI que executa esse teste com MySQL no GitHub Actions.

## Banco de dados

O Hibernate usa `ddl-auto=validate`: ele verifica o esquema, sem criar tabelas automaticamente. As tabelas das histórias US01, US02 e US03 devem ser criadas em scripts versionados em `fusex/src/main/resources/db/migration/V1__nome.sql`, `V2__nome.sql` etc. O Flyway aplica os scripts ao iniciar a aplicação. Este incremento ainda não define tabelas de negócio; a primeira migração acompanha a modelagem da US01.

Se for usar um MySQL externo, configure `DB_URL`, `DB_USER` e `DB_PASSWORD` e desabilite o Compose automático com `SPRING_DOCKER_COMPOSE_ENABLED=false`.
