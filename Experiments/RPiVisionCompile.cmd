echo on
pushd "C:\Users\Public\frc2019\frccode"
call frcvars2019.bat
popd
rem echo on
rem set DEBUG=Y
call gradlew.bat build
echo Deploying

echo On the rPi web dashboard:

echo 1) Make the rPi writable by selecting the "Writable" tab

echo 2) In the rPi web dashboard Application tab, select the "Uploaded Java jar"
echo    option for Application

echo 3) Click "Browse..." and select the "name_of_your_project_root_folder-all.jar" file in
echo    your project root directory in the build/libs subdirectory

echo 4) Click Save

echo The application will be automatically started.  Console output can be seen by
echo enabling console output in the Vision Status tab.

exit