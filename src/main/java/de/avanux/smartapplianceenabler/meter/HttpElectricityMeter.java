/*
 * Copyright (C) 2017 Axel Müller <axel.mueller@avanux.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package de.avanux.smartapplianceenabler.meter;

import de.avanux.smartapplianceenabler.appliance.ApplianceIdConsumer;
import de.avanux.smartapplianceenabler.http.HttpMethod;
import de.avanux.smartapplianceenabler.http.HttpRead;
import de.avanux.smartapplianceenabler.http.HttpReadValue;
import de.avanux.smartapplianceenabler.http.HttpValidator;
import de.avanux.smartapplianceenabler.protocol.ContentProtocolHandler;
import de.avanux.smartapplianceenabler.protocol.ContentProtocolType;
import de.avanux.smartapplianceenabler.protocol.JsonContentProtocolHandler;
import de.avanux.smartapplianceenabler.util.Initializable;
import de.avanux.smartapplianceenabler.util.ParentWithChild;
import de.avanux.smartapplianceenabler.util.Validateable;
import de.avanux.smartapplianceenabler.util.ValueExtractor;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Electricity meter reading current power and energy from the response of a HTTP request.
 * <p>
 * IMPORTANT: URLs have to be escaped (e.g. use "&amp;" instead of "&")
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class HttpElectricityMeter implements Meter, Initializable, Validateable, PollPowerExecutor, PollEnergyExecutor,
        ApplianceIdConsumer {

    private transient Logger logger = LoggerFactory.getLogger(HttpElectricityMeter.class);
    @XmlAttribute
    private Integer measurementInterval; // seconds
    @XmlAttribute
    private Integer pollInterval; // seconds
    @XmlAttribute
    private String contentProtocol;
    @XmlElement(name = "HttpRead")
    private List<HttpRead> httpReads;
    private transient String applianceId;
    private transient PollPowerMeter pollPowerMeter = new PollPowerMeter();
    private transient PollEnergyMeter pollEnergyMeter = new PollEnergyMeter();
    private transient ValueExtractor valueExtractor = new ValueExtractor();
    private transient ContentProtocolHandler contentContentProtocolHandler;


    @Override
    public void setApplianceId(String applianceId) {
        this.applianceId = applianceId;
        this.pollPowerMeter.setApplianceId(applianceId);
        this.pollEnergyMeter.setApplianceId(applianceId);
        if(this.httpReads != null) {
            for(HttpRead httpRead: this.httpReads) {
                httpRead.setApplianceId(applianceId);
            }
        }
    }

    public List<HttpRead> getHttpReads() {
        return httpReads;
    }

    public void setHttpReads(List<HttpRead> httpReads) {
        this.httpReads = httpReads;
    }

    public void setContentProtocol(ContentProtocolType contentProtocolType) {
        this.contentProtocol = contentProtocolType != null ? contentProtocolType.name() : null;
    }

    @Override
    public Integer getMeasurementInterval() {
        return measurementInterval != null ? measurementInterval : HttpElectricityMeterDefaults.getMeasurementInterval();
    }

    public void setMeasurementInterval(Integer measurementInterval) {
        this.measurementInterval = measurementInterval;
    }

    public Integer getPollInterval() {
        return pollInterval != null ? pollInterval : HttpElectricityMeterDefaults.getPollInterval();
    }

    protected PollPowerMeter getPollPowerMeter() {
        return pollPowerMeter;
    }

    protected PollEnergyMeter getPollEnergyMeter() {
        return pollEnergyMeter;
    }

    @Override
    public void validate() {
        HttpValidator validator = new HttpValidator(applianceId);

        // Meter should meter either Power or Energy or both
        boolean powerValid = validator.validateReads(Collections.singletonList(MeterValueName.Power.name()), this.httpReads);
        boolean energyValid = validator.validateReads(Collections.singletonList(MeterValueName.Energy.name()), this.httpReads);
        if(! (powerValid || energyValid)) {
            logger.error("{}: Terminating because of incorrect configuration", applianceId);
            System.exit(-1);
        }
    }

    @Override
    public int getAveragePower() {
        int power = pollPowerMeter.getAveragePower();
        logger.debug("{}: average power = {}W", applianceId, power);
        return power;
    }

    @Override
    public int getMinPower() {
        int power = pollPowerMeter.getMinPower();
        logger.debug("{}: min power = {}W", applianceId, power);
        return power;
    }

    @Override
    public int getMaxPower() {
        int power = pollPowerMeter.getMaxPower();
        logger.debug("{}: max power = {}W", applianceId, power);
        return power;
    }

    @Override
    public float getEnergy() {
        return this.pollEnergyMeter.getEnergy();
    }

    @Override
    public void startEnergyMeter() {
        logger.debug("{}: Start energy meter ...", applianceId);
        Float energy = this.pollEnergyMeter.startEnergyCounter();
        logger.debug("{}: Current energy meter value: {} kWh", applianceId, energy);
    }

    @Override
    public void stopEnergyMeter() {
        logger.debug("{}: Stop energy meter ...", applianceId);
        Float energy = this.pollEnergyMeter.stopEnergyCounter();
        logger.debug("{}: Current energy meter value: {} kWh", applianceId, energy);
    }

    @Override
    public void resetEnergyMeter() {
        logger.debug("{}: Reset energy meter ...", applianceId);
        this.pollEnergyMeter.resetEnergyCounter();
    }

    @Override
    public boolean isOn() {
        return pollPower() > 0;
    }

    @Override
    public void init() {
        this.pollEnergyMeter.setPollEnergyExecutor(this);
    }

    @Override
    public void start(Timer timer) {
        logger.debug("{}: Starting ...", applianceId);
        pollEnergyMeter.start(timer, getPollInterval(), getMeasurementInterval(), this);
        pollPowerMeter.start(timer, getPollInterval(), getMeasurementInterval(), this);
    }

    @Override
    public void stop() {
        logger.debug("{}: Stopping ...", applianceId);
        pollEnergyMeter.cancelTimer();
        pollPowerMeter.cancelTimer();
    }

    private boolean isCalculatePowerFromEnergy() {
        return HttpRead.getFirstHttpRead(MeterValueName.Power.name(), this.httpReads) == null;
    }

    /**
     * Calculates power values from energy differences and time differences.
     * @param timestampWithEnergyValue a collection of timestamp with energy value
     * @return the power values in W
     */
    protected Vector<Float> calculatePower(TreeMap<Long, Float> timestampWithEnergyValue) {
        Vector<Float> powerValues = new Vector<>();
        Long previousTimestamp = null;
        Float previousEnergy = null;
        for(Long timestamp: timestampWithEnergyValue.keySet()) {
            Float energy = timestampWithEnergyValue.get(timestamp);
            if(previousTimestamp != null && previousEnergy != null) {
                long diffTime = timestamp - previousTimestamp;
                Float diffEnergy = energy - previousEnergy;
                // diffEnergy kWh * 1000W/kW * 3600000ms/1h / diffTime ms
                float power = diffEnergy * 1000.0f * 3600000.0f / diffTime;
                powerValues.add(power > 0 ? power : 0.0f);
            }
            previousTimestamp = timestamp;
            previousEnergy = energy;
        }
        return powerValues;
    }

    @Override
    public Float pollPower() {
        ParentWithChild<HttpRead, HttpReadValue> powerRead = HttpRead.getFirstHttpRead(MeterValueName.Power.name(), this.httpReads);
        if(powerRead != null) {
            return getValue(powerRead);
        }
        Vector<Float> powerValues = calculatePower(this.pollEnergyMeter.getValuesInMeasurementInterval());
        if(powerValues.size() > 0) {
            Float power = powerValues.lastElement();
            logger.debug("{}: Calculated power from energy: {}W", applianceId, power);
            return power;
        }
        return null;
    }

    @Override
    public Float pollEnergy(LocalDateTime now) {
        return pollEnergy(now.toDateTime().getMillis());
    }

    protected float pollEnergy(long timestamp) {
        ParentWithChild<HttpRead, HttpReadValue> energyRead = HttpRead.getFirstHttpRead(MeterValueName.Energy.name(), this.httpReads);
        return getValue(energyRead);
    }

    private float getValue(ParentWithChild<HttpRead, HttpReadValue> read) {
        if(read != null) {
            String url = read.parent().getUrl();
            String data = read.child().getData();
            HttpMethod httpMethod = data != null ? HttpMethod.POST : HttpMethod.GET;
            String path = read.child().getPath();
            String valueExtractionRegex = read.child().getExtractionRegex();
            Double factorToValue = read.child().getFactorToValue();
            String response = read.parent().execute(httpMethod, url, data);
            logger.debug("{}: url={} httpMethod={} data={} path={} valueExtractionRegex={} factorToValue={}",
                    applianceId, url, httpMethod, data, path, valueExtractionRegex, factorToValue);
            if(response != null) {
                logger.debug("{}: Response: {}", applianceId, response);
                String protocolHandlerValue = response;
                ContentProtocolHandler contentProtocolHandler = getContentContentProtocolHandler();
                if(contentProtocolHandler != null) {
                    contentProtocolHandler.parse(response);
                    protocolHandlerValue = contentProtocolHandler.readValue(path);
                }
                String extractedValue = valueExtractor.extractValue(protocolHandlerValue, valueExtractionRegex);
                String parsableString = extractedValue.replace(',', '.');
                Float value = null;
                if(factorToValue != null) {
                    value = Double.valueOf(Double.parseDouble(parsableString) * factorToValue).floatValue();
                }
                else {
                    value = Double.valueOf(Double.parseDouble(parsableString)).floatValue();
                }
                logger.debug("{}: value={} contentProtocolHandler={} extracted={}",
                        applianceId, value, protocolHandlerValue, extractedValue);
                return value;
            }
        }
        return 0.0f;
    }

    public ContentProtocolHandler getContentContentProtocolHandler() {
        if(this.contentContentProtocolHandler == null) {
            if(ContentProtocolType.json.name().equals(this.contentProtocol)) {
                this.contentContentProtocolHandler = new JsonContentProtocolHandler();
            }
        }
        return this.contentContentProtocolHandler;
    }
}
