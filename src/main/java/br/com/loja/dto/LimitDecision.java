package br.com.loja.dto;

public record LimitDecision(boolean allowed, long retryAfterSeconds) {}
