<p align="center">
  <img src="android/app/src/main/res/drawable-xhdpi/banner.png" alt="Hora do Desenho" width="320">
</p>

<h1 align="center">Hora do Desenho</h1>

<p align="center"><strong>Canais do YouTube com hora marcada — do jeito que a TV era quando a gente era criança. <em>(antes chamada Grade TV)</em></strong></p>

<p align="center">
  Gratuito · Código aberto · Sem cadastro · Sem anúncios próprios · Feito para pais e mães
</p>

<p align="center">
  Um produto <strong>VSA Tecnologia</strong> para a comunidade — de Dourados, MS, para todas as famílias.
</p>

---

## Lembra de esperar o desenho de sábado?

A gente lembra. O programa tinha hora, o dia tinha ritmo, e quando acabava… acabava. Hoje o YouTube entrega tudo, o tempo todo, com rolagem infinita e um "próximo vídeo" que nunca termina.

A **Hora do Desenho** devolve a hora marcada. Você escolhe os canais que confia, monta uma grade de programação (quais dias, que horas) e pronto: quando a criança "liga a TV", só aparece o que está **no ar agora**. O canal preferido fora do horário? Está **dormindo**, com uma contagem regressiva mostrando quando acorda. Sem briga, sem negociação — a TV tem hora, e é a TV que diz.

No modo **linear**, o vídeo já está em andamento quando a criança sintoniza, como um canal de verdade: sem pausar, sem adiantar, sem escolher. Quando a faixa acaba, acabou. Amanhã tem mais.

## O que a Hora do Desenho faz

| Para a criança | Para os pais |
|---|---|
| Interface clara, redonda e colorida, com bolhas dos canais | Área protegida por **PIN** (engrenagem no canto, 4 números) |
| **No ar agora**, **Daqui a pouco** e **Dormindo** — ela entende o que pode ver e quando | **Perfis** por criança: avatar, canais liberados, limite por dia, favoritos próprios |
| Canal fora do horário = TV dormindo com contagem regressiva, e liga sozinha na hora | **Limite diário** de tempo; estourou, aparece "Hora de descansar" (+15 min só com PIN) |
| Modo **linear**: o vídeo está passando, como na TV | Modo **livre** para faixas em que ela pode escolher o vídeo |
| **Favoritos** com o coração, que só tocam quando o canal está no ar | **Guia de programação** da semana, com a linha do "agora" |
| Funciona com controle remoto: setas, OK, números 1–9, Voltar | Canais sugeridos verificados, adicionados com um toque |
| Nada de Shorts, lives nem "próximo vídeo" automático | Exportar/importar a configuração em JSON |
| **Hora de dormir**: nos horários de bloqueio a TV inteira dorme, e quem aparece é o detetive dormindo, em três cenas (noite, fim de tarde, manhã cedo) | **Horários de bloqueio** por dia da semana, com cena automática pela hora; os pais liberam com o PIN |

Tudo roda com o player oficial do YouTube e a API oficial do YouTube. A Hora do Desenho não hospeda, não baixa e não reencoda nada: ela apenas decide **quando** cada canal pode aparecer.

## Como instalar

### Na TV (Android TV, Google TV, Fire TV, TV Box)

1. Baixe o `hora-do-desenho.apk` na página de [**Releases**](../../releases) (ou no artefato da última build em [**Actions**](../../actions)).
2. Instale na TV como qualquer APK (Downloader, envio por rede local, pendrive ou `adb install hora-do-desenho.apk`). Ative "Instalar de fontes desconhecidas" se a TV pedir.
3. Abra a Hora do Desenho. No primeiro acesso ela pede o **PIN dos pais** e cria o **perfil da criança**.
4. Na área dos pais, cole a chave da YouTube Data API (passo a passo logo abaixo) e adicione canais e horários.

**Para ser a tela inicial da TV** (a criança liga a TV e já cai na Hora do Desenho):

