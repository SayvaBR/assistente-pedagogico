# Componentes v2

Medidas alvo em dp/sp; referências AP-01 a AP-05. Não copiar conteúdo fictício.

| Componente | Anatomia | Estados e comportamento |
| --- | --- | --- |
| CTA | Altura mínima 52, raio 16, padding horizontal 20, label 16/20, ícone 24 e gap 8 | Gradiente de ação; pressed #0054B8; loading mantém largura e bloqueia duplicação; disabled #DBE7F0/#526A80, sem sombra; foco navy 2dp com afastamento 2 |
| Secundário | Mesmo tamanho, fundo sky, label azul | Pressed #C8E9FC; disabled e foco equivalentes; não competir com CTA |
| Campo | Label externo 14/20, gap 8, altura mínima 52, padding 16, raio 12 | Foco contorno ação 2dp; erro escrito próximo ao campo; preservar entrada; readonly diferente de disabled; multiline cresce |
| Cartão | Branco, raio 20, padding 16–20, gaps 12 | Selecionado com contorno azul e check; clicável com feedback; não dar aparência clicável a conteúdo estático |
| Chip | Altura visual 32, alvo 48, padding 12, label 14/20 | Seleção com texto/check; quebra de linha; status não depende só da cor |
| Linha de lista | Altura mínima 64, padding 16, gap 12; suporte ilustrado 48 opcional | Texto flexível, metadado 14/20, chevron discreto; ação secundária com alvo próprio |
| Avatar | 40 em listas, 64–80 no cabeçalho | Usar família ilustrada aprovada; não inventar retrato ou dados pessoais |
| Top bar | Mínimo 56 mais inset; back com alvo 48 | Título e ações alinhados; sem status bar falsa |
| Bottom nav | Mínimo 64 mais inset; ícone 24, label visível | Ativo azul, inativo azul acinzentado; cinco destinos existentes; sem copiar nova arquitetura |
| Modal/sheet | Branco, raio 24, padding 24 | Back/cancelar, foco restaurado, rolagem e IME; confirmação destrutiva explícita |
| Feedback | Ícone e texto útil, ação recuperável | Erro persistente legível; sucesso só após operação real; sem métricas inventadas |
| Vazio | Objeto ilustrado do domínio, título específico e ação útil | Diferenciar busca vazia, primeiro uso e erro; não adicionar slogan |

## Receitas de tela

**Home / AP-01:** saudação e avatar; cartão da turma; aula em foco com título/conteúdo real e ilustração contextual; duas ações prioritárias; agenda em painel branco. Em largura pequena, o CTA da turma pode ir para a linha seguinte; não comprimir título e botão até ficarem ilegíveis. Sem turma, oferecer criação real em vez de exibir dados fictícios.

**Aula / AP-03:** cabeçalho, seleção de dia, cartão principal com disciplina/horário, tema, objetivo e materiais; CTA real. Texto pedagógico deve crescer sem altura fixa.

**Frequência / AP-03:** turma/data, resumo de presentes/faltas/pendentes, busca, linhas de alunos, ações de salvar/adiar existentes. Usar texto, cores e semantics; não presumir presença nem inventar pessoas.

**Configurações / AP-04:** perfil seguido de linhas agrupadas com suporte azul para ícone, título, descrição curta e chevron. Mostrar apenas opções implementadas.

**Mais / AP-04:** grade de dois cartões por linha quando cabe; uma coluna com fonte grande. Arte de cabeçalho é opcional conforme conteúdo útil, sem copiar slogan promocional. Não acrescentar login/sincronização apenas porque aparecem na prancha.

**Apresentação / AP-05:** referência de arte e hierarquia expressiva. Não transformar seu texto inteiro, indicadores e lista de benefícios em splash de inicialização.

Adaptar componentes compartilhados; não criar outra família de botões por tela. `ApGlyph` permanece provisório, não é Lucide. Ícones de domínio podem ter volume; setas/busca/ações pequenas precisam de traço simples e leitura clara.
