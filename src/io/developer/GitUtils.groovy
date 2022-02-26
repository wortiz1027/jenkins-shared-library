package io.developer

def checkOut(inBranch = "master", repository, credentials) {
  git branch: inBranch, url: repository, credentialsId: credentials
}

return this
