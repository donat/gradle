/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.progress

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.operations.BuildOperationDescriptor

class NoOpProgressLoggerFactory implements ProgressLoggerFactory {
    @Override
    ProgressLogger newOperation(String loggerCategory) {
        throw new UnsupportedOperationException()
    }

    @Override
    ProgressLogger newOperation(Class<?> loggerCategory) {
        throw new UnsupportedOperationException()
    }

    @Override
    ProgressLogger newOperation(String loggerCategory, BuildOperationDescriptor buildOperationDescriptor) {
        new Logger()
    }

    @Override
    ProgressLogger newOperation(Class<?> loggerClass, ProgressLogger parent) {
        throw new UnsupportedOperationException()
    }

    static class Logger implements ProgressLogger {
        String description
        String shortDescription
        String loggingHeader

        @Override
        String getDescription() { description }

        @Override
        ProgressLogger setDescription(String description) {
            this.description = description
            this
        }

        @Override
        String getShortDescription() { shortDescription }

        @Override
        ProgressLogger setShortDescription(String description) {
            this.shortDescription = description
            this
        }

        @Override
        String getLoggingHeader() { loggingHeader }

        @Override
        ProgressLogger setLoggingHeader(String header) {
            this.loggingHeader = header
            this
        }

        @Override
        ProgressLogger start(String description, String shortDescription) {
            start(description, shortDescription, 0)
        }

        @Override
        ProgressLogger start(String description, String shortDescription, int totalProgress) {
            setDescription(description)
            setShortDescription(shortDescription)
            started()
            this
        }

        @Override
        void started() {}

        @Override
        void started(String status) {}

        @Override
        void started(String status, int totalProgress) {}

        @Override
        void progress(String status) {}

        @Override
        void progress(String status, boolean failing) {}

        @Override
        void completed() {}

        @Override
        void completed(String status, boolean failed) {}
    }
}
