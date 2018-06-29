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

package org.gradle.api.internal.tasks.execution;

import org.gradle.api.execution.internal.ExecuteTaskBuildOperationDetails;
import org.gradle.api.execution.internal.ExecuteTaskBuildOperationResult;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.tasks.TaskExecuter;
import org.gradle.api.internal.tasks.TaskExecutionContext;
import org.gradle.api.internal.tasks.TaskStateInternal;
import org.gradle.execution.TaskExecutionGraphInternal;
import org.gradle.internal.operations.BuildOperationCategory;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.RunnableBuildOperation;

public class EventFiringTaskExecuter implements TaskExecuter {

    private final BuildOperationExecutor buildOperationExecutor;
    private final TaskExecutionGraphInternal taskExecutionGraph;
    private final TaskExecuter delegate;

    public EventFiringTaskExecuter(BuildOperationExecutor buildOperationExecutor, TaskExecutionGraphInternal taskExecutionGraph, TaskExecuter delegate) {
        this.buildOperationExecutor = buildOperationExecutor;
        this.taskExecutionGraph = taskExecutionGraph;
        this.delegate = delegate;
    }

    @Override
    public void execute(final TaskInternal task, final TaskStateInternal state, final TaskExecutionContext context) {
        buildOperationExecutor.run(new RunnableBuildOperation() {
            @Override
            public void run(BuildOperationContext operationContext) {
                taskExecutionGraph.getTaskExecutionListenerSource().beforeExecute(task);

                delegate.execute(task, state, context);
                // Make sure we set the result even if listeners fail to execute
                operationContext.setResult(new ExecuteTaskBuildOperationResult(state, context));

                // If this fails, it masks the task failure.
                // It should addSuppressed() the task failure if there was one.
                taskExecutionGraph.getTaskExecutionListenerSource().afterExecute(task, state);

                operationContext.setStatus(state.getFailure() != null ? "FAILED" : state.getSkipMessage());
                operationContext.failed(state.getFailure());
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                ExecuteTaskBuildOperationDetails taskOperation = new ExecuteTaskBuildOperationDetails(task);
                return BuildOperationDescriptor.displayName("Task " + task.getIdentityPath())
                    .name(task.getIdentityPath().toString())
                    .progressDisplayName(task.getIdentityPath().toString())

                    // This is a legacy quirk.
                    // Scans use this to determine that progress logging is indicating start/finish of tasks.
                    // This can be removed in Gradle 5.0 (along with the concept of a “logging category” of an operation).
                    .progressLoggingCategory("class org.gradle.internal.buildevents.TaskExecutionLogger")

                    .operationType(BuildOperationCategory.TASK)
                    .details(taskOperation);
            }
        });
    }
}
