#!/usr/bin/env bash
# Script: fix_github_noreply.sh
# Propósito: configurar globalmente git con un correo noreply, verificar, enmendar el último commit si usa otro email y hacer push seguro.
# Uso: ./scripts/fix_github_noreply.sh [--yes] [--auto-ssh]

set -euo pipefail
IFS=$'\n\t'

# Configuración - edita si lo deseas
GIT_NAME="Fufushiro"
GIT_NOREPLY_EMAIL="198467872+Fufushiro@users.noreply.github.com"
AUTO_CONFIRM=0
AUTO_SSH=0
SSH_KEY_PATH="$HOME/.ssh/id_ed25519_github_pdftoon"

# Parse args
for arg in "$@"; do
  case "$arg" in
    --yes|-y)
      AUTO_CONFIRM=1
      ;;
    --auto-ssh)
      AUTO_SSH=1
      ;;
    --help|-h)
      echo "Uso: $0 [--yes] [--auto-ssh]"
      echo "  --yes      : confirmar automáticamente enmendar y push forzado"
      echo "  --auto-ssh : crear una clave SSH (si no existe) y convertir remoto HTTPS a SSH cuando corresponda (requiere añadir clave pública a GitHub antes de push)"
      exit 0
      ;;
  esac
done

confirm() {
  if [ "$AUTO_CONFIRM" -eq 1 ]; then
    return 0
  fi
  read -r -p "$1 [y/N]: " answer
  case "$answer" in
    [Yy]|[Yy][Ee][Ss]) return 0 ;;
    *) return 1 ;;
  esac
}

print_step() { echo -e "\n---> $1"; }

print_step "1) Aplicando configuración global de git"
git config --global user.name "$GIT_NAME"
git config --global user.email "$GIT_NOREPLY_EMAIL"

echo "Configuración global aplicada:"
echo "  user.name  = $(git config --global --get user.name)"
echo "  user.email = $(git config --global --get user.email)"

# Mostrar configuración local si existe
local_name=$(git config --get user.name || echo "(none)")
local_email=$(git config --get user.email || echo "(none)")
echo "Configuración local (este repo):"
echo "  user.name  = $local_name"
echo "  user.email = $local_email"

# Comprobar último commit
if git rev-parse --verify HEAD >/dev/null 2>&1; then
  last_email=$(git log -1 --pretty=format:'%ae')
  last_author_full=$(git log -1 --pretty=format:'%an <%ae>')
  echo "Último commit: $last_author_full"
else
  last_email=""
  echo "No hay commits en este repositorio (no hay nada que enmendar)."
fi

amended=0
if [ -n "$last_email" ] && [ "$last_email" != "$GIT_NOREPLY_EMAIL" ]; then
  echo "El último commit usa email diferente: $last_email"
  if confirm "¿Deseas enmendar el último commit para usar $GIT_NOREPLY_EMAIL?"; then
    git commit --amend --author="$GIT_NAME <$GIT_NOREPLY_EMAIL>" --no-edit
    amended=1
    echo "Commit enmendado. Nuevo autor: $(git log -1 --pretty=format:'%an <%ae>')"
  else
    echo "Se omitió enmendar el commit. No se cambiará el historial local."
  fi
else
  echo "No es necesario enmendar (el último commit ya usa el noreply o no hay commits)."
fi

# Determinar rama actual
branch=$(git branch --show-current || true)
if [ -z "$branch" ]; then
  echo "ERROR: no se pudo detectar la rama actual. Asegúrate de estar en una rama (no en detached HEAD)."
  exit 1
fi
echo "Rama actual: $branch"

# Seleccionar remoto: preferir 'origin', sino tomar el primer remoto
remote_to_use=""
if git remote get-url origin >/dev/null 2>&1; then
  remote_to_use=origin
else
  # tomar primer remote si existe
  first_remote=$(git remote | head -n 1 || echo "")
  if [ -n "$first_remote" ]; then
    remote_to_use=$first_remote
  fi
fi

if [ -z "$remote_to_use" ]; then
  echo "No hay remotos configurados. Añade un remote y vuelve a ejecutar. Ej: git remote add origin <url>"
  exit 1
fi

echo "Remoto seleccionado: $remote_to_use"

