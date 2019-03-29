This folder - frc - are the files for the roboRIO project.

Most of these files are for the robot from the 2019 develpoment project.  DO NOT overlay the official project with these vision files.

Some files are added or changed to use the vision data from the Raspberry Pi.

Sorry but you'll have to manually compare this project with the latest 2019 development project to see what needs to be added or changed in the official 2019 development project.


See the Raspberry Pi vision project for instructions about adding
GSON 2.8.5 to the build.gradle and your local maven repository to satisfy the use of GSON in the roboRIO code.

The instructions are the same for the RPi project and this roboRIO project.  But manually add a couple of lines to the roboRIO build.gradle.  That file is vastly different between the RPi and roboRIO.
