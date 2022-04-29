#!/bin/bash
if [ "$ISBPL_PATH" = "" ] ; then
    echo 'ISBPL_PATH=/usr/lib/isbpl' >> /etc/environment
fi
mkdir /usr/lib/isbpl
cp ./*.isbpl /usr/lib/isbpl
cd bootstrap
javac ISBPL.java
zip -r ISBPL.jar *.class META-INF 
rm *.class
cd ..
echo "#!/usr/bin/java -jar" > ISBPL.jar
cat bootstrap/ISBPL.jar >> ISBPL.jar
chmod a+rx ISBPL.jar
cp ISBPL.jar /bin/isbpl
chmod a+rx /bin/isbpl
