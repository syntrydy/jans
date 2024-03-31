package io.jans.chip.modelview;

import android.content.Context;

import io.jans.chip.modal.Fido.config.FidoConfigurationResponse;
import io.jans.chip.modal.SingleLiveEvent;
import io.jans.chip.repository.FidoConfigurationRepository;


public class FidoConfigurationViewModel {
    FidoConfigurationRepository fidoConfigurationRepository;
    public FidoConfigurationViewModel(Context context) {
        fidoConfigurationRepository = FidoConfigurationRepository.getInstance(context);
    }
    public SingleLiveEvent<FidoConfigurationResponse> fetchFidoConfiguration(String configurationUrl) {
        return fidoConfigurationRepository.fetchFidoConfiguration(configurationUrl);
    }
}
