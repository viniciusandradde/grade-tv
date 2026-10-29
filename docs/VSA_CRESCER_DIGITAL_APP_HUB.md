# VSA Crescer+ Digital — Plataforma familiar e agregador de aplicativos

**Slogan:** Tecnologia com limites. Infância com mais vida.

## Estado da implementação nesta branch

Já foram adicionados ao protótipo Android a ponte nativa para listar apps com tela inicial, a lista nativa de pacotes permitidos, uma primeira interface parental de seleção por perfil e uma tela infantil para abrir apps liberados. A branch também declara o receiver de administração do dispositivo e inclui instruções de provisionamento. Os métodos nativos de controle do launcher, modo criança, dados persistidos e orientação da tela agora verificam a origem confiável da WebView (commit `4ab2c9c0d9551b2eaff5315d98190c69c15a7160`). **A nova correção ainda precisa de CI e teste em aparelho físico**; a segurança por perfil e o comportamento real do modo quiosque precisam ser validados antes de uso como bloqueio parental forte.

## Decisão de produto

A VSA Crescer+ Digital será a camada familiar comum para uma coleção de aplicativos infantis mantidos em repositórios independentes. O Hora do Desenho passa a ser o primeiro produto hospedeiro e a interface principal no Android; os demais produtos serão integrados progressivamente, sem fundir seus repositórios ou duplicar regras parentais.

### Repositórios e responsabilidades

- **grade-tv / Hora do Desenho:** experiência infantil, grade do YouTube, bloqueios de horário e catálogo de aplicativos autorizados.
- **tomb-of-the-mask / Tomb Mask Kids:** jogo independente. Desenvolvimento pausado enquanto o app hub e o Hora do Desenho são concluídos.
- **Kiddo Wise Routines:** produto futuro de rotinas familiares, sujeito a avaliação própria.
- **Aplicativos externos:** GCompris e outros apps Android que os responsáveis instalarem e autorizarem explicitamente.

O catálogo compartilhado deve ter um contrato versionado e documentado. Cada repositório continua com seu próprio ciclo de versão e CI; a integração não deve depender de copiar o código de um app para outro.

## Experiência pretendida

### Área dos responsáveis

1. Acessar com PIN, sem exibir ferramentas administrativas à criança.
2. Abrir **Aplicativos permitidos**.
3. Ver apps integrados pela VSA e apps Android compatíveis instalados no dispositivo.
4. Ativar/desativar cada app individualmente e ordenar a lista.
5. Para cada app, visualizar nome, ícone, pacote Android, origem, versão detectada e status de compatibilidade.
6. Salvar e testar a experiência infantil; oferecer retorno claro se um app não puder ser iniciado ou bloqueado.
7. Opcionalmente configurar horários e limites por aplicativo, sem impedir necessidades fisiológicas, descanso essencial ou pedidos de ajuda.

### Área infantil

- Exibir somente os apps autorizados pelo responsável, com cartões grandes e linguagem infantil.
- Incluir Hora do Desenho como experiência principal e mostrar os outros apps liberados, como Tomb Mask Kids ou GCompris, quando instalados e compatíveis.
- Não exibir configurações do Android, lista irrestrita de aplicativos, loja ou controles administrativos.
- Oferecer saída protegida para o responsável e um caminho de emergência acessível.
- Não esconder mensagens de segurança, chamadas de emergência ou recursos de acessibilidade do sistema.

## Arquitetura recomendada

### 1. Catálogo declarativo compartilhado

Manter um catálogo versionado no repositório Hora do Desenho, por exemplo `config/app-catalog.json`. Cada item deve conter:

- `id`: identificador estável da VSA;
- `name`, `description`, `icon`;
- `kind`: `vsa` ou `external`;
- `androidPackage`: pacote Android esperado, quando aplicável;
- `launchMode`: integração nativa, Activity Android ou URL aprovada;
- `minimumVersion` / `compatibilityNotes`;
- `defaultEnabled`: sempre falso para apps externos recém-descobertos;
- `safetyReview`: estado e data da avaliação.

O catálogo define o que pode ser oferecido; a escolha efetiva e a configuração ficam locais no dispositivo e sob controle dos responsáveis. Descobrir um app instalado nunca deve liberá-lo automaticamente.

### 2. Ponte nativa Android

A interface HTML atual roda dentro de WebView. Para descobrir e abrir aplicativos Android, será necessária uma ponte nativa Android pequena, com uma API explícita e validada, por exemplo:

- `listLaunchableApps()`
- `launchAllowedApp(packageName)`
- `getAppVersion(packageName)`
- `getKioskCapability()`

A ponte deve validar no lado nativo que o pacote solicitado está na lista autorizada; não basta confiar em uma verificação JavaScript. Não expor métodos genéricos de execução, intents arbitrários ou acesso a arquivos. A interface e a ponte nativa excluem uma lista inicial de apps que podem contornar a experiência infantil (configurações do sistema, instaladores/lojas, navegadores e o app nativo do YouTube); essa lista precisa ser revista por fabricante e não substitui os controles do Device Owner.

A visibilidade de apps instalados depende das regras de visibilidade de pacotes do Android. Antes de usar `QUERY_ALL_PACKAGES`, avaliar a política de distribuição e a necessidade real; preferir visibilidade limitada a pacotes conhecidos quando possível. Em instalação por sideload, a viabilidade técnica pode ser diferente da distribuição pela Play Store.

