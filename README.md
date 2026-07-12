# CONSUMA Aqui - Android App

## Visão Resumida
A CONSUMA Aqui é a aplicação pública do consumidor dentro do ecossistema CONSUMA. Este projecto contém o frontend nativo Android.

## Stack Técnica
- Kotlin 2.0.21
- Jetpack Compose
- Gradle Kotlin DSL
- Hilt para injecção de dependências
- Navigation Compose para navegação

## Requisitos
- Android Studio Ladybug ou superior
- JDK 17 (configurado no Android Studio)
- Dispositivo ou emulador com Android 8.0+ (API 26+)

## Como abrir no Android Studio
1. Abra o Android Studio.
2. Seleccione "Open".
3. Navegue até o directório raiz do projecto `consumaaqui`.
4. Aguarde o Gradle sincronizar.

## Como executar
1. Seleccione a variante de build (ex: `debug`).
2. Clique no botão "Run" (Shift + F10).

## Como executar testes
- Testes unitários: `./gradlew testDebugUnitTest`
- Testes unitários por variante: `./gradlew testDebugUnitTest`, `./gradlew testStagingUnitTest`, `./gradlew testReleaseUnitTest`
- Lint: `./gradlew lintDebug`
- Testes instrumentados: `./gradlew connectedDebugAndroidTest` (requer dispositivo ou emulador acordado e desbloqueado)

## Fluxo de Navegação
A aplicação segue o fluxo estrutural:

```
Splash → Initialization Gateway → App Shell
```

Dentro do `App Shell`, a navegação principal é feita através de `Bottom Navigation` com quatro destinos permanentes:
- **Início**
- **Pesquisar**
- **Pedidos**
- **Mais**

Os destinos secundários (Definições, Ajuda, Sobre, Design System) são acedidos a partir de `Mais` e não apresentam `Bottom Navigation`.

## Ambientes
A aplicação suporta os seguintes ambientes:
- **DEBUG**: Ambiente de desenvolvimento; apresenta ferramentas de desenvolvimento em `Mais`, incluindo o catálogo do Design System.
- **STAGING**: Ambiente de testes integrados; preserva as ferramentas de validação, incluindo o Design System.
- **RELEASE**: Ambiente de produção; não apresenta ferramentas de desenvolvimento e não regista a rota do Design System no grafo navegável.

A decisão sobre ambientes é centralizada em `NavigationPolicy` e resolvida a partir de `BuildConfig.ENVIRONMENT`.

## Estrutura Inicial
- `core/`: Componentes base (design system, ambiente, navegação, app state, ui states, erros).
- `feature/`: Funcionalidades isoladas (bootstrap, home, search, orders, more, settings, help, about, developer).
- `data/`: Camada de dados (repositórios, fontes de dados) — ainda não implementada.

## Regras de Contribuição
- O desenvolvimento é orientado por funcionalidades.
- Não misturar lógica de negócio com a interface.
- Utilizar os componentes visuais centralizados no `core/designsystem`.
- **NÃO** usar strings hardcoded. Todas as strings devem estar em `strings.xml`.
- **NÃO** usar cores hexadecimais espalhadas no código. Utilize os tokens e as paletas em `Theme.kt`, `Color.kt` e `SemanticColors.kt`.
- **NÃO** usar margens e paddings arbitrários. Utilize os tokens de `ConsumaSpacing` e de `ConsumaSize`.
- Ícones de acção devem ter descrição; ícones meramente decorativos devem evitar anúncios duplicados. Estados de loading/disabled e valores de preço/desconto devem preservar semântica acessível.
- Catálogo de UI: Em modo de desenvolvimento e staging, a tela `DesignSystemCatalogScreen` possibilita a auditoria de componentes e temas. Não está disponível em `RELEASE`.
- Avisos: URLs e secrets NÃO devem ser versionados neste repositório.

## Sequência de Prompts
Este projecto é mantido através de prompts sequenciais:
1. `app-foundation-001`
2. `design-system-foundation-001`
3. `app-shell-navigation-001` (Actual)
... (até a integração final)