# Detectar si el remote usa HTTPS y preparar conversión a SSH
remote_url=$(git remote get-url "$remote_to_use") || remote_url=""
uses_https=0
if [[ "$remote_url" == https://*github.com* ]]; then
  uses_https=1
fi

if [ "$uses_https" -eq 1 ]; then
  echo "El remoto $remote_to_use usa HTTPS: $remote_url"
  if [ "$AUTO_SSH" -eq 1 ] || confirm "¿Deseas crear/usar una clave SSH local y convertir el remoto a SSH (git@github.com:...)?"; then
    # Crear clave SSH si no existe
    if [ ! -f "$SSH_KEY_PATH" ]; then
      echo "Creando clave SSH en $SSH_KEY_PATH (sin passphrase)"
      mkdir -p "$(dirname "$SSH_KEY_PATH")"
      ssh-keygen -t ed25519 -f "$SSH_KEY_PATH" -N "" -C "$GIT_NOREPLY_EMAIL"
      echo "Clave creada: $SSH_KEY_PATH"
    else
      echo "Se encontró clave SSH existente en $SSH_KEY_PATH"
    fi

    echo "\nContenido de la clave pública (pega esto en GitHub > Settings > SSH and GPG keys > New SSH key):"
    echo "---- BEGIN PUBLIC KEY ----"
    cat "$SSH_KEY_PATH.pub"
    echo "---- END PUBLIC KEY ----"

    echo "Para añadir la clave a GitHub: copia la salida anterior y pégala en https://github.com/settings/ssh/new"
    echo "Si deseas que este script intente el push ahora, añade la clave en GitHub y presiona Enter."
    if [ "$AUTO_SSH" -eq 0 ]; then
      read -r -p "Presiona Enter después de añadir la clave pública en GitHub (o Ctrl+C para cancelar)..."
    else
      echo "--auto-ssh: esperando brevemente para que el usuario añada la clave si aún no lo ha hecho (5s)"
      sleep 5
    fi

    # Construir URL SSH a partir de la URL HTTPS
    # Ej: https://github.com/USER/REPO.git -> git@github.com:USER/REPO.git
    repo_path=${remote_url#https://github.com/}
    repo_path=${repo_path#github.com/}
    ssh_url="git@github.com:${repo_path}"
    echo "Cambiando remote $remote_to_use de $remote_url a $ssh_url"
    git remote set-url "$remote_to_use" "$ssh_url"
    remote_url=$ssh_url
  else
    echo "Se conservó el remoto en HTTPS. Si prefieres HTTPS, necesitarás usar un token o credenciales válidas para push."
  fi
fi

# Push según si enmendamos
push_mode=""
if [ "$amended" -eq 1 ]; then
  echo "\nSe reescribió el último commit localmente. Para actualizar el remoto se necesita forzar el push."
  if confirm "¿Confirmas hacer push con --force-with-lease a $remote_to_use/$branch?"; then
    echo "Ejecutando: git push --force-with-lease $remote_to_use $branch"
    if git push --force-with-lease "$remote_to_use" "$branch"; then
      push_mode="force-with-lease"
      echo "Push forzado seguro completado."
    else
      echo "ERROR: el push con --force-with-lease falló. Intenta de nuevo o revisa tus credenciales/permiso remoto." >&2
      exit 1
    fi
  else
    echo "Se omitió el push forzado. Tu repo remoto no se actualizó con la enmienda local."
    push_mode="skipped"
  fi
else
  # Si no enmendamos, intentar push normal; si no hay upstream, set-upstream
  if git rev-parse --abbrev-ref --symbolic-full-name @{u} >/dev/null 2>&1; then
    echo "Haciendo push normal a la rama remota (upstream existe)..."
    git push
    push_mode="normal"
  else
    echo "No hay upstream: haciendo push y estableciendo upstream en $remote_to_use/$branch"
    git push --set-upstream "$remote_to_use" "$branch"
    push_mode="set-upstream"
  fi
fi

# Resumen final
echo "\n--- Resumen final ---"
echo "Configuración global: user.name=$(git config --global --get user.name) user.email=$(git config --global --get user.email)"
echo "Último commit local: $(git log -1 --pretty=format:'%h %an <%ae> %s' || echo '(no commits)')"
echo "Push result: $push_mode to $remote_to_use/$branch"

echo "\nOperación completada con éxito."

exit 0
