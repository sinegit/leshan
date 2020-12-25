from LeshanRestAPI import Client, Server
import time
from datetime import datetime
from dateutil.parser import parse
import yaml
from Naked.toolshed.shell import execute_js, muterun_js
import requests

global data_server

data_server = Server("http://10.203.53.254:8080")

prior_timestamp = ""
def read_client(client_name: str):
    
    resp = requests.get("http://10.203.53.254:8080/api/clients/"+client_name+"/40000/0/6052")
    content = resp.json()
    output_data = {'timestamp': content["content"]["value"]}
    resp = requests.get("http://10.203.53.254:8080/api/clients/"+client_name+"/40000/0/6051")
    content = resp.json()
    output_data ['voltage']  = content["content"]["value"]
    return output_data

if __name__ == "__main__":

    with open ('config.yaml') as f: 
        aggregator_config= yaml.safe_load(f)
        path = './' + aggregator_config['Name'][0]
        channel_name = aggregator_config['ChannelName'][0]
        sensor_list = aggregator_config['SensorList']
          
    while True:

        for sensor in sensor_list:
            client_list = data_server.getClients()
            if sensor in client_list:
    
                output_data = read_client(sensor)
                arguments_to_pass = sensor + " " +  str(output_data ['timestamp']) + " " +   str(output_data ['voltage'])
                
                response=execute_js('./submitMeasurement.js',arguments= arguments_to_pass)
                
#                    prior_timestamp_data[sensor] = output_data['time_stamp']
        time.sleep(300)
