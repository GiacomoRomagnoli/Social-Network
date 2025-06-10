@echo off
set NAMESPACE=social-network
set KAFKA=kafka
set MYSQL_CLUSTER=mysql-cluster
set MYSQL_OPERATOR=mysql-operator

echo - Avvio Minikube...
minikube start

echo - Aggiunta repo...
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add mysql-operator https://mysql.github.io/mysql-operator/
helm repo update

echo - Verifica connessione al cluster...
kubectl get nodes

echo - Creazione namespace...
kubectl create namespace %NAMESPACE%
kubectl config set-context --current --namespace=%NAMESPACE%

echo - Installazione Kafka in corso...
helm install %KAFKA% bitnami/kafka ^
  --set replicaCount=3 ^
  --set configurationOverrides."auto.create.topics.enable"=true ^
  --set configurationOverrides."default.replication.factor"=3 ^
  --set configurationOverrides."offsets.topic.replication.factor"=3 ^
  --set configurationOverrides."transaction.state.log.replication.factor"=3 ^
  --set configurationOverrides."transaction.state.log.min.isr"=2 ^
  --set listeners.client.protocol=PLAINTEXT ^
  --set listeners.external.protocol=PLAINTEXT ^
  --set listeners.interbroker.protocol=PLAINTEXT ^
  --set listeners.controller.protocol=PLAINTEXT

echo - Installazione Operator...
helm install %MYSQL_OPERATOR% mysql-operator/mysql-operator

echo - Installazione DB cluster...
helm install %MYSQL_CLUSTER% mysql-operator/mysql-innodbcluster ^
    --set credentials.root.user="root" ^
    --set credentials.root.password="password" ^
    --set credentials.root.host="%" ^
    --set serverInstances=3 ^
    --set routerInstances=1 ^
    --set tls.useSelfSigned=true
