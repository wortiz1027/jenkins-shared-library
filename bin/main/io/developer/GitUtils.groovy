package io.developer

def checkOut(inBranch = "master", repository) {
  git branch: inBranch, url: repository
}

return this