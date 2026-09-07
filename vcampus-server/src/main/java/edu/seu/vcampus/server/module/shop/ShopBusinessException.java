package edu.seu.vcampus.server.module.shop;

import java.util.Objects;

/** Expected shop business rejection with a stable public error code. */
final class ShopBusinessException extends RuntimeException {

    private final String code;

    ShopBusinessException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    String code() {
        return code;
    }
}
