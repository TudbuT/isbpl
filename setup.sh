#!/bin/bash
if [ "$ISBPL_PATH" = "" ] ; then
    echo 'ISBPL_PATH=/usr/lib/isbpl' >> /etc/environment
fi
IFS=: read -r BIN OTHERBIN <<< "$PATH"
mkdir /usr/lib/isbpl >& /dev/null
cp ./*.isbpl /usr/lib/isbpl >& /dev/null
cd bootstrap
cp ../ISBPL.java .
echo ">>> There will be warnings about the Unsafe. Sadly, the code producing them is required for JIO. <<<"
javac ISBPL.java
rm ISBPL.java ISBPL.jar
zip -r ISBPL.jar * META-INF 
rm *.class
cd ..
echo "#!${BIN}/java -jar" > ISBPL.jar
cat bootstrap/ISBPL.jar >> ISBPL.jar
chmod a+rx ISBPL.jar
cp ISBPL.jar "${BIN}/isbpl"
chmod a+rx "${BIN}/isbpl"
