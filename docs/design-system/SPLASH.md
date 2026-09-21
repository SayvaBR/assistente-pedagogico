# Splash — aplicação da identidade

Âncoras: AP-02 para tela de inicialização; AP-05 para acabamento da marca e ilustração. A apresentação longa de AP-05 não deve virar uma espera obrigatória na abertura.

Conteúdo: símbolo da marca e nome “Assistente Pedagógico”, fundo claro da família azul. Sem slogan acrescentado, login, carrossel ou benefícios na inicialização. Livro é símbolo, não mascote; não criar expressão, gesto ou animação de personagem. Reutilizar desenho aprovado, sem redesenhá-lo por interpretação do agente.

Receita inicial em 390dp: padding 24; símbolo com largura aproximada de 144dp e proporção preservada; gap 20 até título 32/36sp em duas linhas; conjunto centralizado horizontalmente, próximo de 44% da altura útil. Valores são normalização técnica, ajustáveis pela comparação real. Não escalar pixels do mockup diretamente para dp.

Se inicialização demorar de verdade, progresso discreto 112×6dp abaixo do conjunto, sem porcentagem fictícia. Não atrasar abertura para exibir arte. Falha persistente exige recuperação acessível.

O splash do sistema Android tem máscara/restrições próprias: não encaixar a tela inteira dentro do ícone. Manter continuidade de fundo com a primeira composição Compose, sem duas apresentações longas. Movimento opcional de entrada 280ms por opacidade; com animações desativadas, estado final direto.

Antes de implementar, localizar asset master. Recorte da prancha com fundo ou baixa resolução não é asset final. Reconstrução fiel deve ser identificada como pendente de aceite. Entregar captura Android e comparação com as referências; não usar imagem gerada como evidência de execução.
