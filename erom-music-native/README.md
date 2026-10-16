# Erom Música — Android nativo offline

Aplicativo Android em Kotlin e Jetpack Compose. O app importa arquivos de áudio escolhidos pelo usuário, copia-os para a pasta privada `Music/Erom Music` do aplicativo e os reproduz localmente.

## Sem serviços remotos
- Nenhum servidor de músicas ou API de catálogo.
- Nenhum banco de dados remoto ou serviço de autenticação.
- Nenhuma conexão necessária para importar e reproduzir arquivos locais.
- Não faz varredura de pastas do dispositivo; o usuário escolhe os arquivos no seletor Android.

## Compilar
Requisitos: JDK 17, Android SDK 35 e Gradle 8.9.

```bash
gradle assembleDebug
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/app-debug.apk`.
