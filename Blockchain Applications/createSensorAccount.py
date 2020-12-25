import json
import os
from Naked.toolshed.shell import execute_js, muterun_js
import time
import yaml


with open ('config.yaml') as f: 
    aggregator_config= yaml.safe_load(f)
    path = './' + aggregator_config['Name'][0]
    channel_name = aggregator_config['ChannelName'][0]
    sensor_list = aggregator_config['SensorList']
    print(sensor_list)
    for sensor in sensor_list:
        arguments_to_pass = sensor + " " +  sensor
        response=execute_js('./registerUser.js',arguments= arguments_to_pass)

    



