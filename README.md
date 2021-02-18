# HamsterWheel

Mouse tester and benchmark tool

I make it just for fun and to compare my own mice but you can use it for anything

![alt text](https://github.com/szabodanika/HamsterWheel/blob/master/screenshot1.jpg?raw=true[/img])

## Features

- Polling rate testing
- DPI accuracy testing
- Speed and acceleration testing
- Skipping, jumping testing
- Cursor tracking
- Generating statistics in a log file
- Full screen and windowed mode
- Polling rate multiplier for testing different polling rates
- Saving settings in local config file
- Dark mode
- RGB

## How to download and run

1. Find the newest release [here](https://github.com/szabodanika/HamsterWheel/releases)
2. Open the **assets** dropdown
3. Download **HamsterWheel.zip**
4. Unzip folder
5. Launch **HamsterWheel.exe**

## How to use

- Video and written tutorials coming soon

## Building the executable for yourself

###1. Build executable jar with dependencies using Maven
```
mvn clean install
```

###2. Generate custom JRE using jlink (optional, a full fat JDK can be used too)
```
jlink --output hamsterwheel-jre-runtime --add-modules java.desktop
```

###3. Create native windows executable
1. On windows you can use launch4j for this, just load the execonfig.xml
2. Generate executable
3. Place executable in a new empty folder called "HamsterWheel"
4. Place custom JRE from previous step next to the .exe file