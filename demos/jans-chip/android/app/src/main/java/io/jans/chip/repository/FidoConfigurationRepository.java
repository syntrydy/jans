package io.jans.chip.repository;

import android.content.Context;
import android.util.Log;

import io.jans.chip.AppDatabase;
import io.jans.chip.modal.Fido.config.FidoConfiguration;
import io.jans.chip.modal.Fido.config.FidoConfigurationResponse;
import io.jans.chip.modal.OperationError;
import io.jans.chip.modal.SingleLiveEvent;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.utils.AppConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FidoConfigurationRepository {
    public static final String TAG = "FidoConfigurationRepository";
    private final SingleLiveEvent<FidoConfigurationResponse> fidoConfigurationLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;
    private FidoConfigurationRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static FidoConfigurationRepository fidoConfigurationRepository;

    public static FidoConfigurationRepository getInstance(Context context) {
        if (fidoConfigurationRepository == null) {
            fidoConfigurationRepository = new FidoConfigurationRepository(context);
        }
        return fidoConfigurationRepository;
    }

    public SingleLiveEvent<FidoConfigurationResponse> fetchFidoConfiguration(String configurationUrl) {

        String issuer = configurationUrl.replace(AppConfig.FIDO_CONFIG_URL, "");
        Log.d(TAG, "Inside fetchFIDOConfiguration :: configurationUrl ::" + configurationUrl);
        try {
            Call<FidoConfigurationResponse> call = RetrofitClient.getInstance(issuer).getAPIInterface().getFidoConfiguration(configurationUrl);

            call.enqueue(new Callback<FidoConfigurationResponse>() {
                @Override
                public void onResponse(Call<FidoConfigurationResponse> call, Response<FidoConfigurationResponse> response) {
                    if (response.code() == 200) {
                        FidoConfigurationResponse fidoConfiguration = response.body();
                        fidoConfiguration.setSuccessful(true);
                        FidoConfiguration fidoConfigDB = new FidoConfiguration();
                        fidoConfigDB.setSno(AppConfig.DEFAULT_S_NO);
                        fidoConfigDB.setIssuer(fidoConfiguration.getIssuer());
                        fidoConfigDB.setAssertionOptionsEndpoint(fidoConfiguration.getAssertion().getOptionsEndpoint());
                        fidoConfigDB.setAssertionResultEndpoint(fidoConfiguration.getAssertion().getResultEndpoint());
                        fidoConfigDB.setAttestationOptionsEndpoint(fidoConfiguration.getAttestation().getOptionsEndpoint());
                        fidoConfigDB.setAttestationResultEndpoint(fidoConfiguration.getAttestation().getResultEndpoint());

                        Log.d(TAG, "Inside fetchOPConfiguration :: opConfiguration :: " + fidoConfiguration.toString());
                        appDatabase.fidoConfigurationDao().deleteAll();
                        appDatabase.fidoConfigurationDao().insert(fidoConfigDB);
                        fidoConfigurationLiveData.setValue(fidoConfiguration);
                    } else {
                        fidoConfigurationLiveData.setValue(setErrorInLiveObject("Error in fetching FIDO Configuration.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                    }
                }

                @Override
                public void onFailure(Call<FidoConfigurationResponse> call, Throwable t) {
                    Log.e(TAG, "Inside fetchOPConfiguration :: onFailure :: " + t.getMessage());
                    fidoConfigurationLiveData.setValue(setErrorInLiveObject("Error in  fetching OP Configuration.\n" + t.getMessage()));

                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in  fetching OP Configuration.\n" + e.getMessage());
            fidoConfigurationLiveData.setValue(setErrorInLiveObject("Error in  fetching OP Configuration.\n" + e.getMessage()));
        }
        return fidoConfigurationLiveData;

    }
    private FidoConfigurationResponse setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        FidoConfigurationResponse fidoConfiguration = new FidoConfigurationResponse(false, operationError);
        return fidoConfiguration;
    }
}
