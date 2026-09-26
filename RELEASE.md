# Release — Meu Supermercado 1.0.20+23

## Destaques

- Produtos favoritos e recorrentes no catálogo.
- Frequência recorrente semanal, quinzenal ou mensal.
- Nova tela **Adicionar recorrentes** em cada lista.
- Modelos iniciais e modelos criados pelo próprio usuário.
- Criação de nova lista vazia ou a partir de modelo.
- Backup JSON atualizado para schema 2 com os novos dados.

## Modelos iniciais

O modelo **Cicloviagem** reutiliza os 18 itens predefinidos já existentes no projeto. **Compra do mês**, **Churrasco**, **Camping** e **Limpeza** são disponibilizados sem itens, evitando inventar produtos que o usuário não definiu.

Qualquer lista existente pode ser salva como modelo. Ao criar uma nova lista a partir dele, os itens são copiados com novos identificadores e com `purchased=false`.

## Recorrentes

A recorrência é configurada no catálogo como semanal, quinzenal ou mensal. A tela **Adicionar recorrentes** permite escolher quais produtos recorrentes entram na lista atual e evita duplicação por nome normalizado.

## Build

O workflow principal continua sendo `.github/workflows/android-release.yml`, produz APK Release assinado e publica o `.apk` diretamente na GitHub Release, sem `actions/upload-artifact` e sem `source.zip`.
