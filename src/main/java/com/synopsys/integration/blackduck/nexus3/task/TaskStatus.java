package com.synopsys.integration.blackduck.nexus3.task;

import com.synopsys.integration.blackduck.summary.Result;

public enum TaskStatus {
    SUCCESS,
    FAILURE,
    PENDING;

    public static TaskStatus convertResult(Result result) {
        if (Result.SUCCESS == result) {
            return SUCCESS;
        }

        return FAILURE;
    }
}
