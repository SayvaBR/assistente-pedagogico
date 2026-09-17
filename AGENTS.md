# Contrato de execução dos agentes — Assistente Pedagógico

1. Ler `docs/BLUEPRINT.md`, `docs/STATUS.md`, `docs/AGENT_WORKFLOW.md` e as skills pertinentes antes de editar.
2. Projeto clean-room: trabalhar EXCLUSIVAMENTE em `SayvaBR/assistente-pedagogico`; nunca consultar/copiar o repositório antigo.
3. Antes de iniciar, consultar PRs abertos, HEAD, base e arquivos em alteração; registrar dono da área. **Não sobrescrever a branch de outro agente.** O gate `tools/pr_overlap.py` detecta arquivos compartilhados entre PRs irmãos: sobreposição em `app/`, `tools/` ou workflows bloqueia integração até revisão conjunta. PRs empilhados devem documentar sua dependência.
4. Uma unidade de entrega = branch isolada + PR DRAFT + comportamento real + casos de erro + persistência/reabertura quando aplicável + CI e testes relativos ao SHA exato. Build verde não equivale a fluxo Android ou visual aprovado.
5. Toda feature deve antecipar entradas inválidas, IDs de outras turmas, homônimos, toques repetidos, estados não salvos, permissões SAF revogadas, migrações e rollback. Testar ao menos um caso de sucesso e um de falha que preserva os dados.
6. Para alterações de UI, exigir evidência visual **pontual e real** quando necessária ao gate; não produzir capturas repetitivas nem simular aprovação. Respeitar o Design System `docs/DESIGN_SYSTEM.md`.
7. Proteger dados de alunos: somente dados sintéticos em PRs, logs, issues, capturas e agentes. Nunca colocar segredos/tokens externos no repositório público.
8. Não inventar resultados, não reduzir cobertura de testes, não usar força em branches alheias e não adicionar biblioteca sem necessidade/licença revisadas.
9. `main`, publicação, APK/AAB comercial, RevenueCat/Play e decisões irreversíveis dependem de aprovação expressa da titular. A geração/distribuição de novos APKs está adiada por decisão atual da titular; compilar internamente no CI é permitido.
10. Perfis em `.github/agents/` são instruções, não agentes executando sozinhos. O auditor do Actions é determinístico e somente leitura; não chamá-lo de revisão visual/segurança completa.
