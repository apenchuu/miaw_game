package com.steven.frontend.patterns.facade;

// Menyediakan pintu akses sederhana untuk operasi backend yang sering dipakai.
public class BackendFacade {
    @FunctionalInterface
    public interface RequestSender {
        String send(String method, String url, String body) throws Exception;
    }

    @FunctionalInterface
    public interface AuthSender {
        String send(String baseUrl, String username, String password) throws Exception;
    }

    private final RequestSender requestSender;
    private final AuthSender authSender;

    public BackendFacade(RequestSender requestSender, AuthSender authSender) {
        this.requestSender = requestSender;
        this.authSender = authSender;
    }

    public String loginOrRegister(String baseUrl, String username, String password) throws Exception {
        return authSender.send(baseUrl, username, password);
    }

    public String sendProgress(String url, String payload) throws Exception {
        return requestSender.send("PUT", url, payload);
    }

    public String sendInventory(String url, String payload) throws Exception {
        return requestSender.send("PUT", url, payload);
    }
}
