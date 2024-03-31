package io.jans.chip.modal.Fido.config;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;


import com.google.gson.annotations.SerializedName;

import io.jans.chip.modal.OperationError;

public class FidoConfigurationResponse {

    @SerializedName("issuer")
    private String issuer;
    @SerializedName("attestation")
    private Attestation attestation;
    @ColumnInfo(name = "ASSERTION")
    private Assertion assertion;
    @Ignore
    private boolean isSuccessful;
    @Ignore
    private OperationError operationError;

    public FidoConfigurationResponse(String issuer, Attestation attestation, Assertion assertion, boolean isSuccessful) {
        this.issuer = issuer;
        this.attestation = attestation;
        this.assertion = assertion;
        this.isSuccessful = isSuccessful;
    }

    public FidoConfigurationResponse(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Attestation getAttestation() {
        return attestation;
    }

    public void setAttestation(Attestation attestation) {
        this.attestation = attestation;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public OperationError getOperationError() {
        return operationError;
    }

    public void setOperationError(OperationError operationError) {
        this.operationError = operationError;
    }

    @Override
    public String toString() {
        return "FidoConfigurationResponse{" +
                "issuer='" + issuer + '\'' +
                ", attestation=" + attestation +
                ", assertion=" + assertion +
                ", isSuccessful=" + isSuccessful +
                ", operationError=" + operationError +
                '}';
    }
}
