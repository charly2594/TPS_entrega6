##RPCServer Card Service
Usando el patrón RPC, este software esta alerta de un broker de mensajes rabbitMQ.
RabbitMq broker:
- Host: crane.rmq.cloudamqp.com
- Vhost: riikuyvl
- User: riikuyvl
- Password: WtYUU4rdx0-UOTPE0yrObjMZt4WXuAxh
##Intstrucciones de uso:

1) Abrir terminal en directorio raiz del proyecto
2) /# mvn install

*Para ejecutar RPCServer (escuchará mensajes constantemente):*
- /# mvn exec:java -Dexec.mainClass=RPCServer

*Para ejecutar RPCClient (Prueba manual temporarizada):*
- /# mvn exec:java -Dexec.mainClass=RPCClient
