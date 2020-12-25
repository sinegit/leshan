package org.eclipse.leshan.client.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.scandium.dtls.MaxFragmentLengthExtension.Length;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.re.easymodbus.exceptions.ModbusException;
import de.re.easymodbus.modbusclient.*;

public class PowerMonitor extends BaseInstanceEnabler implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(PowerMonitor.class);
    
    private static final int VOLTAGE = 6051;
    private static final int TIMESTAMP = 6052; 
    private static final int RESET_DEVICE = 6053;
    private static final int MEASUREMENT_RESOLUTION = 6054 ; 

    private static final int RESET_COIL_ADDRESS = 0;
    // private static final int SENSOR_VALUE = 5700;
    // private static final int SENSOR_VALUE_KELVIN = 6051;
    // private static final int UNITS = 6052;
    // private static final int MAX_MEASURED_VALUE = 5602;
    // private static final int MIN_MEASURED_VALUE = 5601;
    // private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(VOLTAGE, TIMESTAMP, RESET_DEVICE, MEASUREMENT_RESOLUTION);
    private final ScheduledExecutorService scheduler;
    private Date current_timestamp = new Date();
    private ModbusClient modbusClient = new ModbusClient("127.0.0.1", 502);
    private int[] voltage_register_address = { 0, 4, 8 };
    private String voltage_data;
    SimpleDateFormat date_format = new SimpleDateFormat ("yyyy.MM.dd_hh.mm.ss");
    private static final String filePath = "C:/Github/leshan/leshan-client-demo/data/PowerMonitor.csv";
    private int time_resolution = 300;

    public PowerMonitor() {

        try {
            modbusClient.Connect();
        } catch (IOException e) {
            LOG.info("CANT CONNECT TO MODBUS CLIENT");
            e.printStackTrace();
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Power Monitor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updateTimeStamp();
                UpdateVoltage(true);
                fireResourcesChange(TIMESTAMP, VOLTAGE);
                
            }
        }, 2, time_resolution, TimeUnit.SECONDS);
    }

    private String UpdateVoltage(Boolean write_to_file) {
        
        voltage_data = "";
        try {
            for (int reg : voltage_register_address) {
                voltage_data  = voltage_data +  String.valueOf(ModbusClient.ConvertRegistersToFloat(modbusClient.ReadHoldingRegisters(reg, 2),
                ModbusClient.RegisterOrder.HighLow)) + "_";
            }
            voltage_data =  voltage_data.substring(0, voltage_data.length()-1);
            if (write_to_file) 
            {
                writeDataLine(filePath, date_format.format(current_timestamp) , voltage_data);
            }
            return voltage_data;
            
        } catch (IllegalArgumentException | ModbusException | IOException e) {
            LOG.info("ERROR in updating voltage");
            e.printStackTrace();
        }
        return "0_0_0";
    }

    private String getVoltage()
    {
        try{
            return UpdateVoltage(false);
        } catch (Exception e)
        {
            e.printStackTrace();
            return "0_0_0";
        }
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on Power Monitor resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case VOLTAGE:
            LOG.info("Voltage Reading : {}", getVoltage());
            return ReadResponse.success(resourceId, getVoltage());
        case TIMESTAMP:
            return ReadResponse.success(resourceId, getTimeStamp());
        case MEASUREMENT_RESOLUTION:
            return ReadResponse.success(resourceId,time_resolution);
        default:
            return super.read(identity, resourceId);
        }
    }
    
 
    private Date updateTimeStamp()
    {
        current_timestamp = new Date();
        return current_timestamp;
    }

    private String getTimeStamp() { return date_format.format(updateTimeStamp()); }


    private boolean resetDevice()
    {
        try {
            modbusClient.WriteSingleCoil(RESET_COIL_ADDRESS, true);
        } catch (ModbusException | IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        LOG.info("Execute Reseting the device /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case RESET_DEVICE:
            resetDevice();
            return ExecuteResponse.success();

        default:
            return super.execute(identity, resourceId, params);
        }
    }

    // @Override
    // public WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
    //     LOG.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceid);

    //     switch (resourceid) {
    //     case MEASUREMENT_RESOLUTION:
    //         time_resolution = ((Number)value.getValue()).intValue();
    //         fireResourcesChange(MEASUREMENT_RESOLUTION);
    //     default:
    //         return super.write(identity, resourceid, value);
    //     }
    // }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }

    public void writeDataLine(String filePath, String time, String value) {

        // first create file object for file placed at location
        // specified by filepath
        File file = new File(filePath);
        FileWriter fileWriter = null;
        try {
            // create FileWriter object with file as parameter
            fileWriter = new FileWriter(file, true);
            fileWriter.append(time);
            fileWriter.append(',');
            fileWriter.append(value);
            fileWriter.append('\n');
        } catch (IOException e) {
            LOG.info("File Not Found");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                LOG.info("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}
