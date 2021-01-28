![Sine Lab](https://github.com/sinegit/leshan/blob/master/leshan-server-demo/src/main/resources/webapp/img/sinelogo.png)

Install Java JDK and set it as the system's default Java kit.

Clone this repository and navigate into the directory.

Generate the public and private keys for the clients and the server according to the instructions provided [here](https://github.com/eclipse/leshan/wiki/Credential-files-format) and store them in `./clientkeys` and `./serverkeys` respectively.

## Build the project

`mvn clean install`

This sometimes provides errors while running the integration tests, and the integration tests can be avoided through the following: 

`mvn -pl '!leshan-integration-tests' clean install`

If you want to skip the tests completely, execute the following command: 

`mvn clean install -DskipTests`

#  Server and Client with Private and Public Keys
## Start the Server

`java -jar .\leshan-server-demo\target\leshan-server-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -pubk .\serverkeys\cpubk.der -prik .\serverkeys\cprik.der`

## Start the client

`java -jar .\leshan-client-demo\target\leshan-client-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -n clientDTLS -m .\models\ -cpubk .\clientkeys\cpubk.der -cprik .\clientkeys\cprik.der -spubk .\clientkeys\serverPubKey.der`



# Instructions to Setup Leshan-Client on a Raspberry PI for the NEPTUNE 2.0 Project

## Step 1: Setup the Raspberry PI
Use the [Raspberry PI Imager](https://www.raspberrypi.org/blog/raspberry-pi-imager-imaging-utility/) to install the `Raspberry PI OS 32 bit` on a 16/32 GB micro SD Card. Insert the Memory Card to a Raspberry PI and complete the necessary setup.

## Step 2: Library Installation
This step will install the required libraries and software for setting up the Raspberry PI as a leshan client.

```sh
$ sudo apt-get update
$ sudo apt-get upgrade
$ sudo apt install openconnect
$ sudo apt install dos2unix
$ sudo apt install maven
$ git clone https://github.com/sinegit/leshan.git
$ git checkout sssaha
$ cd leshan 
$ mvn -pl '!leshan-integration-tests' clean install 
$ sudo reboot
```

## Step 3: Setting up the appropriate IOT Module
This project uses the [Raspberry Pi Cellular IoT HAT](https://sixfab.com/product/raspberry-pi-lte-m-nb-iot-egprs-cellular-hat/) from SIXFAB and the setup instructions can be found [here](https://docs.sixfab.com/docs/raspberry-pi-cellular-iot-hat-introduction). Enable the autoconnect option at the last step of setting the sixfab modem.



## Step 4: Start OpenConnect (if you need to use VPN)
Open a Terminal and execute the following: 
```sh
$ sudo openconnect -b -u Your_ASURITE sslvpn.asu.edu/2fa 
```
Provide your ASU password at the first field, For the second field type `push` which will generate a push notification on your DUO activated device. If you want to avoid the DUO push, execute the following: 

```sh
$ sudo openconnect -b -u Your_ASURITE sslvpn.asu.edu
```

## Step 4: Start The Client for the Powermonitor
To run this, you need to know the IP Address of the leshan server. After knwoing the IP Address of the server host execute the following on a seperate terminal (Provide the IP address as the arguement for the variable `-u`). 
```sh
java -jar ./leshan-client-demo/target/leshan-client-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -n PowerMonitor_1 -m ./models/ -u IP_ADDRESS_OF_THE_SERVER 
```


## Step 4: How to Insall Third Party Jar libraries
For the simulation prupose, this code uses the `EasyModbus` jar file. If the downloaded `jar` file does not come with a `.pom` file, then the installation can be done in the following way 
```sh
$ cd libs
$ mvn install:install-file -Dfile=EasyModbusJava.jar -DgroupId=de.re -DartifactId=EasyModbusJava -Dversion=2.8 -Dpackaging=jar
```

If the `jar` file comes with a `.pom` file, do the following: 
```sh
$ cd libs
$ mvn install:install-file -Dfile=EasyModbusJava.jar -DpomFile=<name_of_pom_file>
```