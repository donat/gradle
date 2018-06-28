/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.cache.internal;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import org.gradle.util.GradleVersion;

import java.util.SortedMap;

public class CacheVersionMapping {

    private final SortedMap<GradleVersion, Integer> versions;

    private CacheVersionMapping(SortedMap<GradleVersion, Integer> versions) {
        this.versions = versions;
    }

    public int getCurrentVersion() {
        return versions.get(versions.lastKey());
    }

    public Optional<Integer> getVersionUsedBy(GradleVersion gradleVersion) {
        Integer result = versions.get(gradleVersion);
        if (result == null) {
            SortedMap<GradleVersion, Integer> smallerVersions = versions.headMap(gradleVersion);
            if (!smallerVersions.isEmpty()) {
                return Optional.of(smallerVersions.get(smallerVersions.lastKey()));
            }
            return Optional.absent();
        }
        return Optional.of(result);
    }

    public static Builder introducedIn(String gradleVersion) {
        return new Builder().changedTo(1, gradleVersion);
    }

    public static class Builder {

        private final SortedMap<GradleVersion, Integer> versions = Maps.newTreeMap();

        private Builder() {
        }

        public Builder incrementedIn(String minGradleVersion) {
            return changedTo(versions.get(versions.lastKey()) + 1, minGradleVersion);
        }

        public Builder changedTo(int cacheVersion, String minGradleVersion) {
            GradleVersion parsedGradleVersion = GradleVersion.version(minGradleVersion);
            if (!versions.isEmpty() && parsedGradleVersion.compareTo(versions.lastKey()) <= 0) {
                throw new IllegalArgumentException("Gradle version (" + parsedGradleVersion.getVersion() + ") must be greater than all previous versions: " + versions.keySet());
            }
            if (!versions.isEmpty() && cacheVersion <= versions.get(versions.lastKey())) {
                throw new IllegalArgumentException("cache version (" + cacheVersion + ") must be greater than all previous versions: " + versions.values());
            }
            versions.put(parsedGradleVersion, cacheVersion);
            return this;
        }

        public CacheVersionMapping build() {
            return new CacheVersionMapping(ImmutableSortedMap.copyOfSorted(versions));
        }
    }
}
