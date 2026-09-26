# Release 1.0.18+19

Catálogo local de produtos e leitura de código de barras integrados às listas existentes.

- Catálogo offline criado a partir dos produtos já cadastrados pelo usuário.
- Deduplicação por nome normalizado, ignorando diferenças de acentos, caixa e espaços extras.
- Cada produto do catálogo guarda nome, categoria, unidade padrão, último preço e código de barras opcional.
- Editor de item sugere produtos conhecidos e reaproveita dados já registrados.
- Leitor de código de barras pela câmera com ML Kit embarcado no APK, sem exigir consulta externa.
- Código conhecido preenche o produto; código desconhecido só é associado após o usuário informar e salvar os dados.
- Nova tela **Catálogo de produtos** em Configurações.
- Preço rápido do Modo compra também atualiza o último preço do catálogo.
- Workflow principal permanece único, gera APK Release assinado e publica o `.apk` diretamente na GitHub Release.
- APK permanece fora do ZIP do código-fonte e não há geração de `source.zip`.
