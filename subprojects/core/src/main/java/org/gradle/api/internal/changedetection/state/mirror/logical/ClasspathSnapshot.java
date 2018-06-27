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

package org.gradle.api.internal.changedetection.state.mirror.logical;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.changedetection.rules.FileChange;
import org.gradle.api.internal.changedetection.rules.TaskStateChangeVisitor;
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot;
import org.gradle.api.internal.changedetection.state.FileContentSnapshot;
import org.gradle.api.internal.changedetection.state.NormalizedFileSnapshot;
import org.gradle.api.internal.changedetection.state.SnapshotMapSerializer;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.gradle.internal.Factory;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.HashCodeSerializer;
import org.gradle.internal.serialize.Serializer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ClasspathSnapshot extends SnapshotFactoryFileCollectionSnapshot<NormalizedFileSnapshot> {

    public ClasspathSnapshot(Factory<Map<String, NormalizedFileSnapshot>> snapshotFactory) {
        super(snapshotFactory);
    }

    private ClasspathSnapshot(Map<String, NormalizedFileSnapshot> snapshots, @Nullable HashCode hashCode) {
        super(snapshots, hashCode);
    }

    @Override
    public boolean visitChangesSince(FileCollectionSnapshot oldSnapshot, String propertyTitle, boolean includeAdded, TaskStateChangeVisitor visitor) {
        Iterator<Map.Entry<String, NormalizedFileSnapshot>> currentEntries = getFileSnapshots().entrySet().iterator();
        Iterator<Map.Entry<String, NormalizedFileSnapshot>> previousEntries = ((ClasspathSnapshot) oldSnapshot).getFileSnapshots().entrySet().iterator();
        while (true) {
            if (currentEntries.hasNext()) {
                Map.Entry<String, NormalizedFileSnapshot> current = currentEntries.next();
                String currentAbsolutePath = current.getKey();
                if (previousEntries.hasNext()) {
                    Map.Entry<String, NormalizedFileSnapshot> previous = previousEntries.next();
                    NormalizedFileSnapshot currentNormalizedSnapshot = current.getValue();
                    NormalizedFileSnapshot previousNormalizedSnapshot = previous.getValue();
                    String currentNormalizedPath = currentNormalizedSnapshot.getNormalizedPath();
                    String previousNormalizedPath = previousNormalizedSnapshot.getNormalizedPath();
                    if (currentNormalizedPath.equals(previousNormalizedPath)) {
                        if (!currentNormalizedSnapshot.getSnapshot().isContentUpToDate(previousNormalizedSnapshot.getSnapshot())) {
                            if (!visitor.visitChange(
                                FileChange.modified(currentAbsolutePath, propertyTitle,
                                    previousNormalizedSnapshot.getSnapshot().getType(),
                                    currentNormalizedSnapshot.getSnapshot().getType()
                                ))) {
                                return false;
                            }
                        }
                    } else {
                        String previousAbsolutePath = previous.getKey();
                        if (!visitor.visitChange(FileChange.removed(previousAbsolutePath, propertyTitle, previousNormalizedSnapshot.getSnapshot().getType()))) {
                            return false;
                        }
                        if (includeAdded) {
                            if (!visitor.visitChange(FileChange.added(currentAbsolutePath, propertyTitle, currentNormalizedSnapshot.getSnapshot().getType()))) {
                                return false;
                            }
                        }
                    }
                } else {
                    if (includeAdded) {
                        if (!visitor.visitChange(FileChange.added(currentAbsolutePath, propertyTitle, current.getValue().getSnapshot().getType()))) {
                            return false;
                        }
                    }
                }
            } else {
                if (previousEntries.hasNext()) {
                    Map.Entry<String, NormalizedFileSnapshot> previousEntry = previousEntries.next();
                    if (!visitor.visitChange(FileChange.removed(previousEntry.getKey(), propertyTitle, previousEntry.getValue().getSnapshot().getType()))) {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }
    }

    @Override
    protected void doGetHash(DefaultBuildCacheHasher hasher) {
        for (NormalizedFileSnapshot normalizedSnapshot : getFileSnapshots().values()) {
            normalizedSnapshot.appendToHasher(hasher);
        }
    }

    @Override
    public Collection<File> getElements() {
        throw new UnsupportedOperationException("Only supported for outputs");
    }

    @Override
    public Map<String, NormalizedFileSnapshot> getSnapshots() {
        return getFileSnapshots();
    }

    @Override
    public Map<String, FileContentSnapshot> getContentSnapshots() {
        throw new UnsupportedOperationException("Only supported for outputs");
    }

    public static class SerializerImpl implements Serializer<ClasspathSnapshot> {

        private final HashCodeSerializer hashCodeSerializer;
        private final SnapshotMapSerializer snapshotMapSerializer;

        public SerializerImpl(StringInterner stringInterner) {
            this.hashCodeSerializer = new HashCodeSerializer();
            this.snapshotMapSerializer = new SnapshotMapSerializer(stringInterner);
        }

        @Override
        public ClasspathSnapshot read(Decoder decoder) throws IOException {
            int type = decoder.readSmallInt();
            Preconditions.checkState(type == 2);
            boolean hasHash = decoder.readBoolean();
            HashCode hash = hasHash ? hashCodeSerializer.read(decoder) : null;
            Map<String, NormalizedFileSnapshot> snapshots = snapshotMapSerializer.read(decoder);
            return new ClasspathSnapshot(snapshots, hash);
        }

        @Override
        public void write(Encoder encoder, ClasspathSnapshot value) throws Exception {
            encoder.writeSmallInt(2);
            encoder.writeBoolean(value.hasHash());
            if (value.hasHash()) {
                hashCodeSerializer.write(encoder, value.getHash());
            }
            snapshotMapSerializer.write(encoder, value.getSnapshots());
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }

            ClasspathSnapshot.SerializerImpl rhs = (ClasspathSnapshot.SerializerImpl) obj;
            return Objects.equal(snapshotMapSerializer, rhs.snapshotMapSerializer)
                && Objects.equal(hashCodeSerializer, rhs.hashCodeSerializer);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.hashCode(), snapshotMapSerializer, hashCodeSerializer);
        }
    }
}