- **Android TV** (Sony, TCL, Philips, Mi Box, TV Box em geral): aperte **HOME**; o sistema pergunta qual tela inicial usar. Escolha Hora do Desenho e marque "Sempre".
- **Google TV** (Chromecast, TVs novas da Sony/TCL/Hisense) e **Fire TV**: o sistema não deixa trocar a tela inicial. A Hora do Desenho então abre sozinha quando a TV termina de ligar e fica fixada entre os apps. Em alguns modelos é preciso autorizar o app a "iniciar automaticamente".
- Para voltar ao launcher original a qualquer momento: Configurações → Apps → Hora do Desenho → "Limpar padrões".

O botão **Voltar** do controle nunca fecha o app (ele volta de tela). O botão **Menu** abre a área dos pais.

### No celular ou tablet Android

O mesmo `hora-do-desenho.apk` instala no celular. Ali ele se comporta como um app comum (não vira a tela inicial do telefone), funciona em retrato e gira para paisagem sozinho quando um vídeo abre. Com o **modo criança** (ligado por padrão), o app fica fixado: Home e apps recentes não funcionam e só se sai pela área dos pais, com o PIN — na primeira vez o Android pergunta "Fixar este app?", confirme. Serve para a criança assistir no celular dos pais com a mesma grade, ou para o responsável configurar canais e horários com mais conforto e depois exportar/importar a configuração para a TV.

### No navegador (PC, Chromebox, tablet ou celular ligado na TV)

Abra o `index.html` direto no Chrome, ou use a versão hospedada (GitHub Pages). Pressione `F` para tela cheia. Teclas: setas, Enter, 1–9, `G` (guia), `A` (pais), `Esc` (voltar), `C` (favoritar), `L` (lista no modo livre).

## A chave da YouTube Data API (5 minutos, gratuita)

A Hora do Desenho precisa de uma chave para listar os vídeos dos canais. Ela é sua, fica salva só no seu aparelho, e a cota gratuita (10.000 unidades por dia) dá para dezenas de canais.

