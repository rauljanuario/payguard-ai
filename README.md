# PayGuard AI

Auditor de APIs e webhooks que compara código-fonte com a documentação atual, identifica possíveis breaking changes e gera uma versão corrigida da documentação em Markdown.

O projeto usa o Google Gemini para a análise semântica, mascara dados sensíveis antes do envio e mantém o histórico das auditorias em um banco H2 persistente.

## Funcionalidades

- Comparação entre código-fonte e documentação de APIs ou webhooks.
- Detecção de mudanças em:
  - rotas e métodos HTTP;
  - campos de requisição e resposta;
  - tipos e campos obrigatórios;
  - códigos de status;
  - autenticação;
  - payloads de webhooks.
- Classificação da severidade como `LOW`, `MEDIUM` ou `HIGH`.
- Lista de divergências encontradas.
- Geração de documentação corrigida em Markdown.
- Mascaramento de tokens Bearer, segredos, chaves, cabeçalhos de autorização e CPFs antes da chamada ao Gemini.
- Histórico persistido das auditorias, ordenado da mais recente para a mais antiga.
- Aprovação ou descarte da sugestão gerada.
- Interface web estática para criar, consultar e revisar auditorias.

## Tecnologias

- Java 25
- Spring Boot 3.5.7
- Spring Web
- Spring Data JPA
- Spring Validation
- H2 Database
- Google Gemini API
- Maven
- HTML, CSS e JavaScript sem framework

## Pré-requisitos

- JDK 25 ou superior compatível com o projeto.
- Maven 3.9+ (ou Maven Wrapper, caso seja adicionado ao projeto).
- Uma chave de API do Google Gemini.

Confira as versões instaladas:

```bash
java -version
mvn -version
```

## Configuração

Defina a chave da API como variável de ambiente:

```bash
export GEMINI_API_KEY="sua-chave-do-gemini"
```

Como alternativa, crie um arquivo `.env` na raiz do projeto:

```dotenv
GEMINI_API_KEY=sua-chave-do-gemini
```

O `.env` é carregado opcionalmente pela aplicação. Não publique esse arquivo nem a chave no repositório.

As propriedades padrão estão em [`application.properties`](src/main/resources/application.properties):

| Propriedade | Padrão | Descrição |
|---|---|---|
| `gemini.api.url` | `https://generativelanguage.googleapis.com/v1beta/models` | URL base da API Gemini |
| `gemini.api.model` | `gemini-3.5-flash-lite` | Modelo usado na análise |
| `spring.datasource.url` | `jdbc:h2:file:./data/payguard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` | Banco H2 persistente em `data/` |
| `server.port` | `8080` | Porta padrão do Spring Boot |

Também é possível sobrescrever propriedades do Spring por variáveis de ambiente ou argumentos Maven/Spring.

## Executando a aplicação

Na raiz do projeto:

```bash
mvn spring-boot:run
```

Ou gere e execute o JAR:

```bash
mvn clean package
java -jar target/payguard-ai-0.0.1-SNAPSHOT.jar
```

Com a aplicação em execução:

- API: `http://127.0.0.1:8080/api/audits`
- Interface: `http://127.0.0.1:8080/`
- Console H2: `http://127.0.0.1:8080/h2-console`

### Acesso à interface web

A interface está em `src/main/resources/static`. Para usá-la com um servidor estático local, por exemplo o Live Server do VS Code, abra a página em `http://127.0.0.1:5500` ou `http://localhost:5500`. Essas duas origens estão autorizadas pelo CORS da API.

O arquivo [`app.js`](src/main/resources/static/app.js) usa `http://127.0.0.1:8080` como endereço fixo da API. Se a interface for hospedada em outra origem, atualize `API_BASE_URL` e a configuração de CORS do controlador.

## Fluxo de uma auditoria

1. O cliente envia o nome do módulo, o código-fonte e a documentação atual.
2. A aplicação valida os campos obrigatórios.
3. [`SensitiveDataMasker`](src/main/java/com/payguard/security/SensitiveDataMasker.java) mascara dados sensíveis.
4. [`GeminiClient`](src/main/java/com/payguard/client/GeminiClient.java) envia o conteúdo mascarado ao Gemini e solicita uma resposta JSON.
5. A resposta é convertida em um relatório com breaking change, severidade, divergências e documentação sugerida.
6. O relatório é salvo em `audit_reports` e suas divergências em `audit_discrepancies`.
7. O cliente pode aprovar ou descartar a sugestão.

O conteúdo salvo no relatório é o conteúdo já mascarado; a aplicação não persiste os valores originais recebidos.

## API

### Criar auditoria

`POST /api/audits`

Headers:

```http
Content-Type: application/json
```

Exemplo:

