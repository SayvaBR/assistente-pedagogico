# Primeira entrada — implementação inicial

Branch `feat/entry-onboarding`, empilhada sobre PR #18. Dono: integrador principal. Arquivos exclusivos: MainActivity.kt e FirstAccessFlow.kt.

Referências conceituais inspecionadas: IMAGENS PARA REF/1000273009.png (progresso e seleção) e 1000273016.png (perfil e superfícies). Adaptados cartões arredondados, hierarquia forte, azul/branco e profundidade sólida ao design system. As pranchas não são capturas Android.

Fluxo: abertura durante leitura local, boas-vindas, nome, etapa, primeira turma, confirmação após gravação e aplicativo. Progresso e campos locais retomam após fechar. Perfil existente entra diretamente. Perfil e turma são gravados em uma transação SQLite; Voltar preserva campos e envio fica bloqueado durante gravação.

Login e criação de conta possuem telas, mas autenticação remota está indisponível e explicitamente identificada. Nenhuma senha é persistida e nenhuma sessão é simulada. A opção de começar no aparelho é funcional. Provedor, recuperação e sincronização precisam de implementação própria.

Verificação: diff sem erros de whitespace. CI solicitado pela PR, resultado pendente no SHA. Gradle e ADB locais indisponíveis. Validação visual Android, TalkBack, texto ampliado e testes instrumentados específicos de retomada e rollback pendentes. Esta entrega não representa autenticação pronta para lançamento nem aprovação visual.
