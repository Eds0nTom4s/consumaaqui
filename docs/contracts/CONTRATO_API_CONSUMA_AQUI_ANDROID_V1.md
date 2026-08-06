# CONTRATO API CONSUMA AQUI ANDROID V1

## 1. Identificação, versão e estado

| Campo | Valor |
|---|---|
| Documento | Contrato técnico Android ↔ Backend CONSUMA AQUI |
| Versão contratual | `1.0.0-rc.2` |
| Versão de entrada | `1.0.0-rc.1` |
| Data | 2026-08-06 |
| API base | `/api/v1` |
| Público-alvo | Equipas backend, Android, QA, segurança e operações |
| Estado | Congelado com blockers de implementação explicitamente classificados |
| Veredicto | **CONTRATO ANDROID V1 CONGELADO COM BLOCKERS** |

Este é o documento canónico da integração. Em conflito com mocks, endpoints legados ou enums internos, este contrato prevalece depois de aprovado. Não é evidência de que os endpoints aqui definidos já existam.

## 2. Repositórios, referências e método factual

### 2.1 Android

- Repositório: `Eds0nTom4s/consumaaqui`.
- Branch requerida: `android/checkout-flow-mock-001`.
- SHA local e remoto confirmado após `git fetch --all --prune`: `b8b6fffd048558e0c1cd688f5addac7d5fac18bb`.
- `origin/android/checkout-flow-mock-001` e a branch local requerida apontam ao mesmo SHA. O worktree estava na branch local `android/discovery-remote-001`, também no mesmo SHA; nenhuma troca ou alteração de branch foi feita.
- O histórico é cumulativo: foundation → navigation → onboarding/location → home/discovery → merchant discovery → catalog/product → cart → checkout. Os commits funcionais imediatamente relevantes são `ee69dd3`, `bfef5aa`, `b1c386b` e `5b73735`.
- Não havia alterações tracked. Foram preservados e excluídos da evidência os ficheiros não rastreados `nova sequencia` e `visao sobre mobilidade de cargas e transporte de consumidores consuma`.

### 2.2 Backend

O repositório não existia no workspace e foi clonado apenas para leitura num directório temporário. Foi executado `git fetch --all --prune`. Não houve merge nem base artificial.

| Referência | SHA | Capacidades efectivamente analisadas | Relação/limitação |
|---|---|---|---|
| `backend/discovery-api-001` | `b979d2ead1d6663af5955c55d54bdae6861832bc` | Discovery público, publicação, DTOs, paginação, segurança e 11 testes dedicados | Branch separada, não integrada em `main` |
| `main` | `124bb92fff9e599a102b6f174390e90ec1069c09` | Contrato PONTO MVP V1 integrado, catálogo tenant-aware, QR público, pedidos/subpedidos, aceite/rejeição, pagamento após aceite, turnos, fulfillment, Store e idempotência | Baseline obrigatória desta auditoria; schema `V20260722.01`; não contém Discovery |
| `backend/pdv-invoice-delivery-001` | `694054244582d1f8edd4579c4afd8af91478eac4` | Alterações posteriores de PDV, idempotência de PDV/confirmação e entrega fiscal | Descendente de `main`, 2026-08-04; não implementa o contrato público Android |
| `backend/public-order-state-machine-001` | `0284bd13501133852f8711ec25ce227cb27e8a39` | Comparação histórica da máquina pública | Linha divergente; conceitos equivalentes/posteriores foram confrontados com `main` |
| `backend/ponto-order-acceptance-payment-001[-a]` | `f7f5764b6cb835fabb7ecab0656261e9362a5eef` / `fd437d85ececbab8de12f77540d1bdce59985616` | Aceite e pagamento manual após aceite | Ancestrais da linha operacional integrada |
| `cardapio-limits-publish-001` | `bb4d915edca6a66f5ffb56c56c7d7073202d63f8` | Publicação e limites de cardápio | Ancestral de `main` |
| `backend/consuma-appypay-idempotencia-reconciliacao-001` | `364dd448133e9935b7e3393ca64d2022c8b4444b` | Fingerprints e reconciliação AppyPay | Não é idempotência de criação do pedido Android |

Conclusão factual: nenhuma branch contém simultaneamente Discovery e a linha operacional mais recente. Este documento é uma análise comparativa, não uma afirmação de build conjunto.

## 3. Estado factual encontrado

### 3.1 Android

O Android possui contratos de domínio substituíveis, ligados por Hilt a singletons in-memory: `DiscoveryRepository`, `CatalogRepository`, `CartRepository` e `CheckoutRepository`. A navegação real é Home/Search → Merchant Overview → Catalog → Product Detail → Cart → Checkout → Confirmation, transportando IDs. Existem 46 ficheiros de testes JVM e 24 instrumentados.

Factos relevantes:

- `MoneyAmount` usa `Long amountMinor`, moeda `[A-Z]{3}`, operações exactas e proíbe negativos.
- Discovery modela disponibilidade, fulfillment, rating/distância opcionais, popularidade, destaque, `catalogAvailable`; pesquisa local usa página 1, OR entre fulfillment e desempates determinísticos.
- A localização de Discovery contém id/cidade/área/displayName, mas não coordenadas. O endereço de Checkout admite latitude/longitude opcionais em par.
- Catálogo contém versão, moeda, categorias, produtos, disponibilidade, imagens, preço comparativo, tags, grupos/opções, regras min/max/default e ordenação.
- Carrinho é process-local, um merchant por vez, quantidade 1..99, nota até 250 e subtotal apenas estimado. Nomes e preços são snapshots visuais, não autoridade.
- Checkout mock recolhe cliente, pickup/delivery, calcula quote de 10 minutos e cria `CheckoutDraft`, não pedido. `DELIVERY_MOCK` não é enum público aceitável.
- `CheckoutViewModel` grava nome, telefone, email e todo o endereço em `SavedStateHandle`; isto pode sobreviver à recriação do processo e requer correcção Android futura.

### 3.2 Backend

- Discovery branch expõe `/api/v1/discovery/home`, `/search` e **singular** `/merchant/{merchantId}`; página 0; `NAME` é o único sort real; não suporta `onlyOpen` nem filtro de fulfillment; coordenadas são validadas mas não usadas; `nearby` e `featured` são vazios; `Cache-Control: no-store`; merchantId é slug público ainda sem imutabilidade formal.
- `Produto`/`CategoriaProduto` são tenant-aware, usam IDs `Long`; preço é `BigDecimal(10,2)`; produto tem uma descrição, imagem principal, preparação e flags. `ProdutoImagem` suporta galeria ordenada (limite de serviço: 4).
- `VariacaoProduto` representa uma variação simples única (tipo/valor/preço/stock). Não representa múltiplos grupos simultâneos, min/max, default, ordenação por grupo/opção ou adicionais combináveis.
- `TenantCardapioConfig` controla publicação, actualização, banner e máximo de itens, mas não possui versão monotónica de catálogo.
- `Pedido` e `SubPedido` são tenant-aware; IDs e respostas existentes são numéricos. `Pedido` tem `CRIADO/EM_ANDAMENTO/FINALIZADO/CANCELADO` e financeiro `NAO_PAGO/PENDENTE_PAGAMENTO/PAGO/ESTORNADO`.
- `PedidoStatusTransitionService.aceitarPedido` leva subpedidos de `CRIADO` a `PENDENTE`, deriva pedido `EM_ANDAMENTO`, preserva o financeiro e só então chama `OrdemPagamentoService.garantirOrdemPagamentoPedidoAposAceite`.
- `PedidoPagamentoPolicy` impede início/confirmação antes do aceite conforme origem/template. `PedidoAllowedActionsService` também bloqueia pagamento antes do aceite.
- Rejeição é hoje armazenada como `CANCELADO` com motivo; não existe estado/causa estruturada que preserve a distinção pública.
- Criação QR possui idempotência com SHA-256, estado PROCESSING/COMPLETED/FAILED e constraint de corrida, mas é scoped a QR/tenant, recebe produto numérico, depende de QR/mesa/sessão e não suporta quote ou opções.
- Store inicia pagamento durante checkout, usa identidade de sócio/telefone, IDs numéricos e rastreio público por número+telefone. É incompatível com esta API.

## 4. Objectivo, escopo incluído e excluído

Inclui Discovery, detalhe público, catálogo/produto, carrinho local como origem de intenção, cotação autoritativa, pickup/delivery, criação idempotente, confirmação inicial e consulta segura do pedido.

Exclui autenticação de consumidor, carrinho backend, pagamento/instruções de pagamento, cancelamento pelo consumidor, tracking logístico, avaliações, escrita de catálogo, operação do tenant e implementação Android/backend. O endpoint de consulta prepara a fase seguinte sem disponibilizar um comando de pagamento.

## 5. Decisões arquitecturais auditáveis

