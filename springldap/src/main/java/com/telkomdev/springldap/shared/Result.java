package com.telkomdev.springldap.shared;

public class Result<D, E> {

    private D data;
    private E error;

    private Result(D data, E error) {
        this.data = data;
        this.error = error;
    }

    public static <D, E>Result<D, E> from(D data, E error) {
        return new Result(data, error);
    }

    public D getData() {
        return this.data;
    }

    public E getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return "Result(data: " + this.data + ", error: " + this.error + ")";
    }
}
