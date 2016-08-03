cd `dirname $0`
cd ..
rm -rf bin
mkdir bin

rm -rf output
mkdir output
javac -d output src/*.java
echo "Main-Class: Main" > output/Manifest.mf
cd output
jar -cvfm ../bin/acrash-report.jar Manifest.mf *.class
cd ..
rm -rf output

echo 'cd `dirname $0`' > bin/acrash-report.sh
echo 'java -jar acrash-report.jar $@' >> bin/acrash-report.sh
echo 'cd ${0%/*}' > bin/acrash-report.bat
echo 'java -jar acrash-report.jar $@' >> bin/acrash-report.bat
