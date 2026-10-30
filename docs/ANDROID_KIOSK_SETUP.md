# Configuração do modo quiosque gerenciado — VSA Crescer+ Digital

## Objetivo

Permitir que a Hora do Desenho e os aplicativos explicitamente autorizados funcionem em um dispositivo Android dedicado, mantendo o usuário dentro da experiência infantil.

**Não use este procedimento no celular pessoal de uso diário.** O provisionamento como Device Owner normalmente exige um aparelho novo ou restaurado de fábrica, sem contas adicionadas. A configuração pode alterar significativamente o controle do dispositivo.

## Requisitos

- Dispositivo Android dedicado para a criança, compatível com Android Device Policy / Device Owner.
- Hora do Desenho instalada como APK.
- Android Platform Tools (adb) instalado no computador.
- Teste inicial com dados não importantes e plano para restaurar o dispositivo.
- Os aplicativos liberados instalados e testados individualmente.

## Provisionamento de teste

1. Faça backup do que for necessário e restaure o dispositivo de teste para o padrão de fábrica.
2. Conclua apenas as etapas mínimas de configuração exigidas pelo dispositivo; não adicione uma conta pessoal antes de provisionar.
3. Ative a depuração USB e conecte o aparelho ao computador.
4. Instale a versão de teste da Hora do Desenho:

       adb install hora-do-desenho.apk

5. Verifique se o pacote foi instalado:

       adb shell pm list packages br.com.vsa.horadodesenho

6. Configure a Hora do Desenho como Device Owner:

       adb shell dpm set-device-owner br.com.vsa.horadodesenho/.VsaDeviceAdminReceiver

7. Reabra o aplicativo, entre na área dos pais com o PIN e abra **VSA Crescer+ Digital · Aplicativos permitidos**.
8. Selecione os apps necessários para um perfil e salve.
9. Teste o fluxo de ida e volta entre a Hora do Desenho e cada app permitido. Confirme que apps não selecionados não podem ser abertos pelo hub.
10. Teste reinicialização, perda de rede, troca de perfil, horários de bloqueio, limites diários, acessibilidade e saída autorizada.

O comando pode falhar se o aparelho já estiver provisionado, tiver contas adicionadas, usar uma versão/fabricante incompatível ou aplicar restrições próprias. Siga a documentação do fabricante e a documentação oficial Android.

## Recuperação / remoção

Remover Device Owner não é equivalente a desinstalar um app comum e pode exigir uma rotina de desprovisionamento específica ou restauração de fábrica. Planeje como recuperar o aparelho antes de ativar o modo gerenciado. Não execute comandos de remoção em um dispositivo de produção sem compreender o impacto.

## Limites atuais do projeto

- O catálogo de apps e a lista de permitidos são uma primeira implementação e precisam de build e teste físico.
- A seleção por perfil é aplicada na interface; o endurecimento de todas as fronteiras de segurança e a validação por perfil ainda exigem revisão.
- A compatibilidade de cada app externo varia. Conseguir iniciá-lo não significa que o Hora do Desenho consiga limitar seu uso interno.
- A visibilidade de pacotes é declarada para atividades com categoria LAUNCHER, evitando solicitar acesso indiscriminado a todos os pacotes. Revise as políticas de distribuição e valide em cada versão/fabricante.
- Não prometa bloqueio total antes de validar o aparelho e a versão do Android.

## Referências oficiais

- Android dedicated devices / Lock Task Mode: https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- Android package visibility: https://developer.android.com/training/package-visibility
- Device administration: https://developer.android.com/guide/topics/admin/device-admin