1. Entre no [Google Cloud](https://console.cloud.google.com/) com sua conta Google e crie um projeto (qualquer nome, ex. `grade-tv`).
2. [Ative a YouTube Data API v3](https://console.cloud.google.com/apis/library/youtube.googleapis.com) nesse projeto.
3. Em [Credenciais](https://console.cloud.google.com/apis/credentials), clique em **Criar credenciais → Chave de API**.
4. Copie a chave (começa com `AIza`), cole na área dos pais e clique em **Testar**.
5. Recomendado: em "Restringir chave", limite à YouTube Data API v3.

Guia oficial do Google: [Obtaining authorization credentials](https://developers.google.com/youtube/registering_an_application).

## Canais para começar (verificados)

| Canal | Para quem | Dica de grade |
|---|---|---|
| [Detetive Labrador](https://www.youtube.com/@SheriffLabradorPT) | 3 a 7 anos | Episódios de ~30 min: faixas de 30 ou 60 min |
| [As Histórias do Om Nom](https://www.youtube.com/@OmNomPT) | 3 a 7 anos | Compilações de ~20 min |
| [Mundo Bita](https://www.youtube.com/@MundoBita) | 2 a 5 anos | Música brasileira; ótimo para o fim de tarde |
| [Bluey Português Brasil](https://www.youtube.com/@blueyportuguesbrasil-canal) | 3 a 6 anos | Blocos de 40 min a 1 h |
| [Galinha Pintadinha](https://www.youtube.com/@GalinhaPintadinha) | 1 a 4 anos | Clipes curtos: desative o filtro de 2 min |
| [Pokémon Kids TV](https://www.youtube.com/@pokemonkidstv) | 3 a 6 anos | Músicas e rimas, quase sem fala |

Todos estão na lista de **canais sugeridos** dentro do app: um toque e pronto.

## Privacidade, de verdade

- Perfis, grade, preferências e chave da API são armazenados localmente; não existe servidor próprio, conta, cadastro ou rastreamento da Hora do Desenho. As consultas de canais e a reprodução de vídeos se comunicam com os serviços do Google/YouTube e seguem os termos e políticas desses serviços.
- A chave da API e as configurações ficam no armazenamento local do navegador ou do app.
- O YouTube continua sendo o YouTube: anúncios e restrições de incorporação dos canais valem normalmente (uma conta Premium logada no aparelho remove os anúncios).

## Como funciona por dentro

- **Um único `index.html`** com toda a interface e a lógica (HTML, CSS e JavaScript puros, sem framework). Roda em qualquer navegador moderno.
- **App Android** (`android/`): um WebView que serve esse mesmo `index.html` como origem `https://` (exigência do player do YouTube), registra-se como tela inicial (`HOME` + `LEANBACK_LAUNCHER`) e abre sozinho no boot.
- **YouTube Data API v3** lista os vídeos e as durações; **YouTube IFrame Player API** toca. A programação linear calcula qual vídeo está "no ar" pelo horário de início da faixa e pelas durações, em loop (reprise) quando a fila acaba.
- O APK é gerado automaticamente pelo GitHub Actions a cada push; tags `v*` publicam uma Release.

## Roteiro

- [x] Fase 1 — MVP no navegador, grade de horários, modo linear e livre
- [x] Fase 1.5 — interface lúdica, perfis, PIN, limite diário, favoritos
- [x] Fase 2 — app Android TV como tela inicial, APK automático; celular em retrato
- [x] Fase 2.5 — tela de bloqueio com o detetive dormindo e horários de bloqueio
- [ ] Fase 3 — sincronização entre aparelhos (configurar no celular, assistir na TV)
- [ ] Fase 4 — "boletim" de vídeos novos, bloqueio por palavras-chave, relatórios de uso para os pais

## VSA Crescer+ Digital — plataforma familiar

A Hora do Desenho é o primeiro hub da metodologia **VSA Crescer+ Digital**. Na versão Android, a área dos pais inclui uma primeira versão da seleção de aplicativos instalados por perfil infantil; a tela infantil apresenta apenas os apps selecionados naquele perfil.

- [Arquitetura do agregador e plano de implementação](docs/VSA_CRESCER_DIGITAL_APP_HUB.md)
- [Configuração do modo quiosque gerenciado](docs/ANDROID_KIOSK_SETUP.md)
- [Repositório Tomb Mask Kids](https://github.com/viniciusandradde/tomb-of-the-mask) — jogo VSA independente, com desenvolvimento pausado enquanto concluímos este hub.
- GCompris e outros apps Android podem ser selecionados se estiverem instalados e expuserem uma atividade de inicialização.

**Estado:** a interface e a ponte nativa foram adicionadas nesta branch, mas ainda precisam passar pelo build e por testes em dispositivo real. O modo de fixação comum não mantém necessariamente a criança dentro de vários apps. O bloqueio gerenciado exige provisionamento como Device Owner em dispositivo dedicado e validação de compatibilidade. Não use este protótipo como garantia de bloqueio absoluto.

---

## Tempo em Família (app irmão, para os pais)

Em `apps/tempo-em-familia/` há um segundo app, bem pequeno, para o celular **dos pais**: nos horários marcados ele trava o telefone numa tela com as fotos da família (carrossel) e só libera com senha. Serve para o jantar, a hora de dormir das crianças ou qualquer momento em que o celular deva ficar de lado. As fotos ficam apenas no aparelho. APK na página de Releases (`tempo-em-familia.apk`).

## Quem faz

A Hora do Desenho é um projeto da **[VSA Tecnologia](https://github.com/viniciusandradde)**, empresa de tecnologia de Dourados (MS) que desenvolve automação, inteligência artificial e integração de sistemas — principalmente para hospitais. A Hora do Desenho nasceu em casa, da vontade de devolver às crianças a TV com hora marcada, e é oferecida à comunidade de graça, com código aberto e suporte pelas issues do repositório. As configurações são locais; consultas e reprodução usam serviços do Google/YouTube.

## Contribuir

Achou um bug, quer sugerir um canal ou uma função? Abra uma [issue](../../issues). Pull requests são bem-vindos — o projeto é propositalmente simples para qualquer pessoa conseguir mexer.

## Licença

MIT, © VSA Tecnologia. Use, copie, adapte e compartilhe com outros pais.

<p align="center"><sub>Hora do Desenho não é afiliada ao YouTube ou ao Google. YouTube é marca do Google LLC. Os canais pertencem aos seus criadores.</sub></p>
