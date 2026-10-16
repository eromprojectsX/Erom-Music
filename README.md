# Erom Música — aplicativo offline

Este repositório contém somente o projeto Android nativo do Erom Música.

## Funcionamento
- Importa arquivos de áudio escolhidos pelo usuário.
- Copia os arquivos para o armazenamento privado do aplicativo.
- Pesquisa e reproduz músicas locais sem depender de internet.
- Não usa servidor próprio, banco de dados remoto, Supabase, Firebase, Cloudflare R2 ou APIs de catálogo.
- Não varre a pasta do WhatsApp.

## Compilar o APK
O fluxo `Build Erom Música Native APK` em GitHub Actions compila o app. O GitHub é usado somente para armazenar o código-fonte e executar a compilação; não serve músicas ao aplicativo.

Requisitos locais: JDK 17, Android SDK 35 e Gradle 8.9.
