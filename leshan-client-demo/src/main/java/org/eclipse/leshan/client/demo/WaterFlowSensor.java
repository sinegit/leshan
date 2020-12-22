package org.eclipse.leshan.client.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.sql.SQLException;
import java.sql.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.Base64;
import java.util.UUID;
import java.io.UnsupportedEncodingException;

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

import jdk.internal.org.jline.utils.Log;

public class WaterFlowSensor extends BaseInstanceEnabler implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(WaterFlowSensor.class);

    private static final int INTERVAL_PERIOD = 6000;
    private static final int INTERVAL_START_OFFSET = 6001;
    private static final int INTERVAL_UTC_OFFSET = 6002;
    private static final int INTERVAL_COLLECTION_START_TIME = 6003;
    private static final int OLDEST_RECORDED_INTERVAL = 6004;
    private static final int LAST_DELIVERED_INTERVAL = 6005;
    private static final int LATEST_RECORDED_INTERVAL = 6006;
    private static final int INTERVAL_HISTORICAL_READ = 6008;
    private static final int INTERVAL_HISTORICAL_READ_PAYLOAD = 6009;
    private static final int INTERVAL_CHANGE_CONFIGURATION = 6010;
    private static final int START = 6026;
    private static final int STOP = 6027;
    private static final int STATUS = 6028;
    private static final int LATEST_PAYLOAD = 6029;

    private static final List<Integer> supportedResources = Arrays.asList(INTERVAL_PERIOD, INTERVAL_START_OFFSET,
            INTERVAL_UTC_OFFSET, INTERVAL_COLLECTION_START_TIME, OLDEST_RECORDED_INTERVAL, LAST_DELIVERED_INTERVAL,
            LATEST_RECORDED_INTERVAL, INTERVAL_HISTORICAL_READ, INTERVAL_HISTORICAL_READ_PAYLOAD,
            INTERVAL_CHANGE_CONFIGURATION, START, STOP, STATUS, LATEST_PAYLOAD);
    private final ScheduledExecutorService scheduler;
    private final Random rng = new Random();

    private int intervalPeriod = 3;
    private int intervalStartOffset = 0;
    private Date lastDeliveredInterval = new Date();
    private Date collectionStartTime = new Date();
    private Date oldestRecordedInterval = new Date();
    private Timestamp oldestRecordedIntervalTS = new Timestamp(oldestRecordedInterval.getTime());
    private Date latestRecordedInterval = new Date();
    private boolean recording = false;
    private String intervalHistoricalReadPayload = Base64.getUrlEncoder().encodeToString("Nothing here yet".getBytes());
    private String latestPaylod = Base64.getUrlEncoder().encodeToString("Nothing here yet".getBytes());

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on Water Flow resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case INTERVAL_PERIOD:
                return ReadResponse.success(resourceId, intervalPeriod);
            case INTERVAL_START_OFFSET:
                return ReadResponse.success(resourceId, intervalStartOffset);
            case INTERVAL_UTC_OFFSET:
                return ReadResponse.success(resourceId, getUtcOffset());
            case INTERVAL_COLLECTION_START_TIME:
                return ReadResponse.success(resourceId, collectionStartTime);
            case OLDEST_RECORDED_INTERVAL:
                return ReadResponse.success(resourceId, oldestRecordedIntervalTS);
            case LAST_DELIVERED_INTERVAL:
                return ReadResponse.success(resourceId, lastDeliveredInterval);
            case LATEST_RECORDED_INTERVAL:
                return ReadResponse.success(resourceId, latestRecordedInterval);
            case INTERVAL_HISTORICAL_READ_PAYLOAD:
                return ReadResponse.success(resourceId, intervalHistoricalReadPayload);
            case STATUS:
                return ReadResponse.success(resourceId, recording);
            case LATEST_PAYLOAD:
            try {
                return ReadResponse.success(resourceId,getLatestPayload());
            } catch (SQLException e) {
                return ReadResponse.success(resourceId,Base64.getUrlEncoder().encode("".getBytes()));
                }
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        String withParams = null;
        if (params != null && params.length() != 0)
            withParams = " with params " + params;
        LOG.info("Execute on Device resource /{}/{}/{} {}", getModel().id, getId(), resourceId,
                withParams != null ? withParams : "");
        switch (resourceId) {
            case INTERVAL_HISTORICAL_READ:
                try {
                    setupIntervalHistoricalRead(params);
                    return ExecuteResponse.success();
                } catch (SQLException e) {
                    return ExecuteResponse.badRequest(e.toString());
                }
            case INTERVAL_CHANGE_CONFIGURATION:
                LOG.info("{}", params);
                setIntervalChangeConfiguration(params);
                return ExecuteResponse.success();
            case START:
                startRecording();
                return ExecuteResponse.success();
            case STOP:
                stopRecording();
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, params);
        }
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        LOG.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
            case LAST_DELIVERED_INTERVAL:
                setLastDeliveredInterval((Date) value.getValue());
                fireResourcesChange(resourceid);
                return WriteResponse.success();
            default:
                return super.write(identity, resourceid, value);
        }
    }

    private Calendar startTime = Calendar.getInstance();

    private void setIntervalStartOffset() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long ms = startTime.getTime().getTime() - midnight.getTime().getTime();
        intervalStartOffset = (int) (ms / 1000);
    }

    private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

    private String getUtcOffset() {
        return utcOffset;
    }

    private void setCollectionStartTime(long newStartTime) {
        collectionStartTime.setTime(newStartTime);
        fireResourcesChange(INTERVAL_COLLECTION_START_TIME);
    }

    private void setLastDeliveredInterval(Date newLastDeliveredInterval) {
        lastDeliveredInterval = newLastDeliveredInterval;
        fireResourcesChange(LAST_DELIVERED_INTERVAL);
    }

    private void setupIntervalHistoricalRead(String interval) throws SQLException {
        String[] dates = interval.split(",");
        Timestamp starttime = Timestamp.valueOf(dates[0]);
        Timestamp stoptime = Timestamp.valueOf(dates[1]);
        intervalHistoricalReadPayload = Base64.getUrlEncoder().encodeToString(waterflowDB.getWaterFlowsBetweenA_B(starttime, stoptime).getBytes());
        fireResourcesChange(INTERVAL_HISTORICAL_READ_PAYLOAD);
    }

    private void setIntervalChangeConfiguration(String params) {        
        String[] param = params.split(",");
        intervalPeriod = Integer.parseInt(param[0]);
        fireResourcesChange(INTERVAL_PERIOD);
        if (params.length() == 2) {
            intervalStartOffset = Integer.parseInt(param[1]);
            fireResourcesChange(INTERVAL_START_OFFSET);
            if (params.length() == 3) {
                utcOffset = param[2];
                fireResourcesChange(INTERVAL_UTC_OFFSET);
            }
        }
    }

    private void startRecording() {
        recording = true;
        fireResourcesChange(STATUS);
    }

    private void stopRecording() {
        recording = false;
        fireResourcesChange(STATUS);
    }

    private String getLatestPayload() throws SQLException {
        Timestamp starttime = new Timestamp(lastDeliveredInterval.getTime());
        Timestamp stoptime = new Timestamp(latestRecordedInterval.getTime());
        latestPaylod = Base64.getUrlEncoder().encodeToString(waterflowDB.getWaterFlowsBetweenA_B(starttime, stoptime).getBytes());
        setLastDeliveredInterval(latestRecordedInterval);
        LOG.info("Delivered payload between {} and {}", starttime, stoptime);
        return latestPaylod;
    }
    
    private synchronized void setLatestRecordedInterval(Date newlatestRecordedInterval) {
        latestRecordedInterval = newlatestRecordedInterval;        
        fireResourcesChange(LATEST_RECORDED_INTERVAL);
    }

    public WaterFlowSensor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Water Flow Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {
            // TODO: Cancel task and reschedule when intervalPeriod changes
            @Override
            public void run() {
                try {
                    newWaterFlowMeasurement();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0, intervalPeriod, TimeUnit.SECONDS);
    }

    private void newWaterFlowMeasurement() throws SQLException {
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        float value = rng.nextInt(20) / 10f;
        waterflowDB.addnewmeasurement(ts, value);
        setLatestRecordedInterval(date);
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
