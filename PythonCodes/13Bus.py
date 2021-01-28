import numpy as np
import opendssdirect as dss
import matplotlib.pyplot as plt
import copy
import time
import datetime
from random import random
from pymodbus.client.sync import ModbusTcpClient
from pymodbus.payload import BinaryPayloadDecoder
from pymodbus.payload import BinaryPayloadBuilder
from pymodbus.constants import Endian

# Global variable initialization and error checking

IP_ADDRESS = "127.0.0.1" 
MODBUS_PORT = 503
VOLTAGE_PORTS = [0,4,8]

def update_register(data_input, data_type = 'voltage'):
    client = ModbusTcpClient(IP_ADDRESS, MODBUS_PORT, timeout=3)
    client.connect()
    if data_type == 'voltage':
        for i in range(len(data_input)):
            builder = BinaryPayloadBuilder(byteorder=Endian.Big,wordorder=Endian.Big)
            # builder.add_16bit_int(int(data_input[i]*10))
            builder.add_32bit_float(data_input[i])
            payload = builder.build()
            client.write_registers(VOLTAGE_PORTS[i], payload,skip_encode=True, unit = 1)
    client.close()
    print('Writing to Register Completed.')


slack_bus_voltage = 1.02
for itr in range(10000):
    
    dss.run_command('Compile ' + 'IEEE13Nodeckt.dss')  # redirecting to the model
    # print (os.getcwd())
    dss.Vsources.PU(slack_bus_voltage)  # setting up the slack bus voltage
    random_multiplier = random()
    print(f'Running Power Flow with multiplier : {random_multiplier}')
    dss.Solution.LoadMult(random_multiplier)
    # Setting up the solution parameters, check OpenDSS documentation for details
    dss.Solution.Solve()
    dss.Circuit.SetActiveBus('671')
    x=dss.Bus.Voltages()
        # print (x)
    voltage_reading=[]
    for i in range(0,len(x),2):
        voltage_reading.append(dss.CmathLib.cabs(x[i],x[i+1]))
    print(voltage_reading)
    update_register(data_input = voltage_reading)
    time.sleep(75)


# System.out.println(ModbusClient.ConvertRegistersToFloat(modbusClient.ReadHoldingRegisters(0, 2), ModbusClient.RegisterOrder.HighLow));