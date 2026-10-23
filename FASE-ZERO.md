# Hora do Desenho — Fase zero (teste com parceiros)

Antes da Play Store, a Hora do Desenho está sendo testada por algumas famílias parceiras, instalando o APK direto pelo link. Esta versão já vem com a chave do YouTube: **o parceiro não precisa criar nada no Google**.

**Link de download (sempre a versão mais recente):**
https://github.com/viniciusandradde/grade-tv/releases/latest/download/hora-do-desenho.apk

---

## Para o parceiro

### 1. Instalar (2 minutos)

1. No celular Android, abra o link acima no Chrome. O arquivo `hora-do-desenho.apk` é baixado.
2. Toque no arquivo baixado (na notificação ou em *Downloads*).
3. Se aparecer **"Por segurança, seu telefone não pode instalar apps desconhecidos desta fonte"**: toque em **Configurações**, ligue **Permitir desta fonte** e volte.
4. Toque em **Instalar**. Se o Play Protect avisar que o app é desconhecido, toque em **Mais detalhes → Instalar mesmo assim** — é normal enquanto o app não está na Play Store.
5. Abra a **Hora do Desenho**.

**Na TV (Android TV, Google TV, TV Box):** instale o app *Downloader* pela loja da TV, digite o mesmo link e siga a instalação. O resto é igual.

### 2. Passo a passo de configuração (aparece sozinho na primeira vez)

O app guia cada etapa e salva na hora — se fechar no meio, ele volta de onde parou.

| Passo | O que fazer |
|---|---|
| 1. PIN dos pais | Crie 4 números. Eles protegem as configurações e liberam o app quando estiver bloqueado. |
| 2. Quem vai assistir | Nome da criança, avatar e quantos minutos de TV por dia (0 = sem limite). |
| 3. Canais | Toque nos canais que a criança pode ver (Detetive Labrador, Bluey, Mundo Bita…). Dá para colar o link de outro canal. |
| 4. Quando a TV pode ficar ligada | Marque os horários (ex.: fim de tarde, 17:00 às 19:00) e os dias. Fora deles, os canais ficam "dormindo". |
| 5. Hora de dormir | Ex.: 20:30 às 07:00. Nesse horário aparece o detetive dormindo. Escolha a cena **Escuro** se a criança dorme com o celular por perto: a tela fica preta, só com o contorno das letras, e o brilho vai ao mínimo. |
| 6. Este aparelho | **Celular:** deixe o *Modo criança* ligado — o app fica fixado e a criança não sai dele. Na primeira vez o Android pergunta **"Fixar este app?"**: confirme. **TV:** deixe marcado "abrir quando a TV ligar" e, no fim, aperte HOME no controle e escolha *Hora do Desenho → Sempre*. |
| 7. Pronto | Confira o resumo e toque em **Ligar a TV**. |

Para mudar qualquer coisa depois: toque na engrenagem ⚙️ no canto e digite o PIN. Lá também tem **Refazer o passo a passo**.

### 3. Atualizar

Quando sair versão nova, a área dos pais (⚙️) mostra o aviso **"Nova versão disponível"**. Baixe pelo mesmo link e instale **por cima** — canais, horários e PIN continuam.

### 4. Sair do app ou desinstalar

- Com o modo criança ligado, o app só sai pela área dos pais: ⚙️ → PIN. Ali o app é desfixado.
- Para desinstalar: Configurações do Android → Apps → Hora do Desenho → Desinstalar.

### 5. O que testar e como avisar

Use do jeito normal da família por alguns dias e repare:

- Os canais entram no ar e saem na hora certa? O vídeo "já está passando" quando a criança liga?
- A hora de dormir aparece no horário? A cena Escuro fica escura o suficiente no quarto?
- O limite diário funciona? O PIN libera mais 15 minutos?
- No celular: a criança consegue sair do app sozinha?
- Algum canal ou vídeo não tocou?

Mande para o Vinícius (WhatsApp) ou abra uma [issue no GitHub](https://github.com/viniciusandradde/grade-tv/issues) com: **print da tela, horário, modelo do aparelho e o que aconteceu**. A versão aparece em ⚙️ → *Versão e passo a passo*.

---

## Para quem publica (mantenedor)

### Uma vez só

1. **Projeto no Google Cloud só para o app**, com **faturamento desativado**. Ative a *YouTube Data API v3*, crie uma *Chave de API* e, em "Restringir chave", limite a chave à **YouTube Data API v3**. Sem faturamento e com essa restrição, um vazamento da chave nunca vira cobrança — no pior caso, a cota do dia acaba.
2. **Secrets do repositório** (GitHub → *Settings → Secrets and variables → Actions → New repository secret*):
   - `YOUTUBE_API_KEY` — a chave `AIza…` do passo anterior.
   - `ANDROID_KEYSTORE_BASE64` — o keystore de teste em base64 (uma linha).
   - `ANDROID_KEYSTORE_PASSWORD` — a senha do keystore.
3. **Guarde o arquivo do keystore** em lugar seguro. Ele é a "assinatura" do app: é o que permite instalar versões novas por cima. Se perder, todos os parceiros precisam desinstalar e instalar de novo, e ele também será necessário para registrar o app na verificação de desenvolvedor do Android (abaixo).

Sem os secrets o build continua funcionando como antes (pede a chave no passo a passo e assina com chave aleatória); o GitHub Actions mostra um aviso.

### A cada versão nova

1. Suba `versionCode` e `versionName` em `android/app/build.gradle.kts`.
2. *Actions → Android APK → Run workflow*, preencha `release_tag` (ex.: `v0.6.1`). O link de download passa a entregar a versão nova automaticamente e os parceiros veem o aviso na área dos pais.

### Limites desta fase

- **Cota do YouTube:** 10.000 unidades por dia, **gratuitas e compartilhadas por todos os aparelhos** (não existe plano pago; aumento é pedido por formulário de auditoria). Cada canal gasta até ~16 unidades por aparelho por dia, então dá folga para dezenas de famílias. Se acabar, os vídeos já carregados continuam passando e o app avisa; a cota volta à meia-noite do horário do Pacífico.
- **A chave pode ser extraída do APK.** Por isso o projeto não tem faturamento e a chave é restrita ao YouTube.
- **Verificação de desenvolvedor do Android:** desde 30/09/2026, no Brasil, o Google exige desenvolvedor verificado só para apps instalados por **lojas participantes**; APK instalado direto pelo link ainda não é afetado. A exigência deve se estender a todos os apps em 2027 — antes disso, registre o pacote `br.com.vsa.horadodesenho` com o certificado do keystore de teste no *Android Developer Console* (há conta gratuita de distribuição limitada, até 20 aparelhos) ou pela Play Console.
