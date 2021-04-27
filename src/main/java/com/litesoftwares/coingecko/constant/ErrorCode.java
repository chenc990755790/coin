package com.litesoftwares.coingecko.constant;

import lombok.Getter;

public enum ErrorCode {
    SUCCESS("200", "成功"),
    FAIL("500", "后台错误"),
    DATA_NOT_FULL("000", "数据不全"),
    ;
    @Getter
    private String code;
    @Getter
    private String msg;

    ErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
