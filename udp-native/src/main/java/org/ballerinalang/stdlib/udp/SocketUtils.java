/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.udp;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.ballerinalang.stdlib.udp.SocketConstants.ErrorType.GenericError;

/**
 * Represents the util functions of Socket operations.
 *
 * @since 0.985.0
 */
public class SocketUtils {

    private SocketUtils() {
    }

    /**
     * Create Generic udp error with given error message.
     *
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(String errMsg) {
        return ErrorCreator.createDistinctError(GenericError.errorType(), getUdpPackage(),
                                                 StringUtils.fromString(errMsg));
    }

    /**
     * Create udp error with given error type and message.
     *
     * @param type   the error type which cause for this error
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(SocketConstants.ErrorType type, String errMsg) {
        return ErrorCreator.createDistinctError(type.errorType(), getUdpPackage(), StringUtils.fromString(errMsg));
    }


    /**
     * This will return a byte array that only contains the data from ByteBuffer.
     * This will not copy any unused byte from ByteBuffer.
     *
     * @param content {@link ByteBuffer} with content
     * @return a byte array
     */
    public static byte[] getByteArrayFromByteBuffer(ByteBuffer content) {
        int contentLength = content.position();
        byte[] bytesArray = new byte[contentLength];
        content.flip();
        content.get(bytesArray, 0, contentLength);
        return bytesArray;
    }

    /**
     * This will try to shutdown executor service gracefully.
     *
     * @param executorService {@link ExecutorService} that need shutdown
     */
    public static void shutdownExecutorGracefully(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    /**
     * This will shutdown executor immediately.
     *
     * @param executorService {@link ExecutorService} that need shutdown
     */
    public static void shutdownExecutorImmediately(ExecutorService executorService) {
        executorService.shutdownNow();
    }

    /**
     * Gets ballerina udp package.
     *
     * @return io package.
     */
    public static Module getUdpPackage() {
        return ModuleUtils.getModule();
    }
}
