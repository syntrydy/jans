package io.jans.chip.modal.Fido.config;

import androidx.room.ColumnInfo;

import com.google.gson.annotations.SerializedName;

public class Assertion {
    @ColumnInfo(name = "BASE_PATH")
    @SerializedName("base_path")
    private String basePath;
    @ColumnInfo(name = "OPTIONS_ENDPOINT")
    @SerializedName("options_endpoint")
    private String optionsEndpoint;
    @ColumnInfo(name = "OPTIONS_GENERATE_ENDPOINT")
    @SerializedName("options_generate_endpoint")
    private String optionsGenerateEndpoint;
    @ColumnInfo(name = "RESULT_ENDPOINT")
    @SerializedName("result_endpoint")
    private String resultEndpoint;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getOptionsEndpoint() {
        return optionsEndpoint;
    }

    public void setOptionsEndpoint(String optionsEndpoint) {
        this.optionsEndpoint = optionsEndpoint;
    }

    public String getOptionsGenerateEndpoint() {
        return optionsGenerateEndpoint;
    }

    public void setOptionsGenerateEndpoint(String optionsGenerateEndpoint) {
        this.optionsGenerateEndpoint = optionsGenerateEndpoint;
    }

    public String getResultEndpoint() {
        return resultEndpoint;
    }

    public void setResultEndpoint(String resultEndpoint) {
        this.resultEndpoint = resultEndpoint;
    }
}
