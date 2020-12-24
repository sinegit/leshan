package org.eclipse.leshan.client.demo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
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

    private static final int RESET_COIL_ADDRESS = 0;
    // private static final int SENSOR_VALUE = 5700;
    // private static final int SENSOR_VALUE_KELVIN = 6051;
    // private static final int UNITS = 6052;
    // private static final int MAX_MEASURED_VALUE = 5602;
    // private static final int MIN_MEASURED_VALUE = 5601;
    // private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(VOLTAGE, TIMESTAMP, RESET_DEVICE);
    private final ScheduledExecutorService scheduler;
    private Date current_timestamp = new Date();
    private ModbusClient modbusClient = new ModbusClient("127.0.0.1", 502);
    private int[] voltage_register_address = { 0, 4, 8 };
    private String voltage_data;
    SimpleDateFormat date_format = new SimpleDateFormat ("yyyy.MM.dd::hh:mm:ss");


    public PowerMonitor() {

        try {
            modbusClient.Connect();
        } catch (IOException e) {
            LOG.info("CANT CONNECT TO MODBUS CLIENT");
            e.printStackTrace();
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Temperature Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                updateTimeStamp();
                UpdateVoltage(true);
                
            }
        }, 2, 300, TimeUnit.SECONDS);
    }

    private String UpdateVoltage(Boolean write_to_file) {
        
        voltage_data = "";
        try {
            for (int reg : voltage_register_address) {
                voltage_data  = voltage_data +  String.valueOf(ModbusClient.ConvertRegistersToFloat(modbusClient.ReadHoldingRegisters(reg, 2),
                ModbusClient.RegisterOrder.HighLow)) + "_";
            }
            StringUtils.chop(voltage_data);
            if (write_to_file) 
            {
                LOG.info("Attempting to Write to File");
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
        LOG.info("Read on Temperature resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case VOLTAGE:
            LOG.info("Voltage Reading : {}", getVoltage());
            return ReadResponse.success(resourceId, getVoltage());
        case TIMESTAMP:
            return ReadResponse.success(resourceId, getTimeStamp());
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

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }
}
