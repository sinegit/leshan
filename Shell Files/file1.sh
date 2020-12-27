sudo apt-get update
sleep 1 

sudo apt-get upgrade
sleep 1

sudo apt install openconnect
sleep 1 

sudo apt install dos2unix
sleep 1 

sudo apt install maven
sleep 1

git clone https://github.com/sinegit/leshan.git
sleep 1 
cd leshan 
mvn -pl '!leshan-integration-tests' clean install 

sudo reboot