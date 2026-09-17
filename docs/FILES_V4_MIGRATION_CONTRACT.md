# Arquivos — contrato da migração SQLite v3 → v4

Esta branch é empilhada sobre `feat/11-agent-workflow-design-system` e não altera `TeacherApp.kt` enquanto PRs irmãos ainda convergem nesse arquivo.

## Objetivo

Adicionar organização real de Arquivos sem perder registros `saved_files` existentes. A migração v4 deverá introduzir, de forma aditiva:

- pastas persistentes;
- vínculo opcional de arquivo a pasta;
- favorito persistente;
- lixeira reversível com data de remoção;
- restauração sem alterar `uri` nem identidade do arquivo;
- estado suficiente para detectar permissões SAF indisponíveis sem apagar metadados.

## Invariantes obrigatórios

1. Migração v3 → v4 é **não destrutiva**: nunca `DROP TABLE saved_files`, nunca recriar a base apagando dados.
2. Todo registro v3 preserva `id`, `name` e `uri` exatamente após upgrade.
3. Arquivos existentes começam fora da lixeira, não favoritos e sem pasta.
4. `uri` continua única. Organizar, favoritar, enviar à lixeira e restaurar não cria uma segunda referência.
5. Excluir uma pasta não pode excluir o arquivo original nem a referência salva; os arquivos voltam para “Sem pasta”.
6. Enviar à lixeira é reversível. Exclusão definitiva deve ser uma ação separada e explícita.
7. Falha/revogação SAF não remove metadados silenciosamente. A UI deve sinalizar que o acesso precisa ser concedido novamente.
8. Migração precisa ser transacional e idempotente no estado final: abrir novamente a base v4 não modifica dados.
9. Nenhum teste usa URI, nome de aluno ou documento real; somente fixtures sintéticas.

## Modelo proposto

```sql
CREATE TABLE file_folders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL COLLATE NOCASE UNIQUE,
  created_at TEXT NOT NULL
);

ALTER TABLE saved_files ADD COLUMN folder_id INTEGER REFERENCES file_folders(id) ON DELETE SET NULL;
ALTER TABLE saved_files ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1));
ALTER TABLE saved_files ADD COLUMN trashed_at TEXT DEFAULT NULL;
ALTER TABLE saved_files ADD COLUMN access_state TEXT NOT NULL DEFAULT 'unknown' CHECK(access_state IN ('unknown','available','revoked'));
```

Antes de aplicar no código, validar a estratégia de `ALTER TABLE ... REFERENCES` na versão SQLite suportada pelo `minSdk` e cobrir upgrade real de fixture v3 em teste instrumentado. Se a compatibilidade exigir reconstrução da tabela, ela deve ocorrer dentro de transação com cópia integral e assertions de contagem/IDs/URIs antes de remover a tabela antiga.

## Testes mínimos para a implementação

- fixture v3 com múltiplos arquivos → upgrade v4 preserva contagem, IDs, nomes e URIs;
- defaults v4 corretos para registros legados;
- criar/renomear pasta e impedir nomes duplicados por caixa;
- mover arquivo para pasta e remover pasta preservando arquivo;
- favoritar/desfavoritar persiste após fechar/reabrir `TeacherStore`;
- lixeira/restauração persiste após fechar/reabrir;
- URI continua única durante todas as operações;
- acesso SAF revogado não apaga registro;
- operação inválida não deixa alteração parcial;
- reabrir base já em v4 não altera snapshot.

## Gate de UI

Somente depois da camada de dados e regressão v3→v4 verdes: integrar abas/ações reais de Pastas, Favoritos e Lixeira na tela de Arquivos usando o Design System. Validação visual continua separada em 360/412/480dp, font scaling, Android 16/insets e TalkBack; CI verde não equivale a aprovação visual.
