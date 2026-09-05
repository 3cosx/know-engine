package com.cosx.knowengine.document.support;

import com.cosx.knowengine.document.dto.VersionIncrement;
import com.cosx.knowengine.exception.BusinessException;

public record SemanticVersion(int major, int minor, int patch) {

    public static SemanticVersion parse(String value) {
        if (value == null || !value.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new BusinessException(40020, "文档版本号格式必须为 MAJOR.MINOR.PATCH");
        }
        String[] parts = value.split("\\.");
        try {
            return new SemanticVersion(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            throw new BusinessException(40020, "文档版本号超出支持范围");
        }
    }

    public SemanticVersion increment(VersionIncrement increment) {
        return switch (increment) {
            case PATCH -> new SemanticVersion(major, minor, Math.addExact(patch, 1));
            case MINOR -> new SemanticVersion(major, Math.addExact(minor, 1), 0);
            case MAJOR -> new SemanticVersion(Math.addExact(major, 1), 0, 0);
        };
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
