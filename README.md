# Grade TV

Canais do YouTube com horário marcado, como a TV de antigamente.

Você cadastra canais e define uma grade (dias e faixas de horário). Ao "ligar a TV", só aparece o que está no ar naquele momento. Fora da faixa, o canal mostra chuvisco e a contagem regressiva para o próximo horário. No modo **linear**, o vídeo já está em andamento quando você sintoniza — sem pausar, sem adiantar, sem escolher — exatamente como era esperar o programa de sábado.

## Fase 1 — MVP (este repositório)

Um único `index.html`, sem backend. Tudo fica no `localStorage` do navegador.

- Cadastro de canais por link (`youtube.com/@canal`), ID (`UC…`) ou playlist
- Grade de horários por canal: dias da semana, início, fim e modo (linear ou livre)
- Tela de canais com o que está **no ar agora** e quando cada canal **volta**
- Tela **fora do ar** com chuvisco e contagem regressiva (sintoniza sozinho quando o horário chega)
- Modo **linear**: calcula qual vídeo está passando e em que segundo, bloqueia pausa/avanço, segue a fila e marca os vídeos assistidos (sem repetir até esgotar a fila)
- Modo **livre**: dentro do horário você escolhe o vídeo e usa os controles normais
- Guia de programação (EPG) por dia, com a linha do "agora"
- Navegação por teclado / controle remoto: setas, Enter, 1–9, G (guia), A (ajustes), Esc, F (tela cheia)
- Exportar / importar a configuração em JSON

## Como usar

1. Crie uma chave gratuita da **YouTube Data API v3**: crie um projeto no Google Cloud, [ative a API](https://console.cloud.google.com/apis/library/youtube.googleapis.com) e gere a chave na [página de credenciais](https://console.cloud.google.com/apis/credentials) (Criar credenciais → Chave de API). Guia oficial: [Obtaining authorization credentials](https://developers.google.com/youtube/registering_an_application). Restrinja a chave à API do YouTube e ao domínio onde a página vai rodar.
2. Abra o `index.html` (localmente ou pelo GitHub Pages), vá em **Ajustes** (tecla `A`), cole a chave e clique em **Testar**.
3. Adicione os canais e as faixas de horário.
4. Volte para a tela de canais e sintonize.

Para usar na TV: abra a página no navegador de um mini PC, Chromebox, Fire TV (Silk) ou Smart TV ligado por HDMI, pressione `F` para tela cheia e navegue com o teclado ou controle remoto.

## Limitações conhecidas

- Os vídeos tocam pelo player oficial do YouTube: anúncios e restrições de incorporação continuam valendo (canais com embed desativado não tocam).
- O bloqueio de pausa/avanço é uma convenção de interface, não uma trava técnica.
- A cota gratuita da Data API é de 10.000 unidades/dia; cada atualização de canal gasta cerca de 3 a 5 unidades (cache de 6 h).
- Vídeos ao vivo e Shorts (menos de 2 min, configurável) são ignorados.

## Roteiro

- **Fase 2** — backend (FastAPI + SQLite), admin pelo celular, sincronização entre dispositivos, histórico.
- **Fase 3** — app Android TV, perfis (família), bloqueio parental por horário, "boletim" de vídeos novos.

## Licença

MIT
