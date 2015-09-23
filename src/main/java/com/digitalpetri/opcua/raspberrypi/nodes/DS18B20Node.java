/*
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpetri.opcua.raspberrypi.nodes;

import com.digitalpetri.opcua.raspberrypi.GpioConfig.InputConfig;
import com.digitalpetri.opcua.raspberrypi.PiNamespace;
import com.digitalpetri.opcua.sdk.server.api.UaNamespace;
import com.digitalpetri.opcua.sdk.server.model.UaVariableNode;
import com.digitalpetri.opcua.stack.core.Identifiers;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.LocalizedText;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.QualifiedName;
import com.digitalpetri.opcua.stack.core.types.builtin.Variant;
import com.digitalpetri.opcua.stack.core.types.builtin.unsigned.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.*;
import java.util.List;

public class DS18B20Node extends UaVariableNode {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Thread fileReaderThread = null;
    private Path dataFilePath = null;

    public DS18B20Node(UaNamespace nodeManager,
                           NodeId nodeId,
                           QualifiedName browseName,
                           LocalizedText displayName,
                           InputConfig inputConfig) {

        super(nodeManager, nodeId, browseName, displayName);

        // Data type is double, which is degrees in C
        double value = 0;
        setDataType(Identifiers.Double);
        setValue(new DataValue(new Variant(value)));

        // Find folder the data is in. Should start with "28-XXXXX..."
        File file = new File("/sys/bus/w1/devices");
        File[] directories = file.listFiles();
        for(int i=0; i<directories.length; i++){
            if(directories[i].getName().indexOf("28-") != -1 && directories[i].isDirectory()){
                this.dataFilePath = Paths.get(directories[i].toPath() + "/w1_slave");
            }
        }

        if(this.dataFilePath != null) {
            // Kick off a thread
            this.fileReaderThread = new Thread((new DataThread(this)));
            this.fileReaderThread.start();
        }else{
            logger.error("Failed to find DS18B20 sensor file in '{}'. Make sure the sensor is connected.", "/sys/bus/w1/devices");
        }
    }

    private class DataThread implements Runnable {
        private UaVariableNode node;
        public DataThread (UaVariableNode node)
        {
        this.node = node;
        }

        public void run ()
        {
            while(true){
                try {
                    List<String> strLines = Files.readAllLines(dataFilePath);
                    if(strLines.size() == 2)
                    {
                        // Find the t= text and pull the number
                        String strData = strLines.get(1);
                        int nIndex = strData.indexOf("t=");
                        if(nIndex != -1)
                        {
                            int value = Integer.parseInt(strData.substring(nIndex+2));
                            node.setValue(new DataValue(new Variant(value/1000.0)));
                        }
                    }

                    // Slow down...
                    Thread.sleep(1000);
                }
                catch (InterruptedException e){
                 // Stop the thread
                 break;
                }
                catch(Exception e){
                    logger.error("Exception when reading temperature file '{}', error is '{}'", dataFilePath, e.getMessage());
                }
            }
        }
    }

    public static DS18B20Node fromInput(PiNamespace namespace, InputConfig inputConfig) {
        UShort namespaceIndex = namespace.getNamespaceIndex();

        return new DS18B20Node(namespace,
                new NodeId(namespaceIndex, "Pin" + inputConfig.getPin()),
                new QualifiedName(namespaceIndex, inputConfig.getName()),
                LocalizedText.english(inputConfig.getName()),
                inputConfig
        );
    }

}
