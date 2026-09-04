package com.example.demo.dto;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse(
            boolean success,
            String message,
            T data) {

        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 성공 응답
    public static <T> ApiResponse<T> success(
            String message,
            T data) {

        return new ApiResponse<>(
                true,
                message,
                data
        );
    }

    // 데이터 없는 성공 응답
    public static <T> ApiResponse<T> success(
            String message) {

        return new ApiResponse<>(
                true,
                message,
                null
        );
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(
            String message) {

        return new ApiResponse<>(
                false,
                message,
                null
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}