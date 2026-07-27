# TLS certs for docker-compose.https.yml (gitignored).
# Expected files:
#   fullchain.pem
#   privkey.pem
#
# Quick local smoke test (self-signed):
#   openssl req -x509 -nodes -days 30 -newkey rsa:2048 \
#     -keyout privkey.pem -out fullchain.pem \
#     -subj "/CN=localhost"
