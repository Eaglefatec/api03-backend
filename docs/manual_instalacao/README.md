# Manual de Instalação — API03 (FusEx)

Este manual descreve como executar a aplicação **FusEx** (backend) localmente a partir do repositório oficial.

**Repositório:** [https://github.com/Eaglefatec/api03](https://github.com/Eaglefatec/api03)

---

## Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Obtendo o código](#2-obtendo-o-código)
3. [Subindo os serviços com Docker](#3-subindo-os-serviços-com-docker)
4. [Executando a aplicação](#4-executando-a-aplicação)
5. [Encerrando a aplicação](#5-encerrando-a-aplicação)
6. [Solução de problemas](#6-solução-de-problemas)

---

## 1. Pré-requisitos

Antes de começar, o usuário precisa ter instalado:

| Ferramenta | Obrigatório | Observação |
|------------|:-----------:|------------|
| **Docker** (com Docker Compose) | Sim | [Instruções de instalação](https://docs.docker.com/get-docker/) |
| **Git** | Sim | Para clonar o repositório |
| **Java (JDK)** | Sim | Necessário para executar o Spring Boot (versão conforme o `../../backend/fusex/pom.xml` do projeto) |
| **Maven** | Somente no Linux | No Windows, o projeto usa o Maven Wrapper (`mvnw.cmd`) |

Para confirmar que o Docker está instalado e funcionando:

```bash
docker --version
docker compose version
```

> **Importante:** o Docker deve estar **em execução** antes de prosseguir. No Windows, abra o Docker Desktop e aguarde ele iniciar completamente.

---

## 2. Obtendo o código

Clone o repositório e acesse a pasta do projeto:

```bash
git clone https://github.com/Eaglefatec/api03.git
cd api03
```

> **Atenção:** todos os comandos a seguir devem ser executados **dentro do repositório clonado**.

---

## 3. Subindo os serviços com Docker

Acesse o diretório do backend:

```bash
cd backend/fusex
```

Em seguida, derrube quaisquer containers anteriores (garante um ambiente limpo) e suba os serviços:

```bash
docker compose down
docker compose up -d --wait
```

**O que esses comandos fazem:**

| Comando | Descrição |
|---------|-----------|
| `docker compose down` | Encerra e remove containers de execuções anteriores |
| `docker compose up -d` | Sobe os serviços em segundo plano (modo *detached*) |
| `--wait` | Aguarda os serviços ficarem saudáveis (*healthy*) antes de devolver o terminal |

Para verificar se os containers estão rodando:

```bash
docker compose ps
```

---

## 4. Executando a aplicação

Ainda dentro de `../../backend/fusex`, execute o comando correspondente ao seu sistema operacional, utilizando o perfil `local`.

### Windows

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Linux

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Quando a aplicação iniciar com sucesso, o terminal exibirá logs do Spring Boot indicando que o servidor está em execução (por exemplo, `Started ... in X seconds`).

---

## 5. Encerrando a aplicação

1. **Parar a aplicação Spring Boot:** pressione `Ctrl + C` no terminal onde ela está rodando.
2. **Parar os serviços Docker:**

```bash
cd backend/fusex
docker compose down
```

---

## 6. Solução de problemas

| Problema | Possível causa | Solução |
|----------|----------------|---------|
| `Cannot connect to the Docker daemon` | Docker não está em execução | Inicie o Docker Desktop (Windows) ou o serviço do Docker (Linux: `sudo systemctl start docker`) |
| `docker compose up` falha ou fica travado no `--wait` | Serviço não ficou saudável | Verifique os logs com `docker compose logs` |
| Porta já em uso | Outro processo ocupa a porta necessária | Encerre o processo conflitante ou execute `docker compose down` e tente novamente |
| `mvn: command not found` (Linux) | Maven não instalado | Instale o Maven (ex.: `sudo apt install maven`) |
| `JAVA_HOME` não definido / versão incorreta do Java | JDK ausente ou incompatível | Instale o JDK exigido pelo projeto e configure a variável `JAVA_HOME` |
| Erro de conexão com o banco de dados ao iniciar | Containers não estão de pé | Rode `docker compose ps` e refaça a etapa 3 |

---

## Resumo rápido

```bash
git clone https://github.com/Eaglefatec/api03.git
cd api03/backend/fusex

docker compose down
docker compose up -d --wait

# Windows
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Linux
mvn spring-boot:run -Dspring-boot.run.profiles=local
```