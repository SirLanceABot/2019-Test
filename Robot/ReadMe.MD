The files in this folder are for the Raspberry Pi frcvision

The subfolder frc and its subfolders are the source of a roboRIO project that can be used to drive to the vision commands from the RPi.

It's a work in progress and there is no coordination with changes being made to the real 2019 Development code in another repository.  Sorry this is crude but when the vision process is fully developed then compare the actual robot code to this test vision code and update the real repositry as needed.

The fstab file herein contains the line of code plus a comment to activate automount of the USB flash drive.  It is not recommended that this file or any other duplicate file of fstab be used on all images of RPi.  It is best to edit add the two lines to the existing fstab on a RPi SD card.  The fstab contains unique UUIDs that may be important to retain on a particular SD card.

The folder 2.8.5 is GSON (Google SJON) used to format data to send between the RPi and roboRIO.  It should be installed in maven of any computer building code for the roboRIO along with adding it to the compile dependencies of the roboRIO build.gradle.

--rkt 3/4/2019