| Decisão | Justificação | Evidência Android | Evidência backend | Impacto/risco | Acção |
|---|---|---|---|---|---|
| Carrinho permanece local | Não há necessidade de sincronização multi-dispositivo nesta fase | `InMemoryCartRepository`, merchant único, subtotal estimado | Não há carrinho público apropriado; Store é outro domínio | Perda do carrinho ao matar processo; aceitável no MVP | Android poderá persistir localmente depois; não criar tabela de cart |
| Quote obrigatória antes do pedido | Dispositivo não é autoridade financeira | preços locais explicitamente estimados; `CheckoutQuote` mock | QR recalcula apenas preço simples e não congela snapshot | Sem quote haveria adulteração e corrida de catálogo | Implementar quote antes de orders |
| IDs públicos opacos | IDs Long existentes são enumeráveis e vazam estrutura | navegação aceita String | endpoints actuais expõem Long; Discovery usa slug mutável | migrations e dual lookup | adicionar `public_id` UUID, nunca expor PK |
| Página pública começa em 0 | Alinha branch Discovery e convenção Spring | Android começa em 1 | Discovery usa 0 | exige adapter Android, não mudança de UI | transporte converte domínio N ↔ HTTP N-1 |
| Só anunciar sorts com fonte real | Evita resultados falsos | UI oferece 5 sorts | backend só possui NAME | UI remota precisa capability/erro | v1 obrigatório: NAME; demais só após persistência |
| Rejeição é distinta de cancelamento | Semântica, auditoria e UI diferentes | confirmação futura precisa motivo/estado | rejeição vira CANCELADO | impossível reconstruir confiavelmente | persistir termination_type/REJECTED |
| Origem `CONSUMA_AQUI_ANDROID` | QR e Store têm semântica incorrecta | fluxo começa em Discovery, sem QR | `PedidoOrigem` não contém Android | policies precisam reconhecer origem | adicionar enum interno e migration |
| Token opaco para consulta | telefone/número não são credenciais | consumidor ainda anónimo | Store usa telefone+número; QR usa token QR da mesa | takeover/enumeração se reutilizado | novo token 256-bit, hash e scope de pedido |
| Pagamento só após aceite | Regra comercial expressa | checkout termina em draft/confirmação | policy e service já implementam ordem após aceite | regressão financeira crítica | projectar NOT_PAYABLE → UNPAID; nenhum detalhe de pagamento nesta fase |

## 6. Responsabilidades

### 6.1 Android

- Manter carrinho, `clientItemId`, `clientReference`, `X-Client-Instance-Id` e `Idempotency-Key` localmente.
- Enviar apenas produto, quantidade, IDs de opções, nota e fulfillment; tratar preços locais como estimativa.
- Converter paginação 1-based do domínio actual para HTTP 0-based.
- Recalcular/apresentar exclusivamente a resposta da quote na revisão; exigir confirmação nova após divergência.
- Guardar `orderAccessToken` em armazenamento cifrado/Keystore-backed, nunca em analytics, crash report, URI ou `SavedStateHandle`.
- Futuro hardening Android: retirar PII do `SavedStateHandle`, definir política de screenshot/autofill/clipboard e limpar estado ao abandonar checkout.

### 6.2 Backend

- Resolver tenant apenas pelo `merchantId`; ignorar/rejeitar qualquer tenant fornecido pelo cliente.
- Ser autoridade de publicação, catálogo, configurações, moeda, preços, disponibilidade, fulfillment, quote, pedido e estados.
- Aplicar isolamento tenant em query, FK/constraints e serviços; produzir snapshots imutáveis.
- Não criar ordem de pagamento, payment intent ou instruções na criação do pedido.
- Proteger PII, tokens, logs, rate limits, idempotência, auditoria e anti-enumeração.

## 7. Fluxo normativo

```text
GET discovery/home ou discovery/search
  → GET discovery/merchants/{merchantId}
  → GET merchants/{merchantId}/catalog
  → GET merchants/{merchantId}/products[/{productId}]
  → carrinho local
  → POST checkout/quotes
  → revisão autoritativa
  → confirmação do utilizador
  → POST orders (quoteId + Idempotency-Key)
  → PENDING_ACCEPTANCE / NOT_PAYABLE / paymentAvailable=false
  → GET orders/{orderId} com token opaco
```

Aceite é comando interno do estabelecimento, fora desta API. Só após aceite a projecção pode ser `ACCEPTED / UNPAID / paymentAvailable=true`. “Disponível” não significa que esta v1 implemente início de pagamento.

## 8. Convenções HTTP comuns

- HTTPS obrigatório; JSON UTF-8; datas RFC 3339 UTC; IDs lowercase canonical UUID; enums uppercase.
- Headers de request: `Accept: application/json`; `X-Request-Id` opcional UUID; `X-Client-Instance-Id` obrigatório nos POST (UUID gerado por instalação, não é autenticação); `Content-Type` nos POST. `Idempotency-Key` obrigatório apenas em `POST /orders`.
- Headers de response: `X-Request-Id`, `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`; `Retry-After` em 429/503/202 de processamento.
- Parâmetros desconhecidos ou repetidos são `400 INVALID_REQUEST`, excepto `fulfillmentOptions`, cujo parâmetro pode repetir e é convertido num set.
- Strings: trim, Unicode NFC, control characters removidos/rejeitados; vazia opcional vira `null`; IDs e enums não são corrigidos silenciosamente.
- POST máximo 64 KiB; máximo 50 linhas distintas e 99 unidades por linha; nota de item 250; observação/instrução 300; nomes 120; email 254; telefone normalizado E.164 (9–15 dígitos com `+`), validação server-side.
- GETs públicos não requerem autenticação. Quote/order creation são anónimos com contexto de instalação e mitigação de abuso. GET order requer token do próprio pedido.
- Todos os erros usam a secção 25. `traceId` é opaco e não contém PII.

## 9. Inventário normativo

| Método e path | Finalidade | Auth | Cache | Limite inicial |
|---|---|---|---|---|
| `GET /discovery/home` | secções de descoberta | pública | `public,max-age=60,stale-while-revalidate=300` + ETag | 120/min/IP |
| `GET /discovery/search` | pesquisa paginada | pública | `public,max-age=30` + ETag | 120/min/IP |
| `GET /discovery/merchants/{merchantId}` | detalhe | pública | `public,max-age=60` + ETag | 180/min/IP |
| `GET /merchants/{merchantId}/catalog` | metadata/categorias | pública | `public,max-age=60` + ETag | 180/min/IP |
| `GET /merchants/{merchantId}/products` | produtos paginados | pública | `public,max-age=60` + ETag | 180/min/IP |
| `GET /merchants/{merchantId}/products/{productId}` | detalhe/configuração | pública | `public,max-age=60` + ETag | 180/min/IP |
| `POST /checkout/quotes` | quote autoritativa | anónima | `no-store` | 30/min/instalação e 60/IP |
| `POST /orders` | consumir quote/criar pedido | anónima + idem | `no-store` | 10/min/instalação e 20/IP |
| `GET /orders/{orderId}` | consulta inicial/estado | token | `private,no-cache` + ETag | 60/min/token e 120/IP |

Limites são configuração operacional, não promessa de capacidade. Exceder retorna 429 `RATE_LIMITED` sem revelar existência de recurso.

## 10. Modelos JSON canónicos

```json
{
  "amountMinor": 250000,
  "currencyCode": "AOA"
}
```

```json
{
  "page": 0,
  "pageSize": 20,
  "totalCount": 37,
  "hasMore": true,
  "items": []
}
```

Merchant resumido: `merchantId`, `name`, `category{id,name}`, `shortDescription?`, `imageUrl?`, `availability{status,closingInMinutes?,opensAt?}`, `fulfillmentOptions[]`, `distanceMeters?`, `estimatedPreparationMinutes?`, `rating{value,count}?`, `popularityScore?`, `minimumOrderAmount?`, `promotion?`, `featured`, `catalogAvailable`. Nulls opcionais podem ser omitidos; arrays obrigatórios nunca são null.

Produto: `productId`, `merchantId`, `categoryId`, `name`, `shortDescription?`, `fullDescription?`, `primaryImageUrl?`, `gallery[]`, `basePrice`, `compareAtPrice?`, `availability`, `preparationMinutes?`, `tags[]`, `optionGroups[]`, `featured`, `sortOrder`, `productVersion`.

### 10.1 Enums públicos fechados para escrita

Requests só aceitam os valores listados; valor desconhecido é `INVALID_REQUEST`. Responses Android devem conservar fallback seguro para expansão futura.

| Enum | Valores v1 |
|---|---|
| `DiscoverySort` | `NAME`; `FEATURED`, `NEAREST`, `TOP_RATED`, `MOST_POPULAR` apenas quando a capability persistente estiver activa |
| `MerchantAvailabilityStatus` | `OPEN`, `CLOSING_SOON`, `OPENS_AT`, `CLOSED`, `UNKNOWN` |
| `DiscoveryFulfillmentOption` | `PICKUP`, `DELIVERY`, `DINE_IN`, `SERVICE` |
| `CheckoutFulfillmentMethod` | `PICKUP`, `DELIVERY` |
| `ProductAvailability` | `AVAILABLE`, `UNAVAILABLE`, `TEMPORARILY_UNAVAILABLE`, `AVAILABLE_FROM`, `OUT_OF_STOCK`, `UNKNOWN` |
| `ChargeType` | `DELIVERY`, `SERVICE`, `DISCOUNT` (amount de desconto continua não negativo e é subtraído segundo `operation`) |
| `ChargeOperation` | `ADD`, `SUBTRACT` |
| `QuoteDivergenceType` | `CATALOG_CHANGED`, `PRICE_CHANGED`, `PRODUCT_CHANGED`, `OPTION_CHANGED`, `FULFILLMENT_CHANGED` |
| `AcceptancePolicy` | `MERCHANT_MUST_ACCEPT` |
| `PaymentPolicy` | `AVAILABLE_AFTER_ACCEPTANCE` |
| `OperationalStatus` | `PENDING_ACCEPTANCE`, `ACCEPTED`, `PREPARING`, `READY`, `COMPLETED`, `REJECTED`, `CANCELLED` |
| `FinancialStatus` | `NOT_PAYABLE`, `UNPAID`, `PAYMENT_PENDING`, `PAID`, `REFUNDED` |

