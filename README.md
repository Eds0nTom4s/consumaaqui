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
- Testes Compose (instrumentados): `./gradlew connectedDebugAndroidTest` (requer emulador)

## Ambientes
A aplicação suporta os seguintes ambientes:
- **DEBUG**: Ambiente de desenvolvimento (local/mock).
- **STAGING**: Ambiente de testes integrados.
- **RELEASE**: Ambiente de produção.

## Estrutura Inicial
- `core/`: Componentes base (design system, ambiente, navegação, ui states, erros).
- `feature/`: Funcionalidades isoladas (bootstrap, placeholder).
- `data/`: Camada de dados (repositórios, fontes de dados).

## Regras de Contribuição
- O desenvolvimento é orientado por funcionalidades.
- Não misturar lógica de negócio com a interface.
- Utilizar os componentes visuais centralizados no `core/designsystem`.
- **NÃO** usar strings hardcoded. Todas as strings devem estar em `strings.xml`.
- **NÃO** usar cores hexadecimais espalhadas no código. Utilize os tokens e as paletas em `Theme.kt`, `Color.kt` e `SemanticColors.kt`.
- **NÃO** usar margens e paddings arbitrários. Utilize os tokens de `ConsumaSpacing` e de `ConsumaSize`.
- Catálogo de UI: Em modo de desenvolvimento, a tela `DesignSystemCatalogScreen` possibilita a auditoria de componentes e temas.
- Avisos: URLs e secrets NÃO devem ser versionados neste repositório.

## Sequência de Prompts
Este projecto é mantido através de prompts sequenciais:
1. `app-foundation-001` (Actual)
2. `design-system-foundation-001`
3. `app-shell-navigation-001`
... (até a integração final)
