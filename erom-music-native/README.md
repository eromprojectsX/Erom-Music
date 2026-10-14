# Erom Música — Android nativo

Projeto Android nativo em Kotlin e Jetpack Compose. Não usa Chrome, WebView nem carrega o site como tela principal.

## Compilar

Requisitos: JDK 17, Android SDK 35 e Gradle 8.9.

```bash
gradle assembleDebug
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## O que já existe

- Interface Android nativa com Início, Buscar e Sua Biblioteca.
- Tema escuro com detalhes verdes.
- Atalho para abrir a playlist pública no aplicativo Spotify, se estiver instalado.
- Abertura da playlist direcionada ao app Spotify; o Erom não abre um navegador como alternativa.

## Limites atuais

A interface e navegação são nativas, mas a pesquisa ainda não consulta um catálogo de músicas. A reprodução é encaminhada ao Spotify instalado. Reprodução dentro do próprio Erom, login Spotify e catálogo sincronizado exigem uma integração oficial autorizada e credenciais de desenvolvedor; não estão simulados como se já funcionassem.
