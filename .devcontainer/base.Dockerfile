ARG OS="focal"
ARG JDK="11"
FROM mcr.microsoft.com/vscode/devcontainers/base:${OS}

# [Optional] Uncomment this section to install additional OS packages.
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends leiningen openjdk-${JDK}-jdk