### 10.2 Nullability e validação de campos transversais

| Campo | Obrigatório | Regra |
|---|---|---|
| IDs server | sim quando o recurso existe | UUID canonical; 36 chars; case-sensitive após normalização para lowercase |
| `currencyCode` | sim em todo Money | exactamente 3 ASCII uppercase; v1 comercial esperada `AOA` |
| `amountMinor` | sim | integer JSON, 0..`Long.MAX_VALUE`; charges/discounts usam operation, não valor negativo |
| `name` público | sim | NFC/trim, 1..120 (produto/merchant pode usar limite persistente mais restrito) |
| descriptions | não | null/omitido; short ≤280, full ≤5000 |
| URL de imagem | não | HTTPS absoluta, ≤2048, host autorizado |
| arrays | sim | nunca null; vazio permitido salvo items de quote/order |
| `rating` | não | value decimal 0..5 e count integer ≥0; ausente não vira zero |
| `distanceMeters` | não | integer ≥0; só com coordenadas e cálculo real |
| minutos | não | integer 0..1440; ausência significa desconhecido |
| `sortOrder` | sim | integer 0..2147483647 |
| `desiredAt` | não | RFC 3339 com offset, no futuro dentro da janela operacional configurada |
| latitude/longitude | não, mas em par | números finitos; [-90,90]/[-180,180], no máximo 6 casas |
| email | não | trim+lowercase, ≤254; validação sintáctica, sem afirmar entrega |
| telefone | sim nos contactos usados | E.164 normalizado; `+` e 9..15 dígitos |

JSON `null` explícito e campo omitido têm a mesma semântica apenas para opcionais; campo obrigatório ausente/null é 400. O backend não devolve placeholders (`0 km`, rating `0`, string vazia) para ausência de dados.

## 11. Discovery: contrato detalhado

### 11.1 `GET /api/v1/discovery/home`

Finalidade: categorias e secções `nearby`, `recommended`, `featured`. Query: `latitude`+`longitude` em par; `municipalityId?`; `categoryId?`; `page=0`; `pageSize=20` (1..100); `sort=NAME`. **Não aceita `query`**; pesquisa textual pertence a `/search`. Sem coordenadas, `nearby.items=[]`; com coordenadas só pode ser preenchido quando houver fonte geográfica real.

Response 200: `{categories:[], nearby:{items,hasMore}, recommended:{items,hasMore}, featured:{items,hasMore}, generatedAt}`. Secção anunciada só pode conter fonte persistente; na implementação inicial `featured` pode ficar vazia, mas `FEATURED` não pode fingir suporte. `forceRefresh` nunca atravessa HTTP.

### 11.2 `GET /api/v1/discovery/search`

Query: `query` 0..100; `categoryId?`; `onlyOpen=false`; `fulfillmentOptions=PICKUP&fulfillmentOptions=DELIVERY`; coordenadas em par; `municipalityId?`; `sort`; paginação. OR dentro de fulfillment; AND entre query/categoria/open/fulfillment/geografia. `onlyOpen=true` aceita apenas `OPEN`/`CLOSING_SOON`, nunca `UNKNOWN`. `NEAREST` exige coordenadas e geografia persistida. `TOP_RATED`, `MOST_POPULAR`, `FEATURED` exigem respectivamente rating, popularidade e flag/curadoria persistentes. Até existirem, retornam 400 `INVALID_REQUEST` com field error `sort`, não fallback.

Response 200: `{categories:[], merchants:[], page,pageSize,totalCount,hasMore}`. Ordenação sempre termina em `merchantId ASC` para estabilidade. Página além do fim é 200 vazia.

### 11.3 `GET /api/v1/discovery/merchants/{merchantId}`

Path UUID público. Response é Merchant completo acrescido de `fullDescription?`, `bannerUrl?`, `logoUrl?`, `address?`, `publicContact?`, `weeklySchedule?`, `catalogId?`. Inexistente, despublicado, inactivo ou de outro tenant produz o mesmo 404 `MERCHANT_NOT_FOUND`. Esta rota plural substitui o singular factual `/discovery/merchant/{merchantId}`; manter alias temporário é decisão de rollout, não parte do contrato.

### 11.4 Regras comuns e aceite Discovery

- 200/304; 400 invalid; 404 apenas detalhe; 429; 500; 503. GET é naturalmente idempotente.
- Autorização é a política de publicação: conta/tenant/instituição/unidade activos, opt-in de Discovery e catálogo publicado. A branch actual não possui `discoveryPublished`; é migration requerida.
- ETag muda com os factos projectados. 503 é retryable; 400/404 não. URLs de imagem apenas HTTPS de hosts/CDN autorizados.
- Testes mínimos: allowlist de parâmetros, repetidos, bounds, 0-based, página vazia, filtros OR/AND, cada sort com e sem fonte, pares de coordenadas, merchant não publicável indistinguível, desempate, ETag/304, cache, rate limit, SQL parametrizado, ausência de tenantId/PK/PII e isolamento concorrente de `TenantContext`.

## 12. Catálogo e produtos: contrato detalhado

### 12.1 `GET /api/v1/merchants/{merchantId}/catalog`

Finalidade: metadata e categorias do catálogo publicado, não todos os produtos. Query nenhuma. Response 200:

```json
{
  "catalogId": "7b318b44-7724-4d60-8f6d-95d376fc8283",
  "merchantId": "75c8eeb9-f1dd-4985-bc51-5c3ec9180fe7",
  "name": "Cardápio principal",
  "description": null,
  "catalogVersion": "42",
  "currencyCode": "AOA",
  "updatedAt": "2026-08-05T10:00:00Z",
  "categories": [{"categoryId":"c73995e0-a54e-43fd-b7cf-15f96aa2fe21","name":"Pratos","description":null,"imageUrl":null,"sortOrder":0,"available":true}]
}
```

### 12.2 `GET /api/v1/merchants/{merchantId}/products`

Query: `query` 0..100, `categoryId?`, `onlyAvailable=false`, `page=0`, `pageSize=20` (máx. 100). Response paginada com produtos resumidos; `optionGroupCount` pode ser incluído, mas os grupos completos são obrigatórios no detalhe. Ordem `sortOrder,name,productId`. Category tem de pertencer ao mesmo merchant/catalog.

### 12.3 `GET /api/v1/merchants/{merchantId}/products/{productId}`

Response inclui produto completo e grupos ordenados. Cada grupo: `optionGroupId,name,description?,minimumSelections,maximumSelections,required,singleChoice,sortOrder,options[]`. Cada opção: `optionId,name,description?,additionalPrice,available,defaultSelected,sortOrder`.

### 12.4 Regras comuns e aceite de catálogo

- Pública, read-only, tenant resolvido por merchant; 200/304, 400, 404 `CATALOG_NOT_FOUND`/`PRODUCT_NOT_FOUND`, 409/410 não usados em GET, 429/500/503.
- Catálogo não publicado é `CATALOG_UNAVAILABLE` 404/503 conforme permanente/temporário, sem revelar tenant. Produto despublicado retorna 404; conhecido mas temporariamente indisponível permanece 200 com estado.
- Cache/ETag dependem de `catalogVersion`/`productVersion`; `If-None-Match` obrigatório de suportar. IDs numéricos, `BigDecimal` e enums internos nunca saem.
- Testes: publicação, categoria activa, gallery ordenada, URL segura, paginação/filtros, determinismo, option constraints/defaults, cross-tenant, queries sem N+1, version bump em toda alteração comercial, arredondamento/conversão e produto alterado entre GET e quote.

## 13. Cotação: `POST /api/v1/checkout/quotes`

Headers comuns; sem `Idempotency-Key` obrigatório (repetir pode gerar nova quote equivalente). Body:

```json
{
  "merchantId": "75c8eeb9-f1dd-4985-bc51-5c3ec9180fe7",
  "catalogVersion": "42",
  "items": [{
    "clientItemId": "96cf76d1-4049-4ee7-ac5d-c0c9d89e24dc",
    "productId": "5bc9346e-d18a-4ae0-98be-98780dbce4d5",
    "quantity": 2,
    "selectedOptions": [{"optionGroupId":"62c09e68-c80c-433e-b5c9-4d09c85865be","optionIds":["00a02677-5a8e-41a2-a49a-cb795286377b"]}],
    "note": "Sem cebola"
  }],
  "fulfillment": {
    "method": "DELIVERY",
    "desiredAt": null,
    "delivery": {
      "province": "Luanda", "municipality": "Luanda", "districtOrArea": "Maianga",
      "streetOrReference": "Rua 10", "buildingOrHouse": null,
      "referencePoint": "Banco BIC", "latitude": -8.83, "longitude": 13.23,
      "recipientName": "Ana Silva", "recipientPhone": "+244923456789",
      "instructions": "Ligar ao chegar"
    }
  }
}
```

`PICKUP` usa `pickup{contactName,contactPhone}` e `desiredAt` (null = ASAP). Exactamente um de `delivery`/`pickup` corresponde ao método. A quote não precisa de email/nome geral do comprador; PII só quando necessária a fulfillment.

