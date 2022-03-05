#!/bin/bash
echo 'ISBPL_PATH=/usr/lib/isbpl' >> /etc/environment
mkdir /usr/lib/isbpl
cp ./*.isbpl /usr/lib/isbpl
