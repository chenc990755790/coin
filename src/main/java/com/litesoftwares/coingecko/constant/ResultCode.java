package com.litesoftwares.coingecko.constant;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultCode {

    private String code;
    private String msg;

    public ResultCode(ErrorCode code) {
        this.code = code.getCode();
        this.msg = code.getMsg();
    }
}