Validação autoritativa: merchant/publicação/horário; fulfillment; catálogo e versão; categoria/produto/opções; min/max/default; disponibilidade; quantidade/limites; moeda/preço/promoção; endereço/área/taxa; SLA; overflow e soma. O backend ignora qualquer campo financeiro desconhecido e, pela allowlist, preferencialmente o rejeita.

Response 201:

```json
{
  "quoteId": "a3948ceb-2fe9-43bd-b90c-71d7cc2c23dd",
  "quoteVersion": "1",
  "merchant": {"merchantId":"75c8eeb9-f1dd-4985-bc51-5c3ec9180fe7","name":"Sabor da Maianga"},
  "catalogVersion": "42",
  "items": [{
    "clientItemId":"96cf76d1-4049-4ee7-ac5d-c0c9d89e24dc","productId":"5bc9346e-d18a-4ae0-98be-98780dbce4d5","name":"Muamba","quantity":2,
    "unitBasePrice":{"amountMinor":250000,"currencyCode":"AOA"},
    "selectedOptions":[],"unitOptionsTotal":{"amountMinor":0,"currencyCode":"AOA"},
    "lineTotal":{"amountMinor":500000,"currencyCode":"AOA"}
  }],
  "subtotal":{"amountMinor":500000,"currencyCode":"AOA"},
  "charges":[{"type":"DELIVERY","operation":"ADD","label":"Entrega","amount":{"amountMinor":150000,"currencyCode":"AOA"}}],
  "total":{"amountMinor":650000,"currencyCode":"AOA"},
  "estimatedPreparationMinutes":30,
  "estimatedDeliveryMinutes":45,
  "expiresAt":"2026-08-05T10:10:00Z",
  "acceptancePolicy":"MERCHANT_MUST_ACCEPT",
  "paymentPolicy":"AVAILABLE_AFTER_ACCEPTANCE",
  "divergences":[{"type":"PRICE_CHANGED","clientItemId":"96cf76d1-4049-4ee7-ac5d-c0c9d89e24dc"}]
}
```

Quote é imutável, vinculada ao tenant/merchant, client-instance e hash canónico da intenção; TTL recomendado 10 minutos; não renovável. Snapshot inclui request normalizado, produtos/opções/nomes/preços, regras, fulfillment/PII necessária, taxas, totais, políticas e versões. Códigos: 201; 400 invalid/config; 404 merchant/catalog/product; 409 `CATALOG_CHANGED`, `PRICE_CHANGED`, `FULFILLMENT_UNAVAILABLE`; 422 endereço não servido; 429/500/503.

Política de mudança: antes da quote, servidor devolve divergências e a sua verdade; Android substitui a revisão. Depois da quote, alterações não alteram snapshot. Na criação, quote expirada é rejeitada; indisponibilidade/encerramento críticos podem invalidá-la e exigir nova quote. Nunca ajustar total silenciosamente durante `POST /orders`.

Testes: todas as validações acima, concorrência de catálogo, TTL boundary/clock, imutabilidade, canonical hash, PII, combinações de options, área de entrega, aritmética/overflow, mesma intenção em ordens diferentes e abuso/rate limit.

## 14. Criação: `POST /api/v1/orders`

Headers: `Idempotency-Key` obrigatório, UUID 36; `X-Client-Instance-Id` obrigatório. Body:

```json
{
  "quoteId": "a3948ceb-2fe9-43bd-b90c-71d7cc2c23dd",
  "quoteVersion": "1",
  "customer": {"fullName":"Ana Silva","phoneNumber":"+244923456789","email":null},
  "clientReference": "f6480507-6d21-4a40-a4f0-d50cdd063239"
}
```

Não aceita preços, taxas, totais, nomes ou items. O servidor bloqueia/lê quote `FOR UPDATE`, verifica scope/TTL/estado/hash, resolve tenant/instituição/unidade/rotas, exige turno aberto quando a policy assim determinar, cria pedido/subpedidos/items/snapshots/fulfillment/auditoria/idempotência, marca quote consumida e faz commit único. Falha total faz rollback; notificação é outbox pós-commit.

Estado inicial obrigatório:

```json
{
  "orderId":"9d0c9224-a2ee-4488-a952-2829d62f0fdb",
  "orderNumber":"PED-20260805-000123",
  "operationalStatus":"PENDING_ACCEPTANCE",
  "financialStatus":"NOT_PAYABLE",
  "paymentAvailable":false,
  "statusMessage":"Pedido enviado. Aguarde o aceite do estabelecimento.",
  "orderAccessToken":"<base64url-256-bit>",
  "version":"1",
  "createdAt":"2026-08-05T10:03:00Z"
}
```

Response 201. O token é apresentado apenas no resultado lógico da criação/idempotent replay, nunca em GET. A origem interna é `CONSUMA_AQUI_ANDROID`, não `QR_PUBLICO`. Não criar sessão de mesa. Uma sessão técnica invisível só é admissível se invariantes legadas a exigirem, com tipo próprio e sem simular QR; preferência: tornar sessão opcional para esta origem.

Sem turno quando obrigatório: 409 `MERCHANT_UNAVAILABLE`, quote não consumida, nenhuma entidade parcial. Se policy permitir sem turno, pedido é criado, evento estruturado é emitido e aceite continua condicionado à abertura conforme operação aprovada.

Códigos: 201; 202 `REQUEST_IN_PROGRESS` apenas para corrida já activa, com `Retry-After`; 400; 401/403 não usados para checkout anónimo normal; 409 quote used/catalog critical/idempotency; 410 quote expired; 422; 429/500/503. Critério de aceite inclui prova de que não existe `OrdemPagamento`, gateway call ou instrução de pagamento antes de aceite.

## 15. Idempotência de criação

Scope único: hash(`X-Client-Instance-Id`) + endpoint + `Idempotency-Key`. O request hash é SHA-256 de JSON canónico (RFC 8785 ou serializador canónico versionado), depois de normalização, incluindo quoteId/version/customer/clientReference. Retenção mínima: 24 h; recomendada: 72 h; nunca menor que a janela de retry móvel.

| Situação | Resultado |
|---|---|
| mesma chave + mesmo hash, concluída | mesma semântica/status/body e mesmo orderId; header `Idempotent-Replayed: true` |
| mesma chave + hash diferente | 409 `IDEMPOTENCY_CONFLICT` |
| mesma chave em processamento | 202 `REQUEST_IN_PROGRESS`, `Retry-After: 2` |
| falha antes de commit | registro FAILED/retryable; nova tentativa pode assumir lease e executar |
| falha depois de commit/resposta perdida | replay encontra COMPLETED e devolve resultado |
| concorrência | unique scope/key + insert/row lock; exactamente um pedido |

O token pode ser reconstituído apenas para o replay lógico a partir de HMAC versionado sobre `orderPublicId + randomTokenSalt`, guardando salt, versão da chave e hash de verificação, nunca token plaintext. Alternativa de segurança mais estrita (não devolver token no replay) exige fluxo separado de recuperação e é decisão pendente. Logs mascaram keys e nunca incluem token/body completo.

## 16. Consulta: `GET /api/v1/orders/{orderId}`

Header recomendado e obrigatório: `X-Order-Access-Token: <token>`; nunca query/cookie. Um futuro Bearer de consumidor autenticado pode autorizar o mesmo recurso sem mudar o path. Token errado, expirado, revogado, order inexistente ou de outro tenant devolve 404 `ORDER_NOT_FOUND` indistinguível; 403 `ORDER_ACCESS_DENIED` é reservado a consumidor autenticado que tenta recurso conhecido.

Response 200 contém orderId/number, merchant público, snapshots de items/totais/fulfillment minimizados, `operationalStatus`, `financialStatus`, `paymentAvailable`, statusMessage, rejection `{code,message}?`, estimates, created/accepted/updated timestamps e `version`. Email/telefone completos e endereço detalhado não são devolvidos por defeito; telefone mascarado e resumo de entrega bastam.

Polling: mínimo 5 s enquanto pendente, backoff até 30 s; respeitar `Retry-After`. `ETag: "order-{version}"`, `If-None-Match` → 304. `Cache-Control: private,no-cache,no-store` é preferível devido a PII; ETag ainda evita body. Token default expira 90 dias após estado terminal e pode ser revogado; período exacto depende da decisão de retenção.

Testes: token correcto/errado/revogado/expirado, timing/enumeração, cross-tenant, PII minimizada, ETag/304, polling/rate limit, transições e mapeamento público, rejection segura, ausência de payment instructions nesta fase.

## 17. Estados públicos e pagamento após aceite

Não serializar enums internos directamente. Projecção:

| Público operacional | Interno actual/proposto |
|---|---|
| `PENDING_ACCEPTANCE` | `Pedido.CRIADO` + origem Android + termination null |
| `ACCEPTED` | `EM_ANDAMENTO`, subpedidos `PENDENTE`, ainda sem preparação |
| `PREPARING` | algum subpedido `EM_PREPARACAO` |
| `READY` | todos relevantes `PRONTO` |
| `COMPLETED` | `FINALIZADO`/entregue |
| `REJECTED` | termination_type `REJECTED` (nova persistência) |
| `CANCELLED` | termination_type `CANCELLED` |

| Público financeiro | Interno |
|---|---|
| `NOT_PAYABLE` | pré-aceite; não inferível apenas de `NAO_PAGO` |
| `UNPAID` | aceite + `NAO_PAGO` |
| `PAYMENT_PENDING` | `PENDENTE_PAGAMENTO` ou ordem/gateway pendente |
| `PAID` | `PAGO` |
| `REFUNDED` | `ESTORNADO` |

