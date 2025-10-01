# Use uma imagem que tenha o Java Development Kit (JDK) para compilar
FROM openjdk:11-jdk-slim

# Defina o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copie o código-fonte (a pasta src) para dentro do contêiner
COPY src /app/src

# Execute o compilador do Java de forma robusta
RUN javac -d bin $(find src -name "*.java")

# O comando para rodar a aplicação.
CMD ["java", "-cp", "bin", "application.Main", "5000", "knownPeers/knownPeers1_docker.txt", "tmp/peer1"]