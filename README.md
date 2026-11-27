# Flow Launcher — v0.1.0

Launcher Android independente, inspirado na organização e na navegação do BlackBerry 10. Não é uma ROM, não modifica o kernel ou SystemUI e não requer root. Identidade gráfica original, sem arquivos gráficos proprietários do BlackBerry.

## O que existe nesta versão

- **Hub** (à esquerda): notificações ativas de outros aplicativos, mediante autorização explícita; toque para executar a ação da notificação e toque longo para solicitar sua remoção.
- **Início / Active Frames**: até 12 aplicativos abertos pelo Flow, com ícones reais e reabertura por toque. O botão × remove o atalho, não encerra o processo. Não há miniaturas do sistema, processos ativos ou lista global de recentes.
- **Aplicativos** (à direita): grade responsiva com ícones, nomes, busca e acesso à ficha do aplicativo por toque longo. A grade tem rolagem vertical; os três painéis são navegáveis por deslize horizontal e pela barra inferior.
- Tema escuro, menu de configuração e solicitação da função HOME.
- Android 8.0 (API 26) ou superior; alvo API 35. Não há bibliotecas externas de execução.
- Sem permissão INTERNET. Somente o histórico de componentes abertos é persistido em preferências locais. Notificações ficam em memória, sem banco de dados, sem sincronização e sem acesso direto a contas. O Android pode ocultar campos sensíveis das notificações.

## Gerar o APK — GitHub

Abra [Actions](https://github.com/bbvizzotto-run/flow-laubcher/actions), escolha **Build Flow Launcher APK**. O push em main/master dispara o build; também é possível clicar em Run workflow. Abra a execução e, em Artifacts, baixe `FlowLauncher-v0.1-debug`. Extraia `app-debug.apk` do ZIP. O workflow também executa lint após publicar o APK.

O APK de debug é assinado automaticamente para testes. Em builds feitos em máquinas diferentes, a chave de debug pode mudar. Se houver conflito de assinatura, desinstale a versão anterior (isso apaga as preferências do Flow) ou configure uma chave estável própria antes de distribuir atualizações.

## Gerar localmente

Requisitos: JDK 17, Gradle 8.9, Android SDK com plataforma 35 e Build Tools 35.0.0. Configure o SDK no Android Studio ou por ANDROID_HOME. Este pacote não contém o Gradle Wrapper binário. Com Gradle 8.9 instalado, na raiz:

```bash
gradle :app:assembleDebug
gradle :app:lintDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Se desejar usar o Android Studio com Gradle Wrapper, gere-o primeiro com `gradle wrapper --gradle-version 8.9`, depois abra a pasta do projeto no Android Studio. Configure o Gradle JDK 17 e instale a plataforma 35 pelo SDK Manager.

## Instalar e testar

1. Copie o APK para o Motorola ou Samsung e abra pelo gerenciador de arquivos. Autorize a instalação dessa fonte se solicitado. Alternativamente, com depuração USB autorizada: `adb install -r app-debug.apk`.
2. Abra Flow Launcher normalmente. A instalação, por si só, não substitui a tela inicial existente.
3. Vá ao menu ⋮ e selecione **Definir launcher padrão**. Também é possível usar Configurações > Aplicativos > Aplicativos padrão > Aplicativo de início (o nome varia).
4. Deslize à esquerda a partir de Início, abra dois ou três apps, pressione Home e observe os cartões. Toque em × para remover um cartão.
5. Abra Hub e autorize acesso às notificações. Se Android bloquear a configuração por ser um APK externo, revise a tela de informações do aplicativo e as configurações restritas somente se você confiar no APK que compilou. O acesso é opcional e revogável.
6. Teste a grade, a busca e os três painéis no modo retrato e paisagem. Verifique também as notificações e o botão voltar.
7. Para retornar à interface original, selecione novamente seu launcher anterior em Aplicativos padrão. Depois desinstale Flow Launcher se desejar.

Não é necessário alterar o bootloader, desbloquear OEM ou instalar qualquer imagem de firmware.

## Estado da validação

Este é um protótipo inicial de código-fonte. A sintaxe Java e os XML foram verificados no ambiente de entrega; não havia SDK Android nem acesso aos repositórios de dependências para compilar, executar o lint Android ou testar no emulador/dispositivo. A compatibilidade com o Motorola xpeng e Samsung SM-X236B requer os testes acima. Não representa uma implementação de Quickstep, um BlackBerry Hub completo ou um gerenciador global de janelas.

## Próximos passos

Após os testes: ajustar espaçamentos, animações, identidade dos cartões e gestos; adicionar favoritos, papéis de parede e filtros no Hub. A integração profunda de recentes/gestos globais dependeria de permissões privilegiadas e/ou ROM personalizada, fora deste APK.
