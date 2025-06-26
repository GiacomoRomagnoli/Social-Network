@echo off
set NAMESPACE=social-network
set KAFKA=kafka
set MYSQL_CLUSTER=mysql-cluster
set MYSQL_OPERATOR=mysql-operator

echo - Imposto il namespace corrente...
kubectl config set-context --current --namespace=default

echo - Disinstallazione Kafka...
helm uninstall %KAFKA% --namespace %NAMESPACE%

echo - Disinstallazione MySQL cluster...
helm uninstall %MYSQL_CLUSTER% --namespace %NAMESPACE%
kubectl wait --for=delete pod -l app.kubernetes.io/managed-by=%MYSQL_OPERATOR% --namespace=%NAMESPACE% --timeout=600s

echo - Disinstallazione Operator...
helm uninstall %MYSQL_OPERATOR% --namespace %NAMESPACE%

echo - Disinstallazione Servizi...
helm uninstall user-service --namespace %NAMESPACE%
helm uninstall friendship-service --namespace %NAMESPACE%
helm uninstall content-service --namespace %NAMESPACE%
helm uninstall api-gateway --namespace %NAMESPACE%
helm uninstall notification-service --namespace %NAMESPACE%

echo - Disinstallazione degli Ingress...
helm uninstall ingress --namespace %NAMESPACE%

echo - Eliminazione namespace...
kubectl delete namespace %NAMESPACE%

echo - Arresto di Minikube...
minikube stop