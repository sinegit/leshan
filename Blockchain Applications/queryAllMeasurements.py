from Naked.toolshed.shell import execute_js, muterun_js
import re
import yaml
import json
import ast
import argparse
import matplotlib.pyplot as plt

parser = argparse.ArgumentParser(description='Process some integers.')
parser.add_argument('--s', metavar='N', type=str, default= "")

args = parser.parse_args()
sensor = args.s



try:
  with open ('config.yaml') as f:
    aggregator_config = yaml.safe_load(f)
    sensor_list = aggregator_config['SensorList']
    if sensor != "" and sensor not in sensor_list:
      print('Sensor Name is not part of the sensor list')
      raise SystemExit
except FileNotFoundError:
  print('Configuration File Not Found')
  raise SystemExit
  
response = muterun_js('./queryAllMeasurement.js')

if response.exitcode == 0:
    result = response.stdout.decode("utf-8")
    # result = result.decode("utf-8")
    if sensor == "":
      all_saved_data = dict.fromkeys(sensor_list,{})
    else:
      all_saved_data = {sensor:{}}
    
    input_data = result.split('|')
    final_data = {}
    try:
      for data in input_data:
          # print ('Length of Data:',len(data))
            if len(data)>1:
                element,time_stamp, value = data.split('::')[0],data.split('::')[1],data.split('::')[2]
                # print(value)
                
                measurement = [str(i) for i in value.split('_')]
                final_data[time_stamp] = measurement
                # if sensor != "":
                #   if sensor == element:
                #     all_saved_data[element][time_stamp] = measurement
                # else:
                all_saved_data[element][time_stamp] = measurement
                # final_data.append(parsed_data)

      if sensor != "":
        print ('Number of Measurements Collected:',len(all_saved_data[sensor]))
        print(all_saved_data[sensor])
      else: 
        total_number = [len(v) for _,v in all_saved_data.items()]
        print ('Number of Measurements Collected:',sum(total_number))
        print(all_saved_data)
    except:
      print('Data Handling Error')

phase_a_voltage,phase_b_voltage,phase_c_voltage = [],[],[]

for key,value in all_saved_data[sensor].items():
  phase_a_voltage.append(float(value[0]))
  phase_b_voltage.append(float(value[1]))
  phase_c_voltage.append(float(value[2]))

plt.figure()
plt.subplot(311)
plt.plot(phase_a_voltage)
plt.ylabel("Phase A Voltage")
plt.subplot(312)
plt.plot(phase_b_voltage)
plt.ylabel("Phase B Voltage")
plt.subplot(313)
plt.plot(phase_c_voltage)
plt.ylabel("Phase C Voltage")
plt.show()






