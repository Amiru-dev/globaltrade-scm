package com.globaltrade.ejb.service;

import java.io.Serializable;

/**
 * Data Transfer Object for carrier quotes.
 *
 * <p><strong>Serializable:</strong> Required for remote interface calls to enable
 * parameter passing by value across JVM boundaries.</p>
 */
public class CarrierQuote implements Serializable {
    private static final long serialVersionUID = 1L;
    private String origin;
    private String destination;
    private double weight;
    private double price;
    private String callType;

    public CarrierQuote(String origin, String destination, double weight, double price, String callType) {
        this.origin = origin;
        this.destination = destination;
        this.weight = weight;
        this.price = price;
        this.callType = callType;
    }

    // Getters
    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public double getWeight() {
        return weight;
    }

    public double getPrice() {
        return price;
    }

    public String getCallType() {
        return callType;
    }
}