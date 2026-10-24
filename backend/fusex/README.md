# FUSEX — Backend (Sprint 2)

API REST desenvolvida em Java 21 com Spring Boot 3.4.13. Este projeto contém a lógica de negócio, segurança, persistência, migrações Flyway e testes; os arquivos de interface foram separados para o frontend.

## Requisitos

- JDK 21 para execução local
- Maven Wrapper incluído
- Docker e Docker Compose para iniciar o MySQL

## Iniciar o banco de dados

A partir desta pasta (`backend/fusex`):

```bash
docker compose up -d
```

O Compose existente nesta pasta inicia o MySQL. Ele não inicia automaticamente a aplicação Spring Boot.

## Testar e executar localmente

Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw test
./mvnw spring-boot:run
```

A API escuta na porta 8080 e usa o contexto `/api`. A configuração local espera o MySQL em `localhost:3306`.

## Executar com a stack completa

Na raiz da branch de integração, use o `docker-compose.yml` da raiz:

```bash
docker compose up --build
```

Essa configuração constrói o backend e o frontend em contêineres e inicia o MySQL. Para parar os serviços, use `docker compose down`. Os dados do banco permanecem no volume nomeado, salvo se ele for removido explicitamente.

## Configuração e segurança

As credenciais do Compose são apenas para desenvolvimento acadêmico/local. Não reutilize essas credenciais em produção. Antes de implantação, revisar também usuários e senhas de demonstração, CORS, autenticação e configuração de banco por ambiente.

## Testes

Execute `mvnw test` antes de integrar alterações. A criação desta branch não substitui a execução real dos testes nem a validação dos fluxos no navegador.