```bash
curl -X POST http://127.0.0.1:8080/api/audits \
  -H 'Content-Type: application/json' \
  -d '{
    "moduleName": "payments",
    "sourceCode": "@PostMapping(\"/payments\")\nrecord PaymentRequest(String amount) {}",
    "currentDocumentation": "# Payments API\n\nPOST /payments\n\nBody: `{ \"value\": 100 }`"
  }'
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "moduleName": "payments",
  "hasBreakingChange": true,
  "severity": "HIGH",
  "discrepancies": [
    "O campo documentado não corresponde ao campo usado no código"
  ],
  "suggestedDocMarkdown": "# Payments API\n...",
  "reviewStatus": null,
  "createdAt": "2026-11-19T21:30:00Z"
}
```

Os campos `sourceCode`, `currentDocumentation` e `moduleName` são obrigatórios e não podem ser vazios.

### Listar histórico

`GET /api/audits`

```bash
curl http://127.0.0.1:8080/api/audits
```

Retorna uma lista de `AuditResponse`, ordenada por `createdAt` decrescente.

### Revisar auditoria

`PATCH /api/audits/{id}/review`

Para aprovar:

```bash
curl -X PATCH http://127.0.0.1:8080/api/audits/1/review \
  -H 'Content-Type: application/json' \
  -d '{"reviewStatus":"APPROVED"}'
```

Para descartar:

```bash
curl -X PATCH http://127.0.0.1:8080/api/audits/1/review \
  -H 'Content-Type: application/json' \
  -d '{"reviewStatus":"REJECTED"}'
```

O status de revisão aceita apenas `APPROVED` ou `REJECTED`.

### Respostas de erro

Os erros retornam JSON no formato:

```json
{
  "timestamp": "2026-11-19T21:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/audits",
  "fieldErrors": {
    "moduleName": "must not be blank"
  }
}
```

Principais status:

| Status | Situação |
|---|---|
| `201` | Auditoria criada |
| `200` | Consulta ou revisão concluída |
| `400` | JSON inválido ou campos obrigatórios ausentes |
| `404` | Auditoria não encontrada |
| `502` | Falha na configuração, comunicação ou resposta do Gemini |
| `500` | Erro inesperado no servidor |

## Mascaramento de dados sensíveis

Antes da análise, o projeto substitui:

- tokens no formato `Bearer ...`;
- valores associados a `password`, `passwd`, `pwd`, `token`, `secret`, `api_key`, `access_key` e `client_secret`;
- valores de `Authorization`;
- CPFs com ou sem pontuação.

Exemplos:

```text
Authorization: Bearer abc123
api_key=abc123
cpf=123.456.789-09
```

Transformam-se em valores contendo `[REDACTED]` ou `[CPF_REDACTED]`. O mascaramento é baseado em expressões regulares; dados sensíveis em formatos não reconhecidos ainda devem ser removidos pelo cliente antes do envio.

## Banco de dados

O H2 é configurado no modo arquivo e cria os arquivos em `data/`:

- `data/payguard.mv.db`
- `data/payguard.trace.db` (quando o trace estiver habilitado)

O schema é atualizado automaticamente por `spring.jpa.hibernate.ddl-auto=update`. Para inspecionar o banco pelo console H2, use:

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/payguard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` |
| User Name | `sa` |
| Password | vazio |

## Testes

Execute os testes unitários com:

```bash
mvn test
```

Os testes atuais cobrem o serviço de auditoria, incluindo mascaramento antes da análise, persistência, listagem, aprovação, rejeição e auditoria inexistente.

## Estrutura do projeto

```text
.
├── pom.xml
├── data/                         # Banco H2 local persistente
├── src/
│   ├── main/
│   │   ├── java/com/payguard/
│   │   │   ├── client/           # Cliente HTTP do Gemini
│   │   │   ├── controller/       # Endpoints REST
│   │   │   ├── dto/              # Contratos de entrada e saída
│   │   │   ├── exception/        # Exceções e respostas de erro
│   │   │   ├── security/         # Mascaramento de dados
│   │   │   ├── model/            # Entidades e enums
│   │   │   ├── repository/       # Repositórios JPA
│   │   │   └── service/           # Regras de negócio
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/            # Interface web
│   └── test/                      # Testes automatizados
└── target/                       # Artefatos gerados pelo Maven
```

## Limitações e considerações

- A qualidade da detecção depende da resposta estruturada gerada pelo Gemini.
- Cada auditoria exige uma chamada externa ao Gemini; latência, limites e custos da API se aplicam.
- A aplicação exige `GEMINI_API_KEY` na inicialização; sem ela, o contexto não sobe.
- O modelo padrão e a URL do Gemini podem ser alterados pela configuração.
- O H2 em arquivo é adequado para desenvolvimento e uso local. Para produção, recomenda-se um banco gerenciado e configuração de credenciais fora do código.
- Não há autenticação ou autorização nos endpoints REST.
- O CORS está limitado às origens locais `127.0.0.1:5500` e `localhost:5500`.

## Licença

Este projeto está disponível sob a licença MIT. Consulte o arquivo [`LICENSE`](LICENSE) para o texto completo.
