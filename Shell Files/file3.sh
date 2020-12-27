git clone https://github.com/sixfab/Sixfab_RPi_CellularIoT_Library.git
cd Sixfab_RPi_CellularIoT_Library
sleep 1 
sudo python3 setup.py install

cd ./sample/
sleep 1 
python3 configureCATM1.py
sudo reboot 