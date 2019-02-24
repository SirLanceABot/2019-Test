echo on

cd C:\Users\Public\frc2019\frccode

call frcvars2019.bat

cd C:\Users\RKT\frc\FRC2019\Code\java-multiCameraServerRJ

call gradlew.bat build

echo Deploying

echo On the rPi web dashboard:

echo 1) Make the rPi writable by selecting the "Writable" tab

echo 2) In the rPi web dashboard Application tab, select the "Uploaded Java jar"
echo    option for Application

echo 3) Click "Browse..." and select the "java-multiCameraServerRJ-all.jar" file in
echo    your desktop project directory in the build/libs subdirectory

echo C:\Users\RKT\frc\FRC2019\Code\java-multiCameraServer\build\libs  java-multiCameraServerRJ-all.jar

echo 4) Click Save

echo The application will be automatically started.  Console output can be seen by
echo enabling console output in the Vision Status tab.

pause
