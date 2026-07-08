package com.shen.common.result;

import com.shen.common.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private int code;
    private String msg;

    public ApiError(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    public static ApiError of(ResultCode resultCode) {
        return new ApiError(resultCode);
    }

    public static ApiError of(int code, String msg) {
        return new ApiError(code, msg);
    }
}