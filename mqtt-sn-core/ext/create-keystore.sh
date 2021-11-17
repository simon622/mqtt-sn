#!/bin/bash

#
# /*
#  * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
#  *
#  * Find me on GitHub:
#  * https://github.com/simon622
#  *
#  * Licensed to the Apache Software Foundation (ASF) under one
#  * or more contributor license agreements.  See the NOTICE file
#  * distributed with this work for additional information
#  * regarding copyright ownership.  The ASF licenses this file
#  * to you under the Apache License, Version 2.0 (the
#  * "License"); you may not use this file except in compliance
#  * with the License.  You may obtain a copy of the License at
#  *
#  *   http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing,
#  * software distributed under the License is distributed on an
#  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  * KIND, either express or implied.  See the License for the
#  * specific language governing permissions and limitations
#  * under the License.
#  */
#
#

rm  my-keystore.jks    2> /dev/null
rm  my-truststore.jks  2> /dev/null

echo "================================================="
echo "Generating keystore..."
echo "================================================="

keytool -genkeypair -alias a1  -dname cn=a1                        \
  -validity 10000 -keyalg RSA -keysize 2048                        \
  -keystore my-keystore.jks -keypass password -storepass password

keytool -list -v -storepass password -keystore  my-keystore.jks

echo "================================================="
echo "Generating truststore..."
echo "================================================="

  keytool -exportcert -alias a1                                        \
    -keystore my-keystore.jks -keypass password -storepass password    \
| keytool -importcert -noprompt -alias a1                              \
    -keystore my-truststore.jks -keypass password -storepass password

keytool -list -v -storepass password -keystore my-truststore.jks