`paymentAvailable` é policy calculada, não `financialStatus != PAID`: requer aceite, estado não terminal, método permitido e infraestrutura activa. Criação: `PENDING_ACCEPTANCE/NOT_PAYABLE/false`. Aceite: `ACCEPTED/UNPAID/true`. A implementação actual cria uma ordem manual após aceite; a API v1 não a expõe nem a inicia.

## 18. Dinheiro, escala e arredondamento

AOA usa expoente contratual 2. Conversão backend: `BigDecimal.setScale(2, UNNECESSARY).movePointRight(2).longValueExact()`; inversa `BigDecimal.valueOf(amountMinor, 2)`. JSON nunca contém decimal monetário, float ou double. Persistência nova usa `numeric(19,2)` e moeda `char(3)`; migrar colunas `10,2` que possam limitar totais.

Soma e quantidade são exactas. Não há percentagens nesta fase. Caso promoções percentuais sejam introduzidas, arredondamento por componente em minor units e modo fiscal aprovado devem versionar a pricing policy; até lá fracções de minor unit são erro de configuração. Testar limites Long, multiplicação, moeda divergente, `0`, escala 0/2/>2, overflow e round-trip.

## 19. Identificadores públicos

| Campo | Formato/origem | Estabilidade/enumeração |
|---|---|---|
| merchant/catalog/category/product/group/option/order/quote ID | UUID v4 lowercase canonical, server-generated | imutável, globalmente único, não sequencial; unique DB |
| `orderNumber` | sequência tenant/data, server | humano, pode ser enumerável; nunca credencial |
| `clientItemId` | UUID Android por linha | único dentro da quote; ecoado para reconciliação |
| `clientReference` | UUID Android por tentativa lógica | unique no scope instalação; não é idem key |
| `catalogVersion`, `productVersion`, `quoteVersion`, `order.version` | bigint positivo serializado como string | monotónico no agregado, não é ID |

Slug do Discovery passa a `merchantSlug` opcional de apresentação/SEO, nunca chave referencial. Campos `public_id uuid not null default gen_random_uuid()` devem ser backfilled, validados e tornados unique/not null antes de mudar endpoints. FKs continuam nos IDs internos; requests resolvem public_id + tenant.

## 20. Modelo persistente de opções

`VariacaoProduto` não serve: escolhe uma variação singular e não suporta composição de grupos. Manter para Store legado durante transição; não mapear automaticamente para grupos excepto por migration explícita.

`product_option_groups`: `id bigint`, `public_id uuid`, `tenant_id`, `product_id`, `name varchar(120)`, `description varchar(300) null`, `min_selections smallint`, `max_selections smallint`, `sort_order int`, `active boolean`, `version bigint`, timestamps. Constraints: min≥0, max≥1, min≤max; unique `(tenant_id,public_id)`, `(tenant_id,product_id,sort_order,public_id)`; FK composta/trigger garante produto do mesmo tenant.

`product_options`: IDs/tenant/group, `name varchar(120)`, description, `additional_price numeric(19,2)`, `currency_code char(3)`, `available`, `default_selected`, `sort_order`, version/timestamps. Constraints preço≥0; unique public_id; default implica available; moeda = catálogo; número de defaults respeita grupo validado transaccionalmente.

Migration: criar nullable/dual-read desactivado; backfill apenas variações simples aprovadas como grupo min=1/max=1; validar; publicar via nova API; manter Store em `VariacaoProduto`; não eliminar legado nesta fase. Toda mutação incrementa `catalog_version` na mesma transacção.

## 21. Persistência e migrations do contrato

Reutilizar: tenants/business accounts/policies, instituição/unidade/cozinha/rota, Produto/Categoria/ProdutoImagem, Pedido/SubPedido/ItemPedido, turno, event log/outbox, delivery policy e máquina/policies após adaptação.

Extensões:

- `tenants`: `merchant_public_id`, `merchant_slug` imutável/alias, `discovery_published`, coordenadas/geografia e campos de ranking somente quando houver fonte.
- catálogo/produto/categoria/imagem: public_id, versions, moeda, short/full description, compare price, tags/featured/sort/availability necessários; catalogue `version bigint` e `updated_at` autoritativos.
- `pedidos`: `public_id`, `termination_type`, `termination_code`, rejection public message, `pedido_origem=CONSUMA_AQUI_ANDROID`, optimistic `@Version`, accepted/rejected timestamps.
- `itens_pedido`: snapshot JSONB/colunas de nome, base/options/line minor, currency e selecções; não depender de produto mutável para histórico.
- `order_fulfillments`: public address estruturado ou snapshot JSONB cifrado, recipient/contact, desired_at, fees; actual `customer_phone_masked` não basta para operação e texto único reduz validação.

Novas tabelas:

- `checkout_quotes`: public_id, tenant/merchant/catalog_version, quote_version, client_instance_hash, canonical_request_hash, status ISSUED/CONSUMED/EXPIRED/INVALIDATED, snapshots JSONB, subtotal/charges/total minor+currency, expires/consumed timestamps, order_id, version. Índices `(public_id)`, `(tenant_id,status,expires_at)`; check consumed fields.
- `android_order_idempotency`: scope_hash, key_hash/encrypted lookup digest, request_hash, status PROCESSING/COMPLETED/FAILED, lease_until, response order/status, token salt/key version, expires_at; unique `(scope_hash,key_hash)`.
- `order_access_tokens`: order_id unique, token_hash, token_salt/key_version, created/expires/revoked/last_used; nunca plaintext.
- `order_item_option_snapshots` ou snapshot JSONB validado; tabela normalizada preferida para reporting, JSONB canónico preserva história.
- option groups/options da secção 20; transactional outbox para notificação.

Tenant-aware constraints devem incluir tenant nas uniques e, onde PostgreSQL permitir, FKs compostas `(tenant_id,id)`. Retenção/cleanup: quotes após 30 dias (PII pode ser eliminada antes), idem 72 h, token conforme lifecycle, pedidos/financeiro conforme obrigação legal aprovada. Jobs usam batches, índices e auditoria.

## 22. Componentes reutilizáveis e proibidos

Reutilizáveis internamente após adaptação: publication policy/projections do Discovery; repositories tenant-scoped; CardapioConfig; Produto/Categoria/Imagem; route/unit resolution; PedidoNumberService; order/suborder aggregate; Turno policy; OperationalTemplatePolicy; PedidoPagamentoPolicy; status transitions; allowed actions; OrdemPagamentoService **somente no aceite**; event log/outbox; padrões de idempotência QR/device.

Não reutilizar como contrato Android:

- `/public/q/{token}/...`: exige QR/mesa/sessão, aceita IDs numéricos e possui scope de idempotência QR.
- `/public/cardapio/...`: não recebe merchant, usa enum legado, limite fixo 500, envelope/campos/IDs inadequados e risco de tenant context.
- `/store/catalogo/...`: domínio Loja do Sócio, variação/stock/IDs numéricos.
- `/store/ordens/...`: inicia pagamento durante checkout e usa telefone+número para tracking.
- `QR_PUBLICO` como origem; `VariacaoProduto` como grupos; preços/names enviados pelo Android; token QR como token de order.

## 23. Segurança e privacidade

- TLS 1.2+, HSTS no edge; CORS só para clientes web autorizados—Android nativo não depende de CORS.
- Tokens 256-bit CSPRNG/base64url ou HMAC equivalente; comparação constant-time; hash SHA-256/HMAC com pepper em secret manager; rotação/versionamento; revogação e expiração.
- `X-Client-Instance-Id` é pseudónimo, não segredo. Rate limit composto IP/instalação/merchant e detecção de volume; CAPTCHA/device attestation é evolução, não requisito inicial.
- PII mínima, cifrada at rest quando suportado, acesso por função, retenção definida, não incluída em métricas/URLs/traces. Logs: IDs públicos e códigos; telefone/email/endereço/nota/token/header bodies redigidos.
- Anti-enumeração: UUIDs, 404 uniforme e latência comparável; orderNumber/telefone não autorizam consulta.
- Replay: quote scope+TTL+single consumption; orders idem; token só leitura e revogável.
- Observações/endereço são texto não confiável: limites, Unicode NFC, sem HTML, escaping contextual; não executar links/comandos. Coordenadas finitas e em par.
- URLs de imagem são geradas/validadas pelo backend, HTTPS, host allowlist; prevenir SSRF, `file:` e redirects não autorizados.
- Headers: `X-Content-Type-Options:nosniff`, CSP nos consumidores web, `Referrer-Policy:no-referrer`; respostas sensíveis `no-store`.
- Responsabilidade backend: proteger dados recebidos/persistidos/transmitidos. Responsabilidade Android futura: retirar PII de `SavedStateHandle`, armazenamento cifrado, limpar checkout e evitar screenshots/logs. Backend não consegue corrigir persistência local indevida.

## 24. Observabilidade

Métricas sem PII: latência/resultado por endpoint e code, quote issued/expired/invalid, divergências, idempotent replay/conflict/in-progress, order created/failed, acceptance latency, status projection, rate-limit e tenant-isolation guard. Logs estruturados incluem traceId, endpoint template, public merchant/order ID quando autorizado e policy version; nunca token, raw idem key, payload ou PK interna. Tracing separa quote/order transaction/outbox. Alertas para criação sem quote, payment order pré-aceite, duplicação, cross-tenant e aumento de 409/5xx.

