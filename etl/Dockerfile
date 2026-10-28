# Images de l'ETL, construites par docker-compose :
#   - cible "run" : exécution du workflow en ligne de commande (service etl)
#   - cible "web" : interface graphique Hop dans le navigateur (service hop-web)
# Le pilote PostgreSQL est fourni par Hop ; celui de MySQL ne l'est pas (licence), on l'ajoute.

ARG HOP_VERSION=2.18.1
ARG MYSQL_CONNECTOR_VERSION=9.3.0

# ---------- exécution (hop-run)
FROM apache/hop:${HOP_VERSION} AS run
ARG MYSQL_CONNECTOR_VERSION
USER root
RUN curl -fsSL -o /opt/hop/lib/jdbc/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar \
      https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_CONNECTOR_VERSION}/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar
USER hop

# ---------- interface web (Hop Web, Tomcat sur le port 8080 du conteneur)
FROM apache/hop-web:${HOP_VERSION} AS web
ARG MYSQL_CONNECTOR_VERSION
USER root
RUN curl -fsSL -o /usr/local/tomcat/jdbc-drivers/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar \
      https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_CONNECTOR_VERSION}/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar
USER hop
# identifiants du .env, projet produits-etl et environnement docker par défaut, puis démarrage de Hop Web
CMD ["/bin/bash", "/files/docker/demarrer-hop-web.sh"]
