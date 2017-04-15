/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.smlreader.connectors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;

import org.openmuc.jsml.structures.SML_File;
import org.openmuc.jsml.structures.SML_Message;
import org.openmuc.jsml.tl.SMLMessageExtractor;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import gnu.io.factory.DefaultSerialPortFactory;

/**
 * Represents a serial SML device connector.
 *
 * @author Mathias Gilhuber
 * @since 1.7.0
 */
final public class SerialConnector extends ConnectorBase {
    SerialPort serialPort;
    InputStream inputStream;
    DataInputStream is;

    /**
     * The name of the port where the device is connected as defined in openHAB configuration.
     */
    private String portName;

    /**
     * Contructor to create a serial connector instance.
     *
     * @param portName the port where the device is connected as defined in openHAB configuration.
     */
    public SerialConnector(String portName) {
        super();
        this.portName = portName;
    }

    /**
     * @throws IOException
     * @throws ConnectorException
     * @{inheritDoc}
     */
    @Override
    protected SML_File getMeterValuesInternal() throws IOException {
        SML_File smlFile = null;

        SMLMessageExtractor extractor;

        try {
            extractor = new SMLMessageExtractor(is, 5000);
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(extractor.getSmlMessage()));

            smlFile = new SML_File();

            while (is.available() > 0) {
                SML_Message message = new SML_Message();

                if (!message.decode(is)) {
                    throw new IOException("Could not decode message");
                } else {
                    smlFile.add(message);
                }
            }
        } catch (IOException e) {
            logger.error("Error at SerialConnector.getMeterValuesInternal: {}", e.getMessage());
            throw e;
        }

        return smlFile;
    }

    /**
     * @throws IOException
     * @{inheritDoc}
     */
    @Override
    protected void openConnection() throws IOException {
        // CommPortIdentifier portId = getCommPortIdentifier();
        DefaultSerialPortFactory serialPortFactory = new DefaultSerialPortFactory();
        // if (portId != null) {
        try {
            serialPort = serialPortFactory.createSerialPort(portName);
            // serialPort = portId.open("SmlReaderBinding", 2000);
            serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            serialPort.notifyOnDataAvailable(true);
            is = new DataInputStream(new BufferedInputStream(serialPort.getInputStream()));
        } catch (PortInUseException e) {
            throw new IOException(MessageFormat
                    .format("Error at SerialConnector.openConnection: port {} is already in use.", this.portName), e);
        } catch (UnsupportedCommOperationException e) {
            throw new IOException(MessageFormat.format(
                    "Error at SerialConnector.openConnection: params for port {} are not supported.", this.portName),
                    e);
        } catch (IOException e) {
            throw new IOException(MessageFormat.format(
                    "Error at SerialConnector.openConnection: unable to get inputstream for port {}.", this.portName),
                    e);
        } catch (NoSuchPortException e) {
            throw new IOException(MessageFormat
                    .format("Error at SerialConnector.openConnection: serial port not found {}.", this.portName), e);
        }
        // }
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void closeConnection() {
        try {
            if (is != null) {
                is.close();
            }
            if (serialPort != null) {
                serialPort.close();
            }

        } catch (Exception e) {
            logger.error("Error at SerialConnector.closeConnection", e);
        }
    }

    /**
     * Searches and returns the specified port identifier.
     */
    private CommPortIdentifier getCommPortIdentifier() {
        CommPortIdentifier commPort = null;
        Enumeration<?> portList;

        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            commPort = (CommPortIdentifier) portList.nextElement();
            if (commPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (commPort.getName().equals(this.portName)) {
                    break;
                }
            }
        }

        return commPort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((portName == null) ? 0 : portName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SerialConnector other = (SerialConnector) obj;
        if (portName == null) {
            if (other.portName != null) {
                return false;
            }
        } else if (!portName.equals(other.portName)) {
            return false;
        }
        return true;
    }

}
