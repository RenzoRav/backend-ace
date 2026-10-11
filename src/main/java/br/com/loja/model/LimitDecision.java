package br.com.loja.model;
public record LimitDecision(boolean allowed,long retryAfterSeconds) {}
