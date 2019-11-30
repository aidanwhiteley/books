#!/bin/bash
# Sets application user name and password from environment variables
# If editing, ensure file has unix style line endings - use dos2unix after editing on Windows

mongo <<EOF
use admin
db.createUser({
  user:  "$MONGO_INITDB_APP_USERNAME",
  pwd: "$MONGO_INITDB_APP_PASSWORD",
  roles: [{
    role: 'readWrite',
    db: 'books-container-demo-no-auth'
  }]
})
EOF
