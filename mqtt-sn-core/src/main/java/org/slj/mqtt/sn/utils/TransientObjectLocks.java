/*
 *
 *  * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *  *
 *  * Find me on GitHub:
 *  * https://github.com/simon622
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 *
 */

package org.slj.mqtt.sn.utils;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class TransientObjectLocks {

    private final Map<IMutex, WeakReference<IMutex>> lockMap
            = new WeakHashMap<>();

    public IMutex mutex(String id) {

        if (id == null) {
            throw new NullPointerException();
        }

        IMutex key = new IdMutex(id);

        synchronized (lockMap) {

            WeakReference<IMutex> ref = lockMap.get(key);
            if (ref == null) {

                lockMap.put(key,
                        new WeakReference<IMutex>(key));
                return key;
            }

            IMutex mutex = (IMutex) ref.get();
            if (mutex == null) {

                lockMap.put(key,
                        new WeakReference<IMutex>(key));
                return key;
            }
            return mutex;
        }
    }

    public int getMutexCount() {

        synchronized (lockMap) {

            return lockMap.size();
        }
    }

    static interface IMutex { }

    static class IdMutex implements IMutex {

        private final String id;

        protected IdMutex(String id) {

            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IdMutex other = (IdMutex) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        public String toString() {
            return id;
        }
    }
}
