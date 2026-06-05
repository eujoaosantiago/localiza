# Localiza

Aplicativo Android desenvolvido como trabalho para a UNIFOR.

## Visão Geral

`Localiza` é um app que auxilia entrega e roteirização de endereços no Brasil. O usuário autentica com Google, registra endereços e locais associados, organiza rotas automaticamente com base na localização atual e compartilha destinos pelo Google Maps.

## Principais Funcionalidades

- Autenticação com Google Sign-In
- Armazenamento de dados de usuário e endereços no Firebase Firestore
- Busca de endereço por CEP usando a API BrasilAPI
- Geolocalização para preencher endereço atual e otimizar rotas
- Reorganização de endereços com algoritmo de vizinho mais próximo
- Abertura de rotas no Google Maps
- Compartilhamento de endereço via intent de compartilhamento
- Cadastro de locais (places) associados a um endereço
- Interface adaptada para dispositivos Android modernos com ViewBinding

## Estrutura do Projeto

- `app/` - módulo Android principal
  - `src/main/java/com/localiza/uniforads/` - activities e serviço Firebase
  - `src/main/java/com/localiza/uniforads/adapter/` - adapters RecyclerView
  - `src/main/java/com/localiza/uniforads/model/` - modelos de dados
  - `src/main/java/com/localiza/uniforads/network/` - cliente Retrofit e serviço BrasilAPI
  - `src/main/res/layout/` - layouts XML das telas
  - `src/main/res/values/strings.xml` - textos e labels do app
  - `app/src/main/AndroidManifest.xml` - permissões e activities

## Telas e Fluxo de Uso

1. **Tela de Login**
   - Autenticação via Google
   - Solicita permissão de notificações no Android 13+

2. **Lista de Endereços**
   - Exibe endereços cadastrados pelo usuário
   - Permite busca por texto
   - Botões para adicionar endereço, organizar rota e iniciar entregas
   - Suporta arrastar e soltar para reordenar a lista

3. **Cadastro de Endereço**
   - Preenchimento manual de CEP, rua, bairro, cidade e estado
   - Consulta automática de CEP via BrasilAPI
   - Captura de localização atual para preencher dados e coordenadas

4. **Detalhe do Endereço**
   - Exibe informações completas do endereço
   - Lista locais (places) associados ao endereço
   - Permite cadastro de novos locais
   - Exibe perfil do usuário e opção de logout

5. **Compartilhar Localização**
   - Permite abrir o endereço no Google Maps
   - Gera navegação ou busca no Maps
   - Compartilha o endereço via outras apps

## Tecnologias Utilizadas

- Kotlin
- Android SDK 35
- ViewBinding
- Firebase Auth
- Firebase Firestore
- Firebase Cloud Messaging
- Google Sign-In
- Google Location Services
- Retrofit + Gson
- Coil
- BrasilAPI (`https://brasilapi.com.br/api/cep/v2/{cep}`)

## Requisitos de Desenvolvimento

- Android Studio
- JDK 11
- Gradle Wrapper (incluído no repositório)
- Emulador ou dispositivo Android com Google Play Services
- Conexão com internet para APIs e Firebase

## Como Executar

1. Abra o projeto no Android Studio.
2. Sincronize o Gradle.
3. Conecte um dispositivo ou crie um emulador.
4. Execute `app` no Android Studio.

Ou via terminal:

```bash
cd c:\Projetos\Localiza-master
gradlew.bat assembleDebug
```

## Configuração Firebase

O projeto depende de Firebase para autenticação e banco de dados.

- Confirme que o arquivo `google-services.json` está configurado no módulo `app/`.
- Verifique se o projeto Firebase tem os serviços:
  - Authentication (Google Sign-In)
  - Firestore Database
  - Cloud Messaging

## Observações

- O app usa permissão de localização para otimizar rotas e preencher o endereço atual.
- A permissão `POST_NOTIFICATIONS` é solicitada apenas em Android 13+.
- O ícone do app está configurado no `AndroidManifest.xml` como `@drawable/brazukaxi`.

## Pontos de Melhoria

- Adicionar tratamento de erros mais robusto para falha de rede e CEP inválido
- Implementar ordenação fixa de rotas com ponto de origem e retorno
- Adicionar testes automatizados para atividades e lógica de rota
- Sincronizar locais (places) diretamente ao salvar endereço

## Autoria

Trabalho acadêmico para a UNIFOR.

### Contato

- Projeto criado por aluno do curso de Análise e Desenvolvimento de Sistemas
- Tecnologias escolhidas: Kotlin, Firebase e APIs REST
