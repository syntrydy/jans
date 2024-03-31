package io.jans.chip.modal.Fido.config;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
@Entity(tableName = "FIDO_CONFIGURATION")
public class FidoConfiguration {
    @NonNull
    @PrimaryKey
    @SerializedName("SNO")
    private String sno;
    @ColumnInfo(name = "ISSUER")
    @SerializedName("issuer")
    private String issuer;
    @ColumnInfo(name = "ATTESTATION_OPTIONS_ENDPOINT")
    private String attestationOptionsEndpoint;
    @ColumnInfo(name = "ATTESTATION_RESULT_ENDPOINT")
    private String attestationResultEndpoint;
    @ColumnInfo(name = "ASSERTION_OPTIONS_ENDPOINT")
    private String assertionOptionsEndpoint;
    @ColumnInfo(name = "ASSERTION_RESULT_ENDPOINT")
    private String assertionResultEndpoint;
    @NonNull
    public String getSno() {
        return sno;
    }

    public void setSno(@NonNull String sno) {
        this.sno = sno;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAttestationOptionsEndpoint() {
        return attestationOptionsEndpoint;
    }

    public void setAttestationOptionsEndpoint(String attestationOptionsEndpoint) {
        this.attestationOptionsEndpoint = attestationOptionsEndpoint;
    }

    public String getAttestationResultEndpoint() {
        return attestationResultEndpoint;
    }

    public void setAttestationResultEndpoint(String attestationResultEndpoint) {
        this.attestationResultEndpoint = attestationResultEndpoint;
    }

    public String getAssertionOptionsEndpoint() {
        return assertionOptionsEndpoint;
    }

    public void setAssertionOptionsEndpoint(String assertionOptionsEndpoint) {
        this.assertionOptionsEndpoint = assertionOptionsEndpoint;
    }

    public String getAssertionResultEndpoint() {
        return assertionResultEndpoint;
    }

    public void setAssertionResultEndpoint(String assertionResultEndpoint) {
        this.assertionResultEndpoint = assertionResultEndpoint;
    }
}
