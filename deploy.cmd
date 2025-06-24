@echo off
set NAMESPACE=social-network
set KAFKA=kafka
set MYSQL_CLUSTER=mysql-cluster
set MYSQL_OPERATOR=mysql-operator
set ROOT=root
set PASSWORD=password
set USER_SERVICE=user-service
set CONTENT_SERVICE=content-service
set FRIENDSHIP_SERVICE=friendship-service
set NOTIFICATION_SERVICE=notification-service
set API_GATEWAY=api-gateway

echo - Avvio Minikube...
minikube start --memory=5926 --cpus=8

echo - Aggiunta repo...
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add mysql-operator https://mysql.github.io/mysql-operator/
helm repo update

echo - Verifica connessione al cluster...
kubectl get nodes

echo - Creazione namespace...
kubectl create namespace %NAMESPACE%
kubectl config set-context --current --namespace=%NAMESPACE%

echo - Installazione Kafka...
helm install %KAFKA% bitnami/kafka --wait ^
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
helm install %MYSQL_OPERATOR% mysql-operator/mysql-operator --wait

echo - Installazione DB cluster...
helm install %MYSQL_CLUSTER% mysql-operator/mysql-innodbcluster ^
    --set credentials.root.user=%ROOT% ^
    --set credentials.root.password=%PASSWORD% ^
    --set credentials.root.host=% ^
    --set serverInstances=2 ^
    --set routerInstances=1 ^
    --set tls.useSelfSigned=true

echo - Installazione Servizi...
helm install %USER_SERVICE% ./kubernetes/microservice --set serviceType=%USER_SERVICE%
helm install %FRIENDSHIP_SERVICE% ./kubernetes/microservice --set serviceType=%FRIENDSHIP_SERVICE%
helm install %CONTENT_SERVICE% ./kubernetes/microservice --set serviceType=%CONTENT_SERVICE%
helm install %API_GATEWAY% ./kubernetes/microservice --set serviceType=%API_GATEWAY%
helm install %NOTIFICATION_SERVICE% ./kubernetes/microservice --set serviceType=%NOTIFICATION_SERVICE%
