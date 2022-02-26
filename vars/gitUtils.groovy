#!/usr/bin/env groovy

def call(String inBranch = "master", String inRepository, String inCredentials) {
  git branch: inBranch, url: inRepository, credentialsId: inCredentials
}
