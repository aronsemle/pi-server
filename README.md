Raspberry Pi OPC-UA Server
=========

Hacked Kevin's code to get this working with a DS18B20 sensor. Do the following to get this working.

* Follow these instructions to read data from the DSB18B20. https://www.cl.cam.ac.uk/projects/raspberrypi/tutorials/temperature/
* Build this code using 'mvn clean install'
* Unpack the .zip file in the target directory
* In the unpacked directory rename 'config/gpio-config.json.example' to 'gpio-config.json', and make sure the pin # (defaults to 4) is correct
* In the unpacked directory 'chmod +x bin/pi-server.sh'
* Run 'sudo sh pi-server.sh console'
* The UA server should be up and running. Check the console for any error messages
* Browse the UA server on IP:12685
* Connect and you should see a GPIO folder with a 'temperature' node. The value of the node should be the temperature in C

Note the temperature is read from a file on the PI every second.