### 3. Modo quiosque: limitação importante

O modo de fixação de tela (*screen pinning*) não equivale a um bloqueio parental inviolável do dispositivo. Um launcher ou WebView comum não consegue garantir, em todos os aparelhos, que a criança jamais acesse a interface do sistema.

Para controle realmente gerenciado entre aplicativos, usar **Android Lock Task Mode com o app provisionado como Device Owner**, em dispositivos dedicados/configurados pelo responsável. O código inicial sincroniza os pacotes liberados com a política de Lock Task quando detecta Device Owner. Isso exige processo de provisionamento, testes por fabricante/versão e instruções claras de saída e recuperação. Não prometer bloqueio total em aparelhos pessoais não gerenciados. Consulte [ANDROID_KIOSK_SETUP.md](ANDROID_KIOSK_SETUP.md).

Enquanto o modo gerenciado não estiver configurado, apresentar o recurso como proteção de navegação/experiência, não como bloqueio absoluto.

### 4. Segurança e privacidade

- PIN armazenado com derivação criptográfica adequada e salt; nunca armazenar PIN em texto puro.
- Limitar tentativas, atrasar novas tentativas e exigir PIN para alterar a lista autorizada.
- Configuração infantil por perfil e armazenada localmente por padrão.
- Não enviar lista de aplicativos instalados, hábitos ou histórico de uso a servidores.
- Não usar publicidade comportamental ou perfis de marketing infantil.
- Registrar apenas telemetria técnica estritamente necessária, desativada por padrão quando possível.
- Documentar serviços externos: o player oficial do YouTube e a API do YouTube podem tratar dados segundo as políticas deles; “sem servidor próprio” não significa ausência de tratamento por terceiros.
- Respeitar acessibilidade, chamadas de emergência, notificações críticas, sono e pausas saudáveis.

## Integração dos produtos

| Produto | Papel | Estado inicial |
|---|---|---|
| Hora do Desenho | App principal, grade audiovisual e hub familiar | Produto prioritário |
| Tomb Mask Kids | Jogo VSA opcional no hub | Pausado; não iniciar novas tarefas agora |
| GCompris | App externo selecionado pelos responsáveis | Descoberta/compatibilidade a validar |
| Kiddo Wise Routines | Rotinas e transições familiares | Futuro, após revisão do repositório |

Para apps externos, a VSA deve distinguir “instalado”, “compatível para iniciar” e “gerenciado em modo quiosque”. Abrir um app é diferente de conseguir limitar seu tempo ou impedir que ele saia para o sistema.

## Plano de entrega

### Fase A — conclusão segura do Hora do Desenho
- [ ] Revisar e testar PIN, perfis, limites diários e bloqueios em Android TV e celular.
- [ ] Verificar os fluxos de saída, reinicialização, relógio alterado e falta de rede.
- [ ] Documentar limites reais do modo criança em cada versão/fabricante.
- [ ] Revisar uso do player/API do YouTube e a descrição de privacidade.
- [ ] Gerar APK e validar em aparelho real; não declarar concluído antes de CI e teste físico.

### Fase B — catálogo parental
- [ ] Adicionar catálogo declarativo e interface “Aplicativos permitidos” na área dos responsáveis.
- [ ] Adicionar armazenamento local e perfis de apps liberados.
- [ ] Implementar ponte nativa restrita para listar e iniciar apps autorizados.
- [ ] Testar pacotes ausentes, apps removidos, versões incompatíveis e falhas de inicialização.

### Fase C — quiosque e validação
- [ ] Prototipar Lock Task Mode em dispositivo de teste provisionado como Device Owner.
- [ ] Validar recuperação, emergência, acessibilidade e saída dos responsáveis.
- [ ] Documentar diferenças entre Android TV, Google TV, Fire TV e celular Android.
- [ ] Validar integração de GCompris e, posteriormente, Tomb Mask Kids.
- [ ] Só então decidir o critério de release estável.

## Critérios de aceite do hub

1. Nenhum app é liberado apenas por estar instalado.
2. Alterar apps permitidos exige autenticação parental.
3. A criança vê somente os apps liberados no catálogo infantil.
4. Um pacote não autorizado não pode ser aberto por chamada direta à ponte nativa.
5. Falha ao iniciar um app não remove nem amplia a lista autorizada.
6. O produto informa claramente quando o dispositivo não oferece bloqueio quiosque forte.
7. Testes documentam modelo do dispositivo, versão Android e resultado.
8. Privacidade e segurança infantil são revisadas antes de uma distribuição pública.

## Referências técnicas para validação

- Android dedicated devices / Lock Task Mode: https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- Android package visibility: https://developer.android.com/training/package-visibility
- YouTube API Services Terms: https://developers.google.com/youtube/terms/api-services-terms-of-service
- AAP 5 Cs: https://www.aap.org/5Cs
- LGPD (Lei nº 13.709/2018): https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm

> Esta branch contém uma primeira implementação do catálogo, da interface de seleção e da ponte nativa. Isso não significa que o hub esteja validado: build, testes de segurança e testes em aparelho real ainda são necessários.