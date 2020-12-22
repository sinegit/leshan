![Sine Lab](https://github.com/sinegit/leshan/blob/master/leshan-server-demo/src/main/resources/webapp/img/sinelogo.png)

Generate the public and private keys for the clients and the server according to the instructions provided [here](https://github.com/eclipse/leshan/wiki/Credential-files-format) and store them in `./clientkeys` and `./serverkeys` respectively.

Build the project

`mvn clean install`

Start the server

`java -jar .\leshan-server-demo\target\leshan-server-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -pubk .\serverkeys\cpubk.der -prik .\serverkeys\cprik.der`

Start the client

`java -jar .\leshan-client-demo\target\leshan-client-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -n clientDTLS -m ..\models\ -cpubk ..\clientkeys\cpubk.der -cprik ..\clientkeys\cprik.der -spubk ..\clientkeys\serverPubKey.der`