## 25. Contrato uniforme de erros

```json
{"error":{"code":"QUOTE_EXPIRED","message":"A cotação expirou.","retryable":false,"fieldErrors":[],"traceId":"01J4W4ZJQ0KQF2A4C4NQX7GT5D"}}
```

`fieldErrors`: `{field,code,message}`; paths JSON Pointer; mensagem pública localizada no Android preferencialmente por code. Matriz:

| Code | HTTP | Retry | Tratamento Android / refresh |
|---|---:|---|---|
| INVALID_REQUEST | 400 | não | corrigir campo; não repetir |
| MERCHANT_NOT_FOUND | 404 | não | sair/detalhe indisponível; refresh Discovery |
| MERCHANT_UNAVAILABLE | 409/503 | depende | informar; 503 retry/backoff |
| CATALOG_NOT_FOUND | 404 | não | voltar ao merchant |
| CATALOG_UNAVAILABLE | 409/503 | depende | bloquear compra; refresh |
| CATALOG_CHANGED | 409 | não | descarregar catálogo e pedir nova quote |
| PRODUCT_NOT_FOUND | 404 | não | remover linha após confirmação |
| PRODUCT_UNAVAILABLE | 409 | não | rever carrinho/catalog |
| INVALID_PRODUCT_CONFIGURATION | 422 | não | destacar grupos/opções; refresh produto |
| FULFILLMENT_UNAVAILABLE | 409 | não | escolher outro método/nova quote |
| DELIVERY_ADDRESS_UNSUPPORTED | 422 | não | corrigir endereço/método |
| QUOTE_UNAVAILABLE | 503 | sim | manter intenção, backoff |
| QUOTE_EXPIRED | 410 | não | pedir nova quote |
| QUOTE_ALREADY_USED | 409 | não | consultar order via idem ou reiniciar |
| PRICE_CHANGED | 409 | não | mostrar divergência/nova quote |
| ORDER_NOT_FOUND | 404 | não | mensagem genérica; não inferir existência |
| ORDER_ALREADY_CREATED | 409 | não | usar orderId devolvido só se autorizado |
| ORDER_ACCESS_DENIED | 403 | não | limpar credencial; apenas auth futura |
| IDEMPOTENCY_CONFLICT | 409 | não | gerar nova key apenas para nova intenção |
| REQUEST_IN_PROGRESS | 202/409 | sim | respeitar Retry-After, mesma key |
| RATE_LIMITED | 429 | sim | Retry-After/backoff |
| SERVICE_UNAVAILABLE | 503 | sim | cache read-only/backoff; nunca duplicar order com nova key |
| UNKNOWN | 500 | talvez | mensagem genérica/traceId; mesma idem key |

Nunca expor stack, SQL/tabelas, exception class, IDs internos, tenant alheio, infraestrutura ou regras que facilitem enumeração.

## 26. Matriz Android ↔ endpoint ↔ teste

| Android/estado | Endpoint | Cenários contratuais mínimos |
|---|---|---|
| Home Loading/Content/Empty/Error/Offline | home | secções vazias, localização ausente, 304, 503/cache |
| Search critérios SavedState | search | 0↔1, OR fulfillment, AND filtros, sort indisponível, página vazia |
| Merchant Overview | merchant detail | publicado, parcial/nulls, 404 uniforme, catalogAvailable |
| CatalogViewModel | catalog/products | versão, categorias indisponíveis, busca, filtro, paginação/304 |
| ProductDetailViewModel | product detail | options min/max/default, unavailable, price display, ownership |
| Cart local | nenhum | não criar cart backend; merchant único; snapshots não autoritativos |
| Checkout fulfillment/customer | quote | pickup/delivery, endereço inválido, preço/catalog changed, expiry |
| Review | quote response | totais do servidor, divergências e nova confirmação |
| Confirm pending | POST orders | timeout/replay, in-progress/conflict, turno, quote consumed, no payment |
| Confirmation/Orders future | GET order | token, 304/polling, estados, rejection, PII e rate limit |

## 27. Testes e critérios de aceite transversais

Cada endpoint exige unitários de validação/policy/mapper; controller e schema/OpenAPI; integração PostgreSQL/Flyway; contrato consumer-driven Android; persistência/constraints; isolamento tenant; segurança e rate limit. Não basta happy path.

Gates obrigatórios:

1. Nenhum preço/nome do request chega ao snapshot financeiro; arithmetic e overflow property-based.
2. Concorrência de quote consumption/idempotency cria exactamente um pedido.
3. Mesmo merchant public_id nunca resolve tenant errado; IDs de opção/produto cruzados falham sem leak.
4. Pedido sem turno obedece policy e faz rollback total.
5. Criação produz PENDING_ACCEPTANCE/NOT_PAYABLE/false e zero ordens/gateway calls.
6. Aceite produz ACCEPTED/UNPAID/true e só então cria/disponibiliza ordem conforme policy.
7. Rejeição persiste tipo estruturado REJECTED e não se confunde com CANCELLED.
8. Token inválido/ID aleatório apresentam envelope, status e timing compatíveis; telefone+número falha.
9. Catalog/price/product/option changed e quote expiry conduzem a nova revisão.
10. Migration/backfill tem teste de rollback, uniques, FKs tenant-aware e dados legados.

## 28. Paginação, filtros e normalização — resolução da divergência

Contrato HTTP: `page=0`, default 0; `pageSize=20`, 1..100. Android adapter converte request `max(1,N)-1` e response `page+1`; UI actual continua first-page sem alteração imediata. `forceRefresh` selecciona cache policy no repository Android, nunca query/header especial. `fulfillmentOptions` repetido é a única repetição permitida. Query vazia lista; acentos/case usam política de busca documentada (PostgreSQL unaccent/ILIKE ou índice equivalente) e desempate público.

## 29. Disponibilidade, horários e fulfillment

Availability é projecção calculada com timezone do merchant e horário excepcional/feriados, não boolean inventado. Valores públicos: `OPEN`, `CLOSING_SOON`, `OPENS_AT`, `CLOSED`, `UNKNOWN`. `onlyOpen` não considera UNKNOWN. Preparação/SLA são nullable quando não calculáveis.

Fulfillment público Discovery: `PICKUP`, `DELIVERY`, `DINE_IN`, `SERVICE`; checkout v1 aceita só `PICKUP`/`DELIVERY`. Backend `CUSTOMER_PICKUP`, `TENANT_DELIVERY`, `CONSUMA_NETWORK_DELIVERY` é mapeamento interno. Área/taxa de entrega precisam de policy persistente; `DeliveryFeeQuoteService`/OrderFulfillment podem ser reaproveitados, mas texto de morada e telefone mascarado actuais não satisfazem todo o contrato.

## 30. Políticas de catálogo e quote

`catalogVersion` incrementa em qualquer mudança que possa alterar publicação, seleção ou preço: categoria/produto/opção, disponibilidade, promoção, moeda, fulfillment/taxa relevante. Quote guarda policy version e não depende de leitura mutable posterior. `divergences` informa mudanças observáveis entre intenção/version e verdade; nunca transforma uma configuração inválida numa diferente sem consentimento.

## 31. Criação de subpedidos e operação

O tenant vem do merchant da quote. Instituição/unidade são a unidade pública configurada para pedidos Android; não escolher “primeira” implicitamente. Itens são agrupados por rota/cozinha com tenant e categoria validados. Production-disabled pode criar pedido sem subpedido apenas se máquina/policies suportarem explicitamente. Turno é locked/resolvido antes de persistir. Número usa `PedidoNumberService`, não random. Auditoria regista origem/policy/quote/clientReference sem PII. Outbox notifica estabelecimento depois do commit.

## 32. Compatibilidade factual Android ↔ Backend

| Capacidade | Android | Backend factual | Resultado contratual |
|---|---|---|---|
| IDs | String mock | slug Discovery/Long restantes | UUID público + migration |
| Money | Long minor | BigDecimal scale 2 | conversão exacta |
| Discovery page | 1 | 0 | adapter Android |
| 5 sorts/open/fulfillment | mock suporta | só NAME; sem filtros | não anunciar até persistir |
| Coordenadas Discovery | ausentes | aceita mas não usa | evolução Android + fonte geo |
| Catálogo versionado | campo opcional | timestamp, sem version | version bigint |
| Grupos/opções | completos | variação simples | novas tabelas |
| Cart | local | Store possui outro fluxo | permanece local |
| Quote | mock | inexistente | novo agregado/endpoint |
| Order Android | inexistente | QR/Store/device | nova facade/origem |
| Anonymous lookup | futuro | QR token ou phone+number | token por pedido |
| Payment after accept | requerido | policy existente | preservar/provar |

## 33. Lacunas bloqueantes

Bloqueiam implementação de order: quote/snapshot inexistentes; IDs públicos de catálogo/pedido/opções; modelo de grupos/opções; catalogue version; origem Android; token de acesso; idempotência scoped à instalação; projecção pública de estados; rejeição persistente distinta; unidade pública/turno definidos; error envelope uniforme.

Bloqueiam capacidades Discovery anunciadas: branch não integrada; flag opt-in de publicação; geografia; horários; rating; popularidade; featured; filtro fulfillment. Até resolver, apenas NAME e filtros com fonte real são contractualmente activáveis.

