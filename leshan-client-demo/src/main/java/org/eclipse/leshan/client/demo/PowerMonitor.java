package org.eclipse.leshan.client.demo;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    
    private static final int SENSOR_VALUE = 5700;
    private static final int SENSOR_VALUE_KELVIN = 6051;
    private static final int UNITS = 6052;
    private static final int MAX_MEASURED_VALUE = 5602;
    private static final int MIN_MEASURED_VALUE = 5601;
    private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, SENSOR_VALUE_KELVIN,UNITS, MAX_MEASURED_VALUE,
            MIN_MEASURED_VALUE, RESET_MIN_MAX_MEASURED_VALUES);
    private final ScheduledExecutorService scheduler;
    private final Random rng = new Random();
    private double currentTemp = 20d;
    private double currentTemp_kelvin = 20d;
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;
    private Date timestamp = new Date();
    private ModbusClient modbusClient = new ModbusClient("127.0.0.1",502);
    private int[] register_address = {0,4,8};

   
    public PowerMonitor() {
        
    	try {
			modbusClient.Connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Temperature Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                adjustTemperature();
                adjustKelvinTemperature();
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on Temperature resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case MIN_MEASURED_VALUE:
            LOG.info("Min Measured Value / {}", getTestValue());
            return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
        case MAX_MEASURED_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp_kelvin));
        case SENSOR_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
        case SENSOR_VALUE_KELVIN:
            LOG.info("Sensor Value in KELVIN / {}", getTwoDigitValue(currentTemp_kelvin));
            return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp_kelvin));
        case UNITS:
            return ReadResponse.success(resourceId, getTimeStamp());
        default:
            return super.read(identity, resourceId);
        }
    }
    
    private String getTestValue() 
    {
    	
    	try {
			return String.valueOf(modbusClient.ReadHoldingRegisters(0, 1)[0]);
		} catch (ModbusException | IOException e1) {
			
			e1.printStackTrace();
		}
    	return "";
    }
    
    private String getTimeStamp()
    {
        return new Date().toString();
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        LOG.info("Execute on Temperature resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case RESET_MIN_MAX_MEASURED_VALUES:
            resetMinMaxMeasuredValues();
            return ExecuteResponse.success();
        default:
            return super.execute(identity, resourceId, params);
        }
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustTemperature() {
        float delta = (rng.nextInt(20) - 10) / 10f;
        currentTemp += delta;

        Integer changedResource = adjustMinMaxMeasuredValue(currentTemp);
        if (changedResource != null) {
            fireResourcesChange(SENSOR_VALUE, changedResource);
        } else {
            fireResourcesChange(SENSOR_VALUE);
        }
    }

    private void adjustKelvinTemperature()
    {
        currentTemp_kelvin = currentTemp + 2;
        fireResourcesChange(SENSOR_VALUE_KELVIN);
    }


    private synchronized Integer adjustMinMaxMeasuredValue(double newTemperature) {
        if (newTemperature > maxMeasuredValue) {
            maxMeasuredValue = newTemperature;
            return MAX_MEASURED_VALUE;
        } else if (newTemperature < minMeasuredValue) {
            minMeasuredValue = newTemperature;
            return MIN_MEASURED_VALUE;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentTemp;
        maxMeasuredValue = currentTemp;
        fireResourcesChange(MIN_MEASURED_VALUE, MAX_MEASURED_VALUE);
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
