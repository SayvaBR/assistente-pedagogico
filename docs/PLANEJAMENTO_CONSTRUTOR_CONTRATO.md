# Planejamento — contrato do construtor flexível (18/09/2026)

## Estado da implementação

A engine imutável `data/PlanComposition.kt` e seus testes já foram adicionados. **O editor atual continua fixo**: não há ainda edição visual de modelos, armazenamento de layout por plano, exportação seguindo ordem customizada nem migração v9. Não apresentar estas capacidades como concluídas.

## Objetivo funcional

O professor escolhe quais seções compõem o documento, pode adicionar seções próprias, renomeá-las, escrevê-las enquanto monta o plano e mover cada uma para cima/baixo. Pode salvar um modelo próprio de escola sem copiar automaticamente o conteúdo de aulas anteriores. Um plano existente deve manter seu conteúdo e organização mesmo depois de editar ou excluir o modelo original. Todo fluxo funciona offline.

## Três camadas, nunca confundir

1. **Dados canônicos do plano:** ID, turma, data, disciplina, horário/duração, estado pedagógico, códigos BNCC e demais campos do `LessonPlanV6`. A data usada no calendário NÃO pode depender da posição/visibilidade do cartão de Identificação. BNCC é referência estruturada ao catálogo auditado; não armazenar texto supostamente oficial criado por sugestão.
2. **Composição por plano:** IDs estáveis dos blocos, tipo semântico, rótulo personalizável, ordem, seleção/visibilidade e conteúdo dos blocos livres. Os campos nativos leem/escrevem os dados canônicos sem mantê-los duplicados em JSON.
3. **Modelo reutilizável:** apenas estrutura e rótulos, sem texto livre, nome de aluno, observação pessoal ou conteúdo de uma aula por padrão. Uma cópia do modelo é aplicada ao plano; mudanças posteriores do modelo não modificam o documento concluído.

## Comportamentos e limites da engine introduzida

- `PlanComposition.standard()` fornece a organização atual sem alterar planos legados.
- `add`, `remove`, `move(±1)` e `update` são imutáveis; não se perde conteúdo durante uma mudança de posição.
- `IDENTIFICATION` é dado estrutural obrigatório, mas sua posição no documento é livre. Os outros blocos podem ser incluídos/removidos; várias seções `CUSTOM` são permitidas, com identificadores exclusivos. Blocos nativos não podem ser duplicados para não haver dois campos oficiais contraditórios.
- `saveAsTemplate` descarta conteúdo/tempos; duplicar uma aula completa será um comando separado, consciente e já conta com fluxo específico para o plano existente.
- Por ora o modelo é somente domínio/testes: nenhuma UI ou banco o consome. Não implementar uma interface que permita reorganizar visualmente sem persistir o resultado.

## Próximo recorte de código — atomicidade primeiro

1. Introduzir migração SQLite v8→v9 **aditiva** para `plan_templates` e `lesson_layouts`, sem reescrever linhas de `lessons` e sem alterar IDs. Os layouts de planos antigos são interpretados como `standard()` por fallback (migração preguiçosa, sem preencher o banco inteiro).
2. Criar codec de layout versionado, limite de tamanho, validação rigorosa de tipos/IDs e payloads; operações salvar plano + layout usam UMA transação. Templates editados não alteram snapshots de planos já criados.
3. Estender exportação, preview, integridade e restauração do `.apbackup` com os layouts/modelos; aceitar backups v1 antigos com fallback padrão e rejeitar novos payloads inválidos antes de qualquer DELETE.
4. Integrar no fluxo real uma seção `Montar meu plano`, com botões Adicionar/Remover/Subir/Descer, texto próprio, editor simples por tipo e opção Salvar como modelo. Rascunho deve sobreviver a rotação/retomada, descarte exige confirmação; formulário pode ser preenchido enquanto a estrutura é montada.
5. Fazer a visualização e o compartilhamento respeitarem **exatamente** a ordem escolhida; validação do status `PRONTO` verifica os dados exigidos pelo modelo, sem impedir rascunhos parciais. Testar emulação real de rotação, save→reopen, duplicação, edição e restore.

## BNCC é gate editorial distinto

O dataset independente atual contém 1.721 entradas e NÃO está homologado integralmente; vide `docs/PLANEJAMENTO_BNCC_AUDITORIA.md`. O construtor não deve gerar códigos, completar habilidade desconhecida ou rotular uma sugestão como texto oficial. Mostrar a fonte e obter conferência de cada registro antes da entrega do produto.

## Pronto apenas quando

O docente consegue criar um modelo, inserir dois blocos próprios, mover os blocos, preencher, salvar o plano, fechar e reabrir o app com a ordem intacta, duplicar sem acoplamento, restaurar backup e exportar um documento na ordem configurada — com testes automáticos e aceite manual. UI azul/branco/Lucide virá depois do funcionamento, sem screenshots rotineiras.