## 34. Sequência de implementação e gates

1. **Fundação**: error envelope, UUIDs públicos/backfill, dinheiro e tenant constraints. Gate: migrations e anti-enumeração.
2. **Integrar/corrigir Discovery**: plural path, publicação opt-in, paginação; apenas capabilities reais. Gate: testes branch + main.
3. **Catálogo público**: versionamento, DTOs, cache/ETag, imagens. Gate: IDs e isolation.
4. **Grupos/opções**: schema, administração e read model. Gate: validator completo/migration legado.
5. **Quote**: snapshot, pricing, TTL, divergências. Gate: concorrência/arithmetic/fulfillment.
6. **Pickup/delivery**: áreas, taxa, unidade/rota/turno. Gate: policy operacional definida.
7. **Segurança anónima/idempotência**: instance scope, access token, cleanup. Gate: replay/enumeração.
8. **Order creation**: transacção, snapshots, origem, outbox. Gate: quote obrigatória e zero payment pré-aceite.
9. **Projecção de estados**: termination type, financeiro/policy. Gate: aceite/rejeição/cancelamento testados.
10. **GET order**: token, ETag/polling/PII. Gate: segurança e rate limiting.
11. **Contrato Android**: DTO adapter, mocks substituídos gradualmente e suite end-to-end. Gate: consumer-driven contract.

Não iniciar fase 8 antes dos gates 3–7.

## 35. Riscos

- Divergência de branches pode fazer Discovery não compilar com o backend mais recente; requer integração controlada separada.
- Alterar slug para UUID quebra Android mock/deep links se adapter não traduzir.
- `BigDecimal(10,2)` pode limitar valores e conversão incorrecta multiplicar/dividir AOA por 100.
- Estado interno agregado não distingue ACCEPTED de PREPARING sem subpedidos/timestamps; projecção deve usar factos explícitos.
- Rejeição textual como cancelamento perde semântica definitivamente.
- Token perdido num timeout requer replay seguro; design HMAC e rotação precisam revisão de segurança.
- PII em SavedState Android e snapshots de quote aumenta superfície de retenção.
- Store/QR parecem próximos e podem ser reutilizados por conveniência, introduzindo pagamento precoce ou contexto errado.
- Rating/popularity/distance falsos degradam confiança; manter null/unsupported.

## 36. Critérios de compatibilidade e versionamento

Adicionar campos opcionais é compatível; remover/renomear, mudar nullability, enum sem fallback Android, unidade monetária ou semântica requer nova versão. Android DTO deve mapear enum desconhecido para UNKNOWN quando seguro; método/estado crítico desconhecido bloqueia checkout. OpenAPI é gerado e diffado em CI; exemplos são testados. `quoteVersion` versiona instância, enquanto pricing/schema policy version fica no snapshot/auditoria.

## 37. Decisões RC.1 reavaliadas

| ID | Decisão | Estado | Evidência/default seguro | Blocking? | Fase responsável |
|---|---|---|---|---|---|
| D-01 | TTL da quote | `RESOLVED` | 10 minutos, configurável; não foi encontrado impedimento técnico | não | `BACKEND-ANDROID-CHECKOUT-QUOTE-001` |
| D-02 | TTL do token | `DEFERRED_WITH_SAFE_DEFAULT` | 90 dias após estado terminal, configurável; separado da retenção legal | não | `BACKEND-ANDROID-ANONYMOUS-SECURITY-IDEMPOTENCY-001` |
| D-03 | Replay do token | `RESOLVED` | HMAC versionado de domínio+order public ID+salt aleatório; guardar salt, key version e hash de verificação; reter chave antiga pela janela de replay de 72 h | não | `BACKEND-ANDROID-ANONYMOUS-SECURITY-IDEMPOTENCY-001` |
| D-04 | geografia/rating/popularidade/featured/horários | `DEFERRED_WITH_SAFE_DEFAULT` | sem fonte persistente real; capabilities ficam `supported=false` e campos omitidos/null | não | `BACKEND-DISCOVERY-CANONICAL-INTEGRATION-001` |
| D-05 | merchant → unidade | `UNRESOLVED_BLOCKING` | exige mapping persistido e explícito; ausência devolve `MERCHANT_UNAVAILABLE`; é proibido escolher a primeira unidade | sim | `BACKEND-ANDROID-FULFILLMENT-POLICY-001` |
| D-06 | fiscal/rounding futuro | `OUT_OF_V1` | V1 não aceita percentagens; dinheiro exacto em minor units | não | futura policy fiscal versionada |
| D-07 | alias singular Discovery | `RESOLVED` | `/discovery/merchants/{merchantId}` plural é canónico; alias singular não integra o contrato V1 | não | `BACKEND-DISCOVERY-CANONICAL-INTEGRATION-001` |
| D-08 | `VariacaoProduto` | `RESOLVED` | novo modelo de grupos/opções; backfill somente opt-in e validado para caso simples | não | `BACKEND-PRODUCT-OPTIONS-CANONICAL-001` |
| LEGAL-01 | retenção legal de pedido/PII em Angola | `UNRESOLVED_NON_BLOCKING` | `LEGAL_RETENTION_UNRESOLVED`; nenhum prazo jurídico foi inventado | não para o contrato técnico | revisão jurídica versionada |

Estas decisões não autorizam atalhos nos bloqueios de segurança, quote, pagamento após aceite ou IDs públicos.

## 38. Veredicto de prontidão

**CONTRATO ANDROID V1 CONGELADO COM BLOCKERS — MAPPING MERCHANT→UNIDADE AUSENTE; FUNDAÇÕES PÚBLICAS E ENDPOINTS AINDA NÃO IMPLEMENTADOS.**

O contrato está suficientemente preciso para execução sequenciada, mas permanece em `1.0.0-rc.2`: o mapping operacional explícito de merchant para instituição/unidade ainda não tem decisão persistente na main. A backend factual não está pronta para integração Android end-to-end: faltam IDs públicos transversais, catálogo/versionamento público, opções compatíveis, quote, token anónimo, origem Android, idempotência própria e projecção persistente de rejeição. A criação do pedido não deve iniciar antes dos gates das fases 1–7.

## 39. Checklist de aprovação por endpoint

Para cada endpoint, a revisão deve confirmar: finalidade; auth/autorização; path/query/header/body; required/optional/null; enum/limite/normalização; resposta; HTTP/error; idempotência; cache; rate limit; segurança; tenant resolution; critérios e testes. As secções 8–17 constituem em conjunto o contrato desses 23 itens; nenhuma implementação pode interpretar a ausência de um campo no exemplo como ausência de regra comum.

## 40. Evidência insuficiente declarada

Não foi encontrada fonte persistente aprovada para rating, popularidade, featured, distância/geocódigo ou horário “open now”. Não foi encontrada definição jurídica de retenção PII, política fiscal de arredondamento percentual ou mapeamento único merchant→unidade pública. O documento não inventa essas capacidades; recomenda defaults restritivos e marca aprovação necessária.

## 41. Registo da elaboração RC.1

Esta secção descreve somente a elaboração de entrada `1.0.0-rc.1`. O congelamento `ANDROID-API-CONTRACT-FREEZE-001` é registado nas secções seguintes e nos relatórios versionados.

## 42. Baselines e reconciliação formal de branches

- Android: `origin/android/checkout-flow-mock-001@b8b6fffd048558e0c1cd688f5addac7d5fac18bb`.
- Backend: `origin/main@124bb92fff9e599a102b6f174390e90ec1069c09`, schema `V20260722.01`.
- Discovery histórica: `origin/backend/discovery-api-001@b979d2ead1d6663af5955c55d54bdae6861832bc`.
- Merge-base Discovery/main: `4568d1ddeb712a80ca3939f9a332e94db52cff3e`.
- Divergência `origin/main...origin/backend/discovery-api-001`: main ahead 10; Discovery ahead 2.
- Decisão: merge integral proibido; reimplementar/portar selectivamente contra a main vigente.

| Commit | Path/símbolo | Equivalente na main | Decisão | Adaptação/teste | Fase |
|---|---|---|---|---|---|
| `13a61ef` | `DiscoveryController`, `/home`, `/search` | ausente | `REIMPLEMENT_AGAINST_CURRENT_MAIN` | pluralização, UUID, envelope, cache/ETag, publication opt-in e capability tests | Discovery canonical |
| `13a61ef` | `/merchant/{merchantId}` singular + slug | ausente | `DEPRECATE` | criar apenas `/merchants/{merchantId}` UUID; nenhum alias canónico | Discovery canonical |
| `13a61ef` | `PersistentDiscoveryRepository`/queries | modelos persistentes evoluíram | `PORT` | adaptar joins/policies à main e remover first-unit implicit | Discovery canonical |
| `13a61ef` | `InMemoryDiscoveryRepository`/scenarios | nenhum runtime equivalente | `DROP` para runtime; `KEEP_MAIN` fixtures só em teste | proibir placeholder em resposta pública | Discovery canonical |
| `13a61ef` | Security/JWT/Tenant filters | main contém hardening posterior | `KEEP_MAIN` | adicionar allowlist pública mínima sobre SecurityConfig actual | Identity + Discovery |
| `13a61ef` | 11 testes Discovery | ausentes na main | `PORT` | actualizar paths, UUIDs, envelope, capacidades e isolamento | Discovery canonical |
| `b979d2e` | relatório histórico | relatórios actuais | `DEPRECATE` como evidência histórica | referenciar, não tratar como runtime actual | esta auditoria |

