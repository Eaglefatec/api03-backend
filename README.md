# FUSEX — Backend

API REST do projeto FUSEX, desenvolvida em Java 21 com Spring Boot 3.4.13.

Este repositório mantém o projeto Maven na raiz. A estrutura interna do projeto Spring Boot foi preservada: `pom.xml`, `src/`, `.mvn/`, `mvnw`, `mvnw.cmd`, `scripts/` e `docker-compose.yml`.

## Requisitos

- JDK 21
- Docker Desktop com Docker Compose
- Windows PowerShell, Linux ou macOS

## Executar localmente (mesmo fluxo do projeto original)

**1. Inicie o MySQL pelo Docker.** Na raiz deste repositório:

```powershell
docker compose up -d
```

Esse Compose inicia somente o banco de dados MySQL, como no projeto original.

**2. Execute os testes.** Em outro terminal, na raiz deste repositório:

```powershell
.\mvnw.cmd test
```

**3. Inicie a API Spring Boot:**

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, use `./mvnw test` e `./mvnw spring-boot:run`.

A API usa a porta `8080` e o contexto `/api`. O perfil `local` conecta ao MySQL em `localhost:3306`. O frontend está em um repositório separado e deve ser servido separadamente; a API continua sendo executada pelo Spring Boot.

## Importação de dados

Os arquivos-fonte utilizados pelos scripts estão em `contexto/`. Os scripts ficam em `scripts/` e geram os CSVs em `src/main/resources/data/`.

Para gerar os CSVs, instale as dependências Python necessárias para leitura de planilhas e execute os scripts conforme a necessidade do ambiente. A aplicação avisa no log quando um CSV opcional não foi encontrado.

## Estrutura

- `src/main/java/`: código Java da aplicação.
- `src/main/resources/`: configurações, migrações Flyway e dados.
- `src/test/`: testes automatizados.
- `scripts/`: utilitários de preparação/importação de dados.
- `contexto/`: arquivos de entrada usados pelos scripts.
- `docs/`: documentação do projeto.
- `modelos-importacao/`: modelos auxiliares.

## Observações

- Este repositório contém o backend; as páginas HTML/CSS/JavaScript estão em [api03-frontend](https://github.com/Eaglefatec/api03-frontend).
- As credenciais do Docker Compose são destinadas somente ao desenvolvimento local.
- A estrutura foi ajustada para que o diretório raiz deste repositório seja a raiz do projeto Maven. Não houve intenção de reorganizar os pacotes Java ou alterar a lógica de negócio.
