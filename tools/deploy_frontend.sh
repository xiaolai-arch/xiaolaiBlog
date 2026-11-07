#!/usr/bin/env bash
# Retain hashed assets for existing browser sessions; publish index.html last with rename.
set -Eeuo pipefail
frontend_dir=$1
stage_dir=$2
site_url=$3
release="$frontend_dir/releases/$(basename "$stage_dir")"
mkdir -p "$release" "$frontend_dir/dist"
tar xzf "$stage_dir/dist.tgz" -C "$release"
test -s "$release/index.html"
if [ -f "$frontend_dir/dist/index.html" ]; then
  cp "$frontend_dir/dist/index.html" "$stage_dir/index.previous.html"
fi
# Copy assets before publishing the document. Old hashed assets stay available.
tar cf - --exclude='./index.html' -C "$release" . | tar xf - -C "$frontend_dir/dist"
chown -R www:www "$release" "$frontend_dir/dist"
cp "$release/index.html" "$frontend_dir/dist/index.html.new"
chown www:www "$frontend_dir/dist/index.html.new"
mv -f "$frontend_dir/dist/index.html.new" "$frontend_dir/dist/index.html"
rollback() {
  code=$?
  trap - ERR
  if [ -f "$stage_dir/index.previous.html" ]; then
    cp "$stage_dir/index.previous.html" "$frontend_dir/dist/index.html.rollback"
    chown www:www "$frontend_dir/dist/index.html.rollback"
    mv -f "$frontend_dir/dist/index.html.rollback" "$frontend_dir/dist/index.html"
  fi
  echo 'Frontend health check failed; previous index restored.' >&2
  exit "$code"
}
trap rollback ERR
curl --fail --silent --show-error --max-time 15 "$site_url/" > /dev/null
curl --fail --silent --show-error --max-time 15 "$site_url/gallery" > /dev/null
trap - ERR
echo 'Frontend deployed; old hashed assets retained.'