## 43. Matriz factual dos nove endpoints

| Método/path | Contrato | Main actual/handler/DTO/persistência | Histórica | Migration/service/tests/consumer | Blocker e fase |
|---|---|---|---|---|---|
| GET `/discovery/home` | alvo congelado | ausente | parcial, handler `DiscoveryController.home`; DTO próprio; persistência tenant/cardápio | publication/UUID; service read model; portar 11 testes; Android Remote pode consumir somente após main/deploy | `HISTORICAL_BRANCH_ONLY`; Discovery canonical |
| GET `/discovery/search` | alvo congelado | ausente | parcial; `NAME`, query/category/municipality; page 0 | publication/UUID; filtros reais; Android adapter 1→0 | `HISTORICAL_BRANCH_ONLY`; Discovery canonical |
| GET `/discovery/merchants/{merchantId}` | plural UUID | ausente | singular `/merchant`, slug e first unit | public IDs + mapping operacional; contract/security tests | `HISTORICAL_BRANCH_ONLY`; Discovery canonical |
| GET `/merchants/{merchantId}/catalog` | catálogo versionado | ausente; `/public/cardapio` e Store não equivalem | ausente | public IDs/catalogVersion; catalog facade/tests | `NOT_IMPLEMENTED`; Public Catalog |
| GET `/merchants/{merchantId}/products` | página pública | ausente; handlers tenant/Store não equivalem | ausente | public IDs/productVersion/pagination | `NOT_IMPLEMENTED`; Public Catalog |
| GET `/merchants/{merchantId}/products/{productId}` | detalhe + grupos | ausente | ausente | novas options + IDs; validator | `NOT_IMPLEMENTED`; Product Options |
| POST `/checkout/quotes` | autoridade financeira | ausente | ausente | quote/snapshot/version/pricing/TTL | `NOT_IMPLEMENTED`; Checkout Quote |
| POST `/orders` | consome quote/idem | ausente; QR/Store/device incompatíveis | ausente | origem, idempotência, token, termination, mapping/turno | `NOT_IMPLEMENTED`; Order Creation |
| GET `/orders/{orderId}` | token opaco | ausente; QR/Store lookup incompatíveis | ausente | public projection/token/rate-limit | `NOT_IMPLEMENTED`; Order Query |

Nenhum dos nove endpoints está `IMPLEMENTED` na main de referência. Três têm apenas implementação histórica parcial; seis não estão implementados. Endpoints internos semelhantes não contam como implementação.

## 44. Matriz de capabilities Discovery

| Capacidade | Branch histórica | Main | Contrato alvo |
|---|---|---|---|
| `NAME` | `IMPLEMENTED` | ausente | suportar após integração canónica |
| query/category/municipality | `IMPLEMENTED`/parcial conforme fonte fiscal | ausente | suportar com persistência e testes |
| latitude/longitude | `FAKE/PLACEHOLDER`: valida e ignora | ausente | reservado; `supported=false` |
| `NEAREST`/distance | `UNSUPPORTED`; nearby vazio | ausente | `supported=false` |
| `TOP_RATED`/rating | `UNSUPPORTED` | ausente | `supported=false` |
| `MOST_POPULAR`/popularity | `UNSUPPORTED` | ausente | `supported=false` |
| `FEATURED` | `FAKE/PLACEHOLDER`: secção vazia | ausente | `supported=false` |
| `onlyOpen`/horários | `UNSUPPORTED`; availability sem fonte horária completa | ausente | `supported=false` |
| fulfillment filter | `UNSUPPORTED`; opções apenas projectadas | ausente | `supported=false` |
| publication | `PARTIAL`: estado/cardápio/local activo, sem opt-in Discovery | policies actuais persistem | criar `discoveryPublished` antes de endpoint |
| cache/ETag | `no-store`, sem ETag | ausente | cache público 60 s + ETag após source/version real |

## 45. Matriz pública de estados

| Operacional | Financeiro | Factos internos mínimos | Payment available | Terminal | Source/gap |
|---|---|---|---|---|---|
| `PENDING_ACCEPTANCE` | `NOT_PAYABLE` | `CRIADO`, origem Android, termination null, zero ordem | false | não | PONTO payment-after-acceptance |
| `ACCEPTED` | `UNPAID` | `EM_ANDAMENTO`, relevantes `PENDENTE`, aceite válido | policy true quando infra/método activos | não | main já garante ordem só após aceite |
| `PREPARING` | `UNPAID/PAYMENT_PENDING/PAID` | algum relevante `EM_PREPARACAO` | policy | não | projecção nova necessária |
| `READY` | `UNPAID/PAYMENT_PENDING/PAID` | todos relevantes `PRONTO` | policy | não | projecção nova necessária |
| `COMPLETED` | `UNPAID/PAYMENT_PENDING/PAID/REFUNDED` | `FINALIZADO` | false | sim | main operacional |
| `REJECTED` | `NOT_PAYABLE` | `terminationType=REJECTED` | false | sim | blocker: main grava `CANCELADO` |
| `CANCELLED` | `NOT_PAYABLE/UNPAID/REFUNDED` | `terminationType=CANCELLED` | false | sim | blocker: termination ausente |

Enums internos nunca são serializados directamente. `NAO_PAGO` pré-aceite projecta `NOT_PAYABLE`, não `UNPAID`.

## 46. Token, replay e lifecycle

A estratégia congelada é HMAC reconstituível versionado, com pelo menos 256 bits de saída, domínio separado, `orderPublicId` e salt aleatório por pedido. Persistem-se salt, versão da chave e hash de verificação; nunca plaintext. Durante 72 horas, replay idempotente com mesmo hash pode reconstituir o mesmo token usando a key version. Rotação mantém chaves referenciadas até terminar a janela; depois disso a consulta verifica o token apresentado pelo hash persistido. Revogação é campo persistido. Roubo apenas da base não fornece o segredo HMAC; resposta perdida é coberta pelo replay; 404 uniforme reduz enumeração.

O token expira por default configurável 90 dias após terminal. Isto não elimina o pedido nem decide retenção jurídica. `LEGAL_RETENTION_UNRESOLVED` permanece não bloqueante para o contrato técnico.

## 47. PII Android

`CheckoutViewModel` persiste no `SavedStateHandle`: nome, telefone e email do cliente; nome/telefone de pickup; província, município, área, rua/referência, edifício/casa, ponto de referência, nome/telefone do destinatário e instruções de entrega. Também persiste session/step/fulfillment, que não são PII por si. Decisão: `PII_SAVEDSTATE_REMOVAL_REQUIRED`, responsável `ANDROID-REMOTE-CONTRACT-INTEGRATION-001` antes de transportar dados reais. Esta fase não altera source Android.

## 48. Fonte machine-readable e OpenAPI

O proprietário machine-readable é `maspe-residencial/docs/contracts/android/CONSUMA_AQUI_ANDROID_V1.contract.json`. Esta cópia Android é snapshot byte-a-byte e deve ter SHA-256 idêntico. O OpenAPI 3.1.0 está no backend e contém exactamente nove paths/operações. Cada operação declara `x-consuma-implementation-status`; neste freeze: 0 `IMPLEMENTED`, 0 `PARTIAL`, 3 `HISTORICAL_BRANCH_ONLY`, 6 `NOT_IMPLEMENTED`.

## 49. Sequência executável oficial

1. `BACKEND-ANDROID-PUBLIC-IDENTITY-FOUNDATION-001` — UUIDs públicos, envelope, dinheiro e tenant constraints.
2. `BACKEND-DISCOVERY-CANONICAL-INTEGRATION-001` — reimplementação/port manual Discovery contra main, sem merge integral.
3. `BACKEND-ANDROID-PUBLIC-CATALOG-001` — catálogo/produtos/versionamento e ETag.
4. `BACKEND-PRODUCT-OPTIONS-CANONICAL-001` — grupos/opções e migração validada.
5. `BACKEND-ANDROID-FULFILLMENT-POLICY-001` — mapping merchant→instituição/unidade, rota/cozinha, pickup/delivery e turno.
6. `BACKEND-ANDROID-CHECKOUT-QUOTE-001` — quote/snapshot/pricing após fulfillment resolvido.
7. `BACKEND-ANDROID-ANONYMOUS-SECURITY-IDEMPOTENCY-001` — client instance, idempotência, HMAC/token e cleanup.
8. `BACKEND-ANDROID-ORDER-CREATION-001` — transacção, origem Android e zero payment pré-aceite.
9. `BACKEND-ANDROID-PUBLIC-ORDER-PROJECTION-001` — termination e estados públicos.
10. `BACKEND-ANDROID-ORDER-QUERY-001` — GET seguro, ETag, rate limit e PII mínima.
11. `ANDROID-REMOTE-CONTRACT-INTEGRATION-001` — consumer-driven adapters, remoção PII SavedState e substituição gradual de mocks.

A ordem candidata foi ajustada: fulfillment/mapping/turno precede quote, pois quote deve validar método, endereço, taxa e unidade; implementar quote antes dessa policy criaria snapshot incompleto. A fase Android `android/discovery-remote-001` permanece preservada, mas só pode consumir Home/Search/Merchant do backend canónico integrado e publicável. Enquanto isso: `BLOCKED_BY_BACKEND_CANONICAL_DISCOVERY`; nunca apontar produção para a branch histórica isolada